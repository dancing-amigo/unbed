package com.unbed.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.unbed.app.UnbedApplication
import com.unbed.domain.alarm.NextTriggerType
import kotlinx.coroutines.launch
import java.time.Instant

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val application = context.applicationContext as? UnbedApplication ?: return
        val pendingResult = goAsync()
        val triggerType =
            intent.getStringExtra(AndroidAlarmScheduler.EXTRA_TRIGGER_TYPE)
                ?.let(NextTriggerType::valueOf)
                ?: return pendingResult.finish()
        val triggerAt =
            intent.getLongExtra(AndroidAlarmScheduler.EXTRA_TRIGGER_AT_EPOCH_MILLIS, -1L)
                .takeIf { it > 0L }
                ?.let(Instant::ofEpochMilli)
                ?: Instant.now()
        val sessionId = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_SESSION_ID)

        application.appContainer.applicationScope.launch {
            try {
                application.appContainer.logger.info(
                    tag = "AlarmReceiver",
                    message = "Received ${triggerType.name} at $triggerAt for session=${sessionId ?: "none"}",
                )
                val ringingSession =
                    application.appContainer.alarmCoordinator.handleTrigger(
                        triggerType = triggerType,
                        triggerAt = triggerAt,
                        sessionId = sessionId,
                    )
                if (ringingSession != null) {
                    application.appContainer.alarmPlaybackController.start()
                    application.appContainer.alarmNotifier.showRingingNotification(
                        sessionId = ringingSession.sessionId,
                    )
                } else {
                    application.appContainer.logger.info(
                        tag = "AlarmReceiver",
                        message = "No active ringing session remained after trigger handling",
                    )
                    application.appContainer.alarmPlaybackController.stop()
                    application.appContainer.alarmNotifier.dismissRingingNotification()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
