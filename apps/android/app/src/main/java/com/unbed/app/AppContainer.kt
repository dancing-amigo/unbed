package com.unbed.app

import android.content.Context
import androidx.room.Room
import com.unbed.app.alarm.AlarmCoordinator
import com.unbed.app.alarm.AlarmNotifier
import com.unbed.app.alarm.AlarmPlaybackController
import com.unbed.app.alarm.AndroidAlarmScheduler
import com.unbed.app.logging.AndroidAppLogger
import com.unbed.app.setup.DeviceSetupManager
import com.unbed.app.setup.OnboardingStore
import com.unbed.core.database.UnbedDatabase
import com.unbed.core.database.repository.RoomAlarmStore
import com.unbed.domain.alarm.AlarmStateMachine
import com.unbed.domain.alarm.ManualReleaseHandler
import com.unbed.domain.alarm.NextTriggerCalculator
import com.unbed.domain.alarm.ReleaseConditionHandlerRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock

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
    private val nextTriggerCalculator = NextTriggerCalculator()
    private val releaseConditionHandlerRegistry =
        ReleaseConditionHandlerRegistry(
            handlers = listOf(ManualReleaseHandler(stateMachine)),
        )
    val logger = AndroidAppLogger()
    val alarmPlaybackController = AlarmPlaybackController(context.applicationContext)
    val alarmNotifier = AlarmNotifier(context.applicationContext)
    val onboardingStore = OnboardingStore(context.applicationContext)
    val deviceSetupManager = DeviceSetupManager(context.applicationContext)

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
            releaseConditionHandlerRegistry = releaseConditionHandlerRegistry,
            calculator = nextTriggerCalculator,
            clock = clock,
        )

    init {
        alarmNotifier.createChannels()
    }
}
