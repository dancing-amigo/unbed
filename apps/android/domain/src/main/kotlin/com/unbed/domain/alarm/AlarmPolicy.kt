package com.unbed.domain.alarm

import com.unbed.core.model.QrConfig
import java.time.Duration

object AlarmPolicy {
    const val DEFAULT_SNOOZE_MINUTES: Long = 10L
    const val MAX_RERING_COUNT: Int = 10
    val DEFAULT_SNOOZE_DURATION: Duration = Duration.ofMinutes(DEFAULT_SNOOZE_MINUTES)
    val DEFAULT_QR_CONFIG: QrConfig =
        QrConfig(
            fixedValue = "UNBED_MVP_FIXED_QR",
        )
}
