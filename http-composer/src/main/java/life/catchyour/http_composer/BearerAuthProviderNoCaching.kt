package life.catchyour.http_composer

import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.plugins.auth.AuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.RefreshTokensParams
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.AuthScheme
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.utils.io.KtorDsl

fun AuthConfig.bearerNoCache(block: BearerAuthConfigOpen.() -> Unit) {
    with(BearerAuthConfigOpen().apply(block)) {
        this@bearerNoCache.providers.add(
            BearerAuthProviderNoCaching(
                refreshTokens = _refreshTokens,
                loadTokens = _loadTokens,
                sendWithoutRequestCallback = _sendWithoutRequest,
                realm = realm
            )
        )
    }
}

@KtorDsl
class BearerAuthConfigOpen {
    internal var _refreshTokens: suspend RefreshTokensParams.() -> BearerTokens? = { null }
    internal var _loadTokens: suspend () -> BearerTokens? = { null }
    internal var _sendWithoutRequest: (HttpRequestBuilder) -> Boolean = { true }

    var realm: String? = null

    /**
     * Configures a callback that refreshes a token when the 401 status code is received.
     */
    fun refreshTokens(block: suspend RefreshTokensParams.() -> BearerTokens?) {
        _refreshTokens = block
    }

    /**
     * Configures a callback that loads a cached token from a local storage.
     * Note: Using the same client instance here to make a request will result in a deadlock.
     */
    fun loadTokens(block: suspend () -> BearerTokens?) {
        _loadTokens = block
    }

    /**
     * Sends credentials without waiting for [HttpStatusCode.Unauthorized].
     */
    fun sendWithoutRequest(block: (HttpRequestBuilder) -> Boolean) {
        _sendWithoutRequest = block
    }
}

class BearerAuthProviderNoCaching(
    private val refreshTokens: suspend RefreshTokensParams.() -> BearerTokens?,
    private val loadTokens: suspend () -> BearerTokens?,
    private val sendWithoutRequestCallback: (HttpRequestBuilder) -> Boolean = { true },
    private val realm: String?
) : AuthProvider {

    @Deprecated(
        "Please use sendWithoutRequest function instead",
        ReplaceWith("error(\"Deprecated\")")
    )
    override val sendWithoutRequest: Boolean
        get() = error("Deprecated")

    override fun sendWithoutRequest(request: HttpRequestBuilder): Boolean =
        sendWithoutRequestCallback(request)

    /**
     * Checks if current provider is applicable to the request.
     */
    override fun isApplicable(auth: HttpAuthHeader): Boolean {
        if (auth.authScheme != AuthScheme.Bearer) {

            return false
        }
        val isSameRealm = when {
            realm == null -> true
            auth !is HttpAuthHeader.Parameterized -> false
            else -> auth.parameter("realm") == realm
        }

        return isSameRealm
    }

    /**
     * Adds an authentication method headers and credentials.
     */
    override suspend fun addRequestHeaders(
        request: HttpRequestBuilder,
        authHeader: HttpAuthHeader?
    ) {
        val token = loadTokens() ?: return

        request.headers {
            val tokenValue = "Bearer ${token.accessToken}"
            if (contains(HttpHeaders.Authorization)) {
                remove(HttpHeaders.Authorization)
            }
            append(HttpHeaders.Authorization, tokenValue)
        }
    }

    override suspend fun refreshToken(response: HttpResponse): Boolean {
        val refresh = loadTokens() ?: return false
        val newToken = refreshTokens(RefreshTokensParams(response.call.client, response, refresh))

        return newToken != null
    }
}
