package com.unbed.app.setup

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OnboardingStore(context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val completedFlow =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_COMPLETED, false))

    fun observeCompleted(): StateFlow<Boolean> = completedFlow.asStateFlow()

    fun markCompleted() {
        sharedPreferences.edit().putBoolean(KEY_COMPLETED, true).apply()
        completedFlow.value = true
    }

    companion object {
        private const val PREFERENCES_NAME: String = "unbed_onboarding"
        private const val KEY_COMPLETED: String = "completed"
    }
}
