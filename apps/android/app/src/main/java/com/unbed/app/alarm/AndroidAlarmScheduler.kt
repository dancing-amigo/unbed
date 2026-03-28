package com.unbed.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.unbed.domain.alarm.AlarmConfigRepository
import com.unbed.domain.alarm.AlarmSessionRepository
import com.unbed.domain.alarm.NextTrigger
import com.unbed.domain.alarm.NextTriggerCalculator
import java.time.Clock

class AndroidAlarmScheduler(
    private val context: Context,
    private val configRepository: AlarmConfigRepository,
    private val sessionRepository: AlarmSessionRepository,
    private val calculator: NextTriggerCalculator,
    private val clock: Clock,
) : AlarmScheduler {
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    override suspend fun reschedule(): NextTrigger? {
        val nextTrigger =
            calculator.calculate(
                config = configRepository.getConfig(),
                activeSession = sessionRepository.getActiveSession(),
                now = clock.instant(),
            )
        cancel()
        nextTrigger?.let(::schedule)
        return nextTrigger
    }

    fun cancel() {
        alarmManager.cancel(createPendingIntent(null))
    }

    override fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    override fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent =
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            context.startActivity(intent)
        }
    }

    private fun schedule(trigger: NextTrigger) {
        val pendingIntent = createPendingIntent(trigger)
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                trigger.triggerAt.toEpochMilli(),
                pendingIntent,
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                trigger.triggerAt.toEpochMilli(),
                pendingIntent,
            )
        }
    }

    private fun createPendingIntent(trigger: NextTrigger?): PendingIntent {
        val intent =
            Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_TRIGGER_ALARM
                putExtra(EXTRA_TRIGGER_TYPE, trigger?.type?.name)
                putExtra(EXTRA_TRIGGER_AT_EPOCH_MILLIS, trigger?.triggerAt?.toEpochMilli() ?: -1L)
                putExtra(EXTRA_SESSION_ID, trigger?.sessionId)
            }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_TRIGGER_ALARM: String = "com.unbed.app.action.TRIGGER_ALARM"
        const val EXTRA_TRIGGER_TYPE: String = "extra_trigger_type"
        const val EXTRA_TRIGGER_AT_EPOCH_MILLIS: String = "extra_trigger_at_epoch_millis"
        const val EXTRA_SESSION_ID: String = "extra_session_id"
        private const val REQUEST_CODE_ALARM: Int = 2001
    }
}
