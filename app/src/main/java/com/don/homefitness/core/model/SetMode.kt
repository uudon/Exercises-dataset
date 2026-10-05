package com.don.homefitness.core.model

enum class SetMode {
    REPS,
    DURATION,
}

data class PlannedSetDraft(
    val mode: SetMode,
    val targetReps: Int? = null,
    val targetSeconds: Int? = null,
    val targetWeightKg: String? = null,
    val restSeconds: Int = 60,
)

data class PlanExerciseDraft(
    val exerciseId: String,
    val sets: List<PlannedSetDraft> = listOf(PlannedSetDraft(SetMode.REPS, targetReps = 10)),
)

data class PlanDraft(
    val name: String,
    val exercises: List<PlanExerciseDraft>,
)

data class WorkoutPlan(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val exercises: List<PlannedExercise>,
)

data class PlannedExercise(
    val id: String,
    val exerciseId: String,
    val position: Int,
    val sets: List<PlannedSet>,
)

data class PlannedSet(
    val id: String,
    val position: Int,
    val mode: SetMode,
    val targetReps: Int?,
    val targetSeconds: Int?,
    val targetWeightGrams: Long?,
    val restSeconds: Int,
)
