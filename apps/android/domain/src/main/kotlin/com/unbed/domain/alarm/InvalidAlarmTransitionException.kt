package com.unbed.domain.alarm

import com.unbed.core.model.AlarmState

class InvalidAlarmTransitionException(
    from: AlarmState,
    event: String,
    allowedStates: Set<AlarmState>,
) : IllegalStateException(
        "Event '$event' is invalid from state '$from'. Allowed: $allowedStates",
    )
