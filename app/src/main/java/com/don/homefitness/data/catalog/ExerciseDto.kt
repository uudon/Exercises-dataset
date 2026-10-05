package com.don.homefitness.data.catalog

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseDto(
    val id: String,
    val name: String,
    val category: String,
    val body_part: String,
    val equipment: String,
    val instructions: Map<String, String>,
    val instruction_steps: Map<String, List<String>>,
    val muscle_group: String,
    val secondary_muscles: List<String>,
    val target: String,
    val media_id: String,
    val image: String,
    val gif_url: String,
    val attribution: String,
    val created_at: String,
)

@Serializable
data class ExerciseOverlayDto(
    val id: String,
    val nameZh: String,
    val aliasesZh: List<String> = emptyList(),
    val requiredEquipment: List<String>,
    val homeEligible: Boolean,
    val reviewStatus: String,
)

data class CatalogExercise(
    val id: String,
    val originalName: String,
    val nameZh: String,
    val aliasesZh: List<String>,
    val bodyPart: String,
    val equipment: String,
    val target: String,
    val secondaryMuscles: List<String>,
    val instructionsZh: String,
    val instructionStepsZh: List<String>,
    val requiredEquipment: Set<String>,
    val homeEligible: Boolean,
    val reviewStatus: String,
    val sourceCommit: String,
    val isFavorite: Boolean = false,
)

data class ImportError(val id: String?, val message: String)

data class ImportResult(
    val acceptedCount: Int,
    val rejectedCount: Int,
    val errors: List<ImportError>,
)
