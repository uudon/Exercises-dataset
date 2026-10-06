package com.don.homefitness.data.body

import kotlinx.serialization.Serializable

@Serializable
data class MuscleActionMapping(
    val muscleGroupId: String,
    val primaryExerciseIds: List<String>,
    val secondaryExerciseIds: List<String>,
)

data class BodyMappingCheckReport(
    val valid: Boolean,
    val errors: List<String>,
)
