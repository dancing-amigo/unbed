package com.unbed.app.setup

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.unbed.app.ui.theme.unbedTheme
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun finishSetupStaysDisabledUntilEveryRequirementIsReady() {
        composeRule.setContent {
            unbedTheme {
                onboardingScreen(
                    uiState =
                        OnboardingUiState(
                            notificationsGranted = true,
                            cameraGranted = false,
                            exactAlarmGranted = true,
                            batteryOptimizationIgnored = true,
                            hasPlacedQrAwayFromBed = false,
                        ),
                    onAction = {},
                    onRefresh = {},
                    onConfirmQrPlacement = {},
                    onComplete = {},
                )
            }
        }

        composeRule.onNodeWithText("Finish setup").assertIsNotEnabled()
        composeRule.onNodeWithText("Grant camera").assertIsEnabled()
    }

    @Test
    fun finishSetupBecomesEnabledWhenOnboardingIsComplete() {
        composeRule.setContent {
            unbedTheme {
                onboardingScreen(
                    uiState =
                        OnboardingUiState(
                            notificationsGranted = true,
                            cameraGranted = true,
                            exactAlarmGranted = true,
                            batteryOptimizationIgnored = true,
                            hasPlacedQrAwayFromBed = true,
                        ),
                    onAction = {},
                    onRefresh = {},
                    onConfirmQrPlacement = {},
                    onComplete = {},
                )
            }
        }

        composeRule.onNodeWithText("Finish setup").assertIsEnabled()
        composeRule.onNodeWithText("Grant camera").assertDoesNotExist()
    }
}
