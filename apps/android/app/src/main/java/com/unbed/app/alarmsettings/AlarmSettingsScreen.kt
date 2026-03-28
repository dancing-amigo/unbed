package com.unbed.app.alarmsettings

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unbed.app.UnbedApplication
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun alarmSettingsRoute() {
    val application = LocalContext.current.applicationContext as UnbedApplication
    val settingsViewModel: AlarmSettingsViewModel =
        viewModel(
            factory = AlarmSettingsViewModel.factory(application.appContainer.alarmCoordinator),
        )
    val uiState by settingsViewModel.uiState.collectAsState()

    alarmSettingsScreen(
        uiState = uiState,
        onEnabledChange = settingsViewModel::onEnabledChange,
        onRepeatDayToggle = settingsViewModel::onRepeatDayToggle,
        onTimeChange = settingsViewModel::onTimeChange,
        onOpenExactAlarmSettings = settingsViewModel::openExactAlarmSettings,
        onSave = settingsViewModel::save,
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "LongParameterList")
@Composable
private fun alarmSettingsScreen(
    uiState: AlarmSettingsUiState,
    onEnabledChange: (Boolean) -> Unit,
    onRepeatDayToggle: (DayOfWeek) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d HH:mm") }
    val enabledDescription =
        if (uiState.enabled) {
            "Alarm will be scheduled on save."
        } else {
            "The config stays stored but unscheduled."
        }
    val nextTriggerText =
        uiState.nextTrigger?.let {
            val formattedTime = it.triggerAt.atZone(ZoneId.systemDefault()).format(formatter)
            "Next scheduled trigger: $formattedTime (${it.type.name.lowercase()})"
        } ?: "No alarm is currently scheduled."

    Scaffold { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "Alarm settings",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Configure one wake-up target now. The data is stored locally and rescheduled on save.",
                style = MaterialTheme.typography.bodyLarge,
            )

            if (!uiState.canScheduleExactAlarms) {
                exactAlarmCard(onOpenExactAlarmSettings = onOpenExactAlarmSettings)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Enabled",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = enabledDescription,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = uiState.enabled,
                    onCheckedChange = onEnabledChange,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Wake-up time",
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedButton(onClick = { showTimePicker = true }) {
                    Text(
                        text =
                            uiState.time.format(
                                if (DateFormat.is24HourFormat(context)) {
                                    DateTimeFormatter.ofPattern("HH:mm")
                                } else {
                                    DateTimeFormatter.ofPattern("hh:mm a")
                                },
                            ),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Repeat days",
                    style = MaterialTheme.typography.titleMedium,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = day in uiState.repeatDays,
                            onClick = { onRepeatDayToggle(day) },
                            label = {
                                Text(day.name.take(DAY_LABEL_LENGTH))
                            },
                        )
                    }
                }
                Text(
                    text = "Leave all days unselected to treat this as a one-time alarm.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Text(
                text = nextTriggerText,
                style = MaterialTheme.typography.bodyMedium,
            )

            Button(
                onClick = onSave,
                enabled = uiState.isLoaded && !uiState.isSaving,
            ) {
                Text(if (uiState.isSaving) "Saving..." else "Save alarm")
            }
        }
    }

    if (showTimePicker) {
        val pickerState =
            rememberTimePickerState(
                initialHour = uiState.time.hour,
                initialMinute = uiState.time.minute,
                is24Hour = DateFormat.is24HourFormat(context),
            )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        onTimeChange(pickerState.hour, pickerState.minute)
                        showTimePicker = false
                    },
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = pickerState)
            },
        )
    }
}

@Composable
private fun exactAlarmCard(onOpenExactAlarmSettings: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Exact alarm permission is unavailable",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Open system settings so Android can fire the alarm close to the requested time.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(onClick = onOpenExactAlarmSettings) {
            Text("Open exact alarm settings")
        }
    }
}

private const val DAY_LABEL_LENGTH: Int = 3
