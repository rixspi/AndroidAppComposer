package life.catchyour.http_composer

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

object KtorClient {

    fun build(
        apiBaseUrl: String,
        jsonConfiguration: Json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        },
        tokenRefreshUrl: String,
        getTokens: suspend () -> BearerTokens?,
        saveTokens: suspend (JsonObject) -> BearerTokens,
        refreshTokenRequestBuilder: suspend HttpRequestBuilder.() -> Unit = {},
    ): HttpClient =
        HttpClient(Android) {

            expectSuccess = true

            install(ContentNegotiation) {
                json(jsonConfiguration)
            }

            // For Logging
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.ALL
            }

            install(Auth) {
                bearerNoCache {
                    sendWithoutRequest { true }

                    loadTokens {
                        getTokens()
                    }

                    refreshTokens {
                        val authResponse = client.post {
                            sendWithoutRequest { true }
                            markAsRefreshTokenRequest()
                            url(tokenRefreshUrl)
                            refreshTokenRequestBuilder()
                        }.body<JsonObject>()

                        saveTokens(authResponse)
                    }
                }
            }

            defaultRequest {
                url(apiBaseUrl)
                contentType(ContentType.Application.Json)
            }
        }
}

