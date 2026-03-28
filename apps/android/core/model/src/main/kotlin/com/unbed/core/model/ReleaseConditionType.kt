package com.unbed.core.model

sealed interface ReleaseConditionType {
    data object ManualRelease : ReleaseConditionType

    data class StepCountRelease(
        val stepsRequired: Int,
    ) : ReleaseConditionType
}
