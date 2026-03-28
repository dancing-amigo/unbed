package com.unbed.domain.alarm

import com.unbed.core.model.AlarmConfig
import com.unbed.core.model.AlarmSession
import com.unbed.core.model.ReleaseConditionType
import java.time.Instant

interface ReleaseConditionHandler {
    fun supports(releaseCondition: ReleaseConditionType): Boolean

    fun complete(
        config: AlarmConfig,
        session: AlarmSession,
        releasedAt: Instant,
    ): AlarmSession
}

class ReleaseConditionHandlerRegistry(
    private val handlers: List<ReleaseConditionHandler>,
) {
    fun complete(
        config: AlarmConfig,
        session: AlarmSession,
        releasedAt: Instant,
    ): AlarmSession {
        val handler =
            handlers.firstOrNull { it.supports(config.releaseCondition) }
                ?: throw UnsupportedReleaseConditionException(config.releaseCondition)
        return handler.complete(config, session, releasedAt)
    }
}

class UnsupportedReleaseConditionException(
    releaseCondition: ReleaseConditionType,
) : IllegalStateException("No release handler is registered for $releaseCondition")

class ManualReleaseHandler(
    private val stateMachine: AlarmStateMachine,
) : ReleaseConditionHandler {
    override fun supports(releaseCondition: ReleaseConditionType): Boolean {
        return releaseCondition is ReleaseConditionType.ManualRelease
    }

    override fun complete(
        config: AlarmConfig,
        session: AlarmSession,
        releasedAt: Instant,
    ): AlarmSession {
        val clearedSession = stateMachine.completeManualRelease(session, releasedAt)
        return stateMachine.finalizeReleasedSession(config, clearedSession)
    }
}
