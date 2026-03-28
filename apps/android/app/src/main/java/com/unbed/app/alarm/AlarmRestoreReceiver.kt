package com.unbed.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.unbed.app.UnbedApplication
import kotlinx.coroutines.launch

class AlarmRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val application = context.applicationContext as? UnbedApplication ?: return
        val pendingResult = goAsync()

        application.appContainer.applicationScope.launch {
            try {
                when (val result = application.appContainer.alarmCoordinator.restoreAfterSystemEvent()) {
                    is RestoreAlarmResult.Ringing -> {
                        application.appContainer.alarmPlaybackController.start()
                        application.appContainer.alarmNotifier.showRingingNotification(
                            sessionId = result.session.sessionId,
                        )
                    }

                    is RestoreAlarmResult.Rescheduled -> {
                        application.appContainer.alarmPlaybackController.stop()
                        application.appContainer.alarmNotifier.dismissRingingNotification()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
