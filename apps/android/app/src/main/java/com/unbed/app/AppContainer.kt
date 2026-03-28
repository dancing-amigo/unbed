package com.unbed.app

import android.content.Context
import androidx.room.Room
import com.unbed.app.alarm.AlarmCoordinator
import com.unbed.app.alarm.AlarmNotifier
import com.unbed.app.alarm.AlarmPlaybackController
import com.unbed.app.alarm.AndroidAlarmScheduler
import com.unbed.core.database.UnbedDatabase
import com.unbed.core.database.repository.RoomAlarmStore
import com.unbed.domain.alarm.AlarmStateMachine
import com.unbed.domain.alarm.NextTriggerCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import java.time.ZoneId

class AppContainer(context: Context) {
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: UnbedDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            UnbedDatabase::class.java,
            "unbed.db",
        ).build()

    private val store =
        RoomAlarmStore(
            alarmConfigDao = database.alarmConfigDao(),
            alarmSessionDao = database.alarmSessionDao(),
        )

    private val clock: Clock = Clock.systemDefaultZone()
    private val stateMachine = AlarmStateMachine()
    private val nextTriggerCalculator = NextTriggerCalculator(ZoneId.systemDefault())
    val alarmPlaybackController = AlarmPlaybackController(context.applicationContext)
    val alarmNotifier = AlarmNotifier(context.applicationContext)

    val alarmScheduler =
        AndroidAlarmScheduler(
            context = context.applicationContext,
            configRepository = store,
            sessionRepository = store,
            calculator = nextTriggerCalculator,
            clock = clock,
        )

    val alarmCoordinator =
        AlarmCoordinator(
            configRepository = store,
            sessionRepository = store,
            stateMachine = stateMachine,
            alarmScheduler = alarmScheduler,
            calculator = nextTriggerCalculator,
            clock = clock,
        )

    init {
        alarmNotifier.createChannels()
    }
}
