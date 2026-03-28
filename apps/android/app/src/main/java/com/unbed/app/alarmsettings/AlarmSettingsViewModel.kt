package com.unbed.app.alarmsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.unbed.app.alarm.AlarmCoordinator
import com.unbed.core.model.AlarmConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek

class AlarmSettingsViewModel(
    private val alarmCoordinator: AlarmCoordinator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AlarmSettingsUiState())
    val uiState: StateFlow<AlarmSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val config = alarmCoordinator.getOrCreateConfig()
            _uiState.value =
                AlarmSettingsUiState(
                    enabled = config.enabled,
                    time = config.time,
                    repeatDays = config.repeatDays,
                    isLoaded = true,
                    nextTrigger = alarmCoordinator.peekNextTrigger(config),
                    canScheduleExactAlarms = alarmCoordinator.canScheduleExactAlarms(),
                )
        }
    }

    fun onEnabledChange(enabled: Boolean) {
        _uiState.update { it.copy(enabled = enabled) }
    }

    fun onTimeChange(
        hour: Int,
        minute: Int,
    ) {
        _uiState.update { it.copy(time = it.time.withHour(hour).withMinute(minute)) }
    }

    fun onRepeatDayToggle(dayOfWeek: DayOfWeek) {
        _uiState.update { currentState ->
            val updatedDays =
                if (dayOfWeek in currentState.repeatDays) {
                    currentState.repeatDays - dayOfWeek
                } else {
                    currentState.repeatDays + dayOfWeek
                }
            currentState.copy(repeatDays = updatedDays)
        }
    }

    fun openExactAlarmSettings() {
        alarmCoordinator.openExactAlarmSettings()
        _uiState.update { it.copy(canScheduleExactAlarms = alarmCoordinator.canScheduleExactAlarms()) }
    }

    fun save() {
        val currentState = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val nextTrigger =
                alarmCoordinator.saveConfig(
                    AlarmConfig(
                        id = AlarmCoordinator.DEFAULT_ALARM_ID,
                        time = currentState.time,
                        repeatDays = currentState.repeatDays,
                        enabled = currentState.enabled,
                    ),
                )
            _uiState.update {
                it.copy(
                    isSaving = false,
                    nextTrigger = nextTrigger,
                    canScheduleExactAlarms = alarmCoordinator.canScheduleExactAlarms(),
                )
            }
        }
    }

    companion object {
        fun factory(alarmCoordinator: AlarmCoordinator): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AlarmSettingsViewModel(alarmCoordinator) as T
                }
            }
        }
    }
}
