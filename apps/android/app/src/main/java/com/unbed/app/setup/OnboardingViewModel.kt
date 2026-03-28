package com.unbed.app.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.unbed.domain.alarm.AlarmPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OnboardingUiState(
    val notificationsGranted: Boolean = false,
    val cameraGranted: Boolean = false,
    val exactAlarmGranted: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false,
    val hasPlacedQrAwayFromBed: Boolean = false,
    val qrValue: String = AlarmPolicy.DEFAULT_QR_CONFIG.fixedValue,
) {
    val canComplete: Boolean
        get() =
            notificationsGranted &&
                cameraGranted &&
                exactAlarmGranted &&
                batteryOptimizationIgnored &&
                hasPlacedQrAwayFromBed
}

class OnboardingViewModel(
    private val onboardingStore: OnboardingStore,
    private val deviceSetupManager: DeviceSetupManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        refreshStatuses()
    }

    fun refreshStatuses() {
        _uiState.update {
            it.copy(
                notificationsGranted = deviceSetupManager.canPostNotifications(),
                cameraGranted = deviceSetupManager.canUseCamera(),
                exactAlarmGranted = deviceSetupManager.canScheduleExactAlarms(),
                batteryOptimizationIgnored = deviceSetupManager.isIgnoringBatteryOptimizations(),
            )
        }
    }

    fun setQrPlacementConfirmed(confirmed: Boolean) {
        _uiState.update { it.copy(hasPlacedQrAwayFromBed = confirmed) }
    }

    fun markCompleted() {
        onboardingStore.markCompleted()
    }

    companion object {
        fun factory(
            onboardingStore: OnboardingStore,
            deviceSetupManager: DeviceSetupManager,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OnboardingViewModel(onboardingStore, deviceSetupManager) as T
                }
            }
        }
    }
}
