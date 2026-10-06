package com.don.homefitness.data.body

import kotlinx.serialization.Serializable

@Serializable
data class MuscleRegion(
    val regionId: String,
    val muscleGroupId: String,
    val displayNameZh: String,
    val meshNodeIdsByGender: Map<BodyGender, Set<String>>,
)
