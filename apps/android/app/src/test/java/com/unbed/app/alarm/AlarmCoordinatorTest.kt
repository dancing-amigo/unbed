package com.unbed.app.alarm

import com.google.common.truth.Truth.assertThat
import com.unbed.core.model.AlarmConfig
import com.unbed.core.model.AlarmSession
import com.unbed.core.model.AlarmState
import com.unbed.domain.alarm.AlarmConfigRepository
import com.unbed.domain.alarm.AlarmSessionRepository
import com.unbed.domain.alarm.AlarmStateMachine
import com.unbed.domain.alarm.ManualReleaseHandler
import com.unbed.domain.alarm.NextTrigger
import com.unbed.domain.alarm.NextTriggerCalculator
import com.unbed.domain.alarm.NextTriggerType
import com.unbed.domain.alarm.ReleaseConditionHandlerRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class AlarmCoordinatorTest {
    private val now: Instant = Instant.parse("2026-03-28T14:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    private val configRepository = FakeAlarmConfigRepository()
    private val sessionRepository = FakeAlarmSessionRepository()
    private val scheduler = FakeAlarmScheduler()
    private val coordinator =
        AlarmCoordinator(
            configRepository = configRepository,
            sessionRepository = sessionRepository,
            stateMachine = AlarmStateMachine(),
            alarmScheduler = scheduler,
            releaseConditionHandlerRegistry =
                ReleaseConditionHandlerRegistry(
                    handlers = listOf(ManualReleaseHandler(AlarmStateMachine())),
                ),
            calculator = NextTriggerCalculator(ZoneId.of("America/Vancouver")),
            clock = clock,
        )

    @Test
    fun `submit qr mismatch keeps scanning state and skips reschedule`() =
        runBlocking {
            configRepository.config = enabledConfig()
            val session =
                AlarmSession(
                    sessionId = "session-1",
                    alarmId = 1L,
                    scheduledAt = now,
                    state = AlarmState.SCANNING_QR,
                )
            sessionRepository.session = session

            val result = coordinator.submitQrCode(session.sessionId, "WRONG")

            assertThat(result).isEqualTo(QrSubmissionResult.Mismatch)
            assertThat(sessionRepository.session?.state).isEqualTo(AlarmState.SCANNING_QR)
            assertThat(scheduler.rescheduleCalls).isEqualTo(0)
        }

    @Test
    fun `submit qr match snoozes session and reschedules`() =
        runBlocking {
            configRepository.config = enabledConfig()
            val session =
                AlarmSession(
                    sessionId = "session-1",
                    alarmId = 1L,
                    scheduledAt = now,
                    state = AlarmState.SCANNING_QR,
                )
            sessionRepository.session = session

            val result = coordinator.submitQrCode(session.sessionId, "UNBED_MVP_FIXED_QR")

            assertThat(result).isInstanceOf(QrSubmissionResult.Matched::class.java)
            assertThat(sessionRepository.session?.state).isEqualTo(AlarmState.SNOOZED_WAITING_RELEASE)
            assertThat(sessionRepository.session?.snoozeUntil).isEqualTo(now.plusSeconds(600))
            assertThat(scheduler.rescheduleCalls).isEqualTo(1)
        }

    @Test
    fun `restore after overdue snooze forces session back to ringing`() =
        runBlocking {
            configRepository.config = enabledConfig()
            sessionRepository.session =
                AlarmSession(
                    sessionId = "session-restore",
                    alarmId = 1L,
                    scheduledAt = now.minusSeconds(900),
                    state = AlarmState.SNOOZED_WAITING_RELEASE,
                    qrValidatedAt = now.minusSeconds(620),
                    snoozeUntil = now.minusSeconds(20),
                    snoozeCycleCount = 1,
                )

            val result = coordinator.restoreAfterSystemEvent(now)

            assertThat(result).isInstanceOf(RestoreAlarmResult.Ringing::class.java)
            assertThat(sessionRepository.session?.state).isEqualTo(AlarmState.RINGING)
            assertThat(sessionRepository.session?.snoozeUntil).isNull()
            assertThat(scheduler.rescheduleCalls).isEqualTo(1)
        }

    @Test
    fun `restore after future snooze keeps session and reschedules future trigger`() =
        runBlocking {
            configRepository.config = enabledConfig()
            val snoozeUntil = now.plusSeconds(300)
            scheduler.nextTrigger =
                NextTrigger(
                    type = NextTriggerType.SESSION_RERING,
                    triggerAt = snoozeUntil,
                    alarmId = 1L,
                    sessionId = "session-future",
                )
            sessionRepository.session =
                AlarmSession(
                    sessionId = "session-future",
                    alarmId = 1L,
                    scheduledAt = now.minusSeconds(900),
                    state = AlarmState.SNOOZED_WAITING_RELEASE,
                    qrValidatedAt = now.minusSeconds(30),
                    snoozeUntil = snoozeUntil,
                    snoozeCycleCount = 1,
                )

            val result = coordinator.restoreAfterSystemEvent(now)

            assertThat(result).isEqualTo(RestoreAlarmResult.Rescheduled(scheduler.nextTrigger))
            assertThat(sessionRepository.session?.state).isEqualTo(AlarmState.SNOOZED_WAITING_RELEASE)
            assertThat(scheduler.nextTrigger?.type).isEqualTo(NextTriggerType.SESSION_RERING)
            assertThat(scheduler.nextTrigger?.triggerAt).isEqualTo(snoozeUntil)
        }

    private fun enabledConfig(): AlarmConfig {
        return AlarmConfig(
            id = 1L,
            time = LocalTime.of(7, 0),
            enabled = true,
        )
    }
}

private class FakeAlarmConfigRepository : AlarmConfigRepository {
    private val configFlow = MutableStateFlow<AlarmConfig?>(null)
    var config: AlarmConfig?
        get() = configFlow.value
        set(value) {
            configFlow.value = value
        }

    override fun observeConfig(): Flow<AlarmConfig?> = configFlow.asStateFlow()

    override suspend fun getConfig(): AlarmConfig? = config

    override suspend fun upsertConfig(config: AlarmConfig) {
        this.config = config
    }

    override suspend fun setEnabled(
        alarmId: Long,
        enabled: Boolean,
    ) {
        config = config?.takeIf { it.id == alarmId }?.copy(enabled = enabled)
    }
}

private class FakeAlarmSessionRepository : AlarmSessionRepository {
    private val sessionFlow = MutableStateFlow<AlarmSession?>(null)
    var session: AlarmSession?
        get() = sessionFlow.value
        set(value) {
            sessionFlow.value = value
        }

    override fun observeActiveSession(): Flow<AlarmSession?> = sessionFlow.asStateFlow()

    override suspend fun getActiveSession(): AlarmSession? = session

    override suspend fun getSession(sessionId: String): AlarmSession? {
        return session?.takeIf { it.sessionId == sessionId }
    }

    override suspend fun upsertSession(session: AlarmSession) {
        this.session = session
    }
}

private class FakeAlarmScheduler : AlarmScheduler {
    var rescheduleCalls: Int = 0
        private set
    var nextTrigger: NextTrigger? = null

    override suspend fun reschedule(): NextTrigger? {
        rescheduleCalls += 1
        return nextTrigger
    }

    override fun canScheduleExactAlarms(): Boolean = true

    override fun openExactAlarmSettings() = Unit
}
