package com.unbed.app.snooze

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.unbed.app.UnbedApplication
import com.unbed.app.alarm.AlarmNotifier
import com.unbed.app.ui.theme.unbedTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SnoozeWaitingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionId = intent.getStringExtra(AlarmNotifier.EXTRA_SESSION_ID).orEmpty()
        val appContainer = (application as UnbedApplication).appContainer

        setContent {
            unbedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    snoozeWaitingRoute(
                        sessionId = sessionId,
                        onReleaseComplete = {
                            appContainer.applicationScope.launch {
                                appContainer.alarmCoordinator.completeRelease(sessionId)
                            }
                            startActivity(
                                Intent(this, com.unbed.app.MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                },
                            )
                            finish()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun snoozeWaitingRoute(
    sessionId: String,
    onReleaseComplete: () -> Unit,
) {
    val application = LocalContext.current.applicationContext as UnbedApplication
    val appContainer = application.appContainer
    val sessionState =
        produceState<com.unbed.core.model.AlarmSession?>(initialValue = null, sessionId) {
            value = appContainer.alarmCoordinator.getSession(sessionId)
        }

    snoozeWaitingScreen(
        snoozeUntil = sessionState.value?.snoozeUntil,
        onReleaseComplete = onReleaseComplete,
    )
}

@Composable
private fun snoozeWaitingScreen(
    snoozeUntil: Instant?,
    onReleaseComplete: () -> Unit,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val remaining = rememberCountdown(snoozeUntil)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Alarm snoozed",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "QR matched. The alarm will ring again unless you confirm release completion.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text =
                snoozeUntil?.let {
                    "Next re-ring: ${it.atZone(ZoneId.systemDefault()).format(formatter)}"
                } ?: "Waiting for next alarm state update.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Time remaining: ${remaining.toMinutes()} min ${remaining.seconds % 60} sec",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onReleaseComplete) {
            Text("Release complete")
        }
    }
}

@Composable
private fun rememberCountdown(snoozeUntil: Instant?): Duration {
    val countdown =
        produceState(initialValue = Duration.ZERO, snoozeUntil) {
            while (true) {
                value =
                    snoozeUntil?.let { target ->
                        Duration.between(Instant.now(), target).coerceAtLeast(Duration.ZERO)
                    } ?: Duration.ZERO
                delay(COUNTDOWN_TICK_MS)
            }
        }
    return countdown.value
}

private const val COUNTDOWN_TICK_MS: Long = 1_000L
