package com.unbed.domain.alarm

import java.time.Instant

data class NextTrigger(
    val type: NextTriggerType,
    val triggerAt: Instant,
    val alarmId: Long,
    val sessionId: String? = null,
)

enum class NextTriggerType {
    REGULAR_ALARM,
    SESSION_RERING,
}
