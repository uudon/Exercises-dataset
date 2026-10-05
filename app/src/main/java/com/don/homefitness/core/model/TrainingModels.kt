package com.don.homefitness.core.model

data class SetResult(
    val mode: SetMode,
    val reps: Int? = null,
    val seconds: Int? = null,
    val weightGrams: Long? = null,
    val loadType: LoadType = if (weightGrams == null) LoadType.BODYWEIGHT else LoadType.EXTERNAL,
)

enum class SessionStatus { ACTIVE, COMPLETED, CANCELLED }
enum class SetStatus { PENDING, COMPLETED, SKIPPED }
enum class TrainingLocation { HOME, GYM }

data class SessionSet(
    val id: String,
    val sessionExerciseId: String,
    val position: Int,
    val mode: SetMode,
    val targetReps: Int?,
    val targetSeconds: Int?,
    val targetWeightGrams: Long?,
    val actualReps: Int?,
    val actualSeconds: Int?,
    val actualWeightGrams: Long?,
    val status: SetStatus,
    val loadType: LoadType,
    val modifiedAt: Long? = null,
)

data class SessionExercise(
    val id: String,
    val sessionId: String,
    val exerciseId: String,
    val nameSnapshot: String,
    val equipmentSnapshot: String,
    val position: Int,
    val sets: List<SessionSet>,
)

data class SessionDetail(
    val id: String,
    val planId: String?,
    val planNameSnapshot: String,
    val status: SessionStatus,
    val startedAt: Long,
    val finishedAt: Long?,
    val localDate: String,
    val zoneId: String,
    val exercises: List<SessionExercise>,
)

data class SessionSummary(
    val id: String,
    val planName: String,
    val status: SessionStatus,
    val startedAt: Long,
    val localDate: String,
    val completedSets: Int,
)

data class TrendPoint(
    val date: String,
    val value: Long,
    val mode: SetMode,
    val loadType: LoadType,
)
