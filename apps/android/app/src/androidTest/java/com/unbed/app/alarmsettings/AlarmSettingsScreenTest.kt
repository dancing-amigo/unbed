package com.unbed.app.alarmsettings

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.unbed.app.ui.theme.unbedTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalTime

class AlarmSettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun saveAlarmStaysDisabledBeforeInitialDataLoadFinishes() {
        composeRule.setContent {
            unbedTheme {
                alarmSettingsScreen(
                    uiState =
                        AlarmSettingsUiState(
                            time = LocalTime.of(7, 30),
                            isLoaded = false,
                        ),
                    onEnabledChange = {},
                    onRepeatDayToggle = {},
                    onTimeChange = { _, _ -> },
                    onOpenExactAlarmSettings = {},
                    onOpenSetup = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText("Save alarm").assertIsNotEnabled()
    }

    @Test
    fun exactAlarmGuidanceIsShownWhenPermissionIsUnavailable() {
        composeRule.setContent {
            unbedTheme {
                alarmSettingsScreen(
                    uiState =
                        AlarmSettingsUiState(
                            time = LocalTime.of(7, 30),
                            isLoaded = true,
                            canScheduleExactAlarms = false,
                        ),
                    onEnabledChange = {},
                    onRepeatDayToggle = {},
                    onTimeChange = { _, _ -> },
                    onOpenExactAlarmSettings = {},
                    onOpenSetup = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText("Open exact alarm settings").assertExists()
        composeRule.onNodeWithText("Save alarm").assertIsEnabled()
    }
}
