package com.unbed.app.ringing

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unbed.app.UnbedApplication
import com.unbed.app.alarm.AlarmNotifier
import com.unbed.app.ui.theme.unbedTheme

class AlarmRingingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureLockScreenBehavior()
        (application as UnbedApplication).appContainer.alarmPlaybackController.start()

        val sessionId = intent.getStringExtra(AlarmNotifier.EXTRA_SESSION_ID).orEmpty()
        setContent {
            unbedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ringingScreen(
                        onOpenQr = {
                            startActivity(
                                Intent(this, QrScanPlaceholderActivity::class.java).apply {
                                    putExtra(AlarmNotifier.EXTRA_SESSION_ID, sessionId)
                                },
                            )
                        },
                    )
                }
            }
        }
    }

    private fun configureLockScreenBehavior() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguardManager = getSystemService(KeyguardManager::class.java)
            keyguardManager?.requestDismissKeyguard(this, null)
        }
    }
}

@androidx.compose.runtime.Composable
private fun ringingScreen(onOpenQr: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, androidx.compose.ui.Alignment.CenterVertically),
    ) {
        Text(
            text = "Alarm active",
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = "No stop button is available here. Move away from bed and open the QR flow.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onOpenQr) {
            Text("Open QR scan")
        }
    }
}
