package life.catchyour.ui_composer.view

import android.annotation.SuppressLint
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialDate: LocalDate,
    selectableDates: (millis: Long) -> Boolean = { millis ->
        millis > LocalDate.of(2024, 12, 19).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000 &&
                millis <= Instant.now().toEpochMilli()
    },
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val orientation = LocalConfiguration.current.orientation

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneId.of("UTC")).toInstant()
            .toEpochMilli(),
        initialDisplayMode = if (orientation == ORIENTATION_LANDSCAPE) DisplayMode.Input else DisplayMode.Picker,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                selectableDates(utcTimeMillis)
        }
    )

    val selectedDate: Instant = datePickerState.selectedDateMillis?.let {
        Instant.ofEpochMilli(it)
    } ?: Instant.now()

    androidx.compose.material3.DatePickerDialog(
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        ),
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(onClick = {
                val localDate = LocalDate.ofInstant(selectedDate, ZoneId.systemDefault())
                onDateSelected(localDate)
                onDismiss()
            }) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
            }) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = orientation != ORIENTATION_LANDSCAPE,
        )
    }
}
