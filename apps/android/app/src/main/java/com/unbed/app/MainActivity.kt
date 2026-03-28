package com.unbed.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.unbed.app.alarmsettings.alarmSettingsRoute
import com.unbed.app.setup.onboardingRoute
import com.unbed.app.ui.theme.unbedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            unbedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    mainRoute()
                }
            }
        }
    }
}

@Composable
private fun mainRoute() {
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as UnbedApplication
    val onboardingCompleted by application.appContainer.onboardingStore.observeCompleted().collectAsState()
    var showOnboarding by remember { mutableStateOf(false) }

    if (!onboardingCompleted || showOnboarding) {
        onboardingRoute(
            onCompleted = {
                showOnboarding = false
            },
        )
    } else {
        alarmSettingsRoute(
            onOpenSetup = {
                showOnboarding = true
            },
        )
    }
}
