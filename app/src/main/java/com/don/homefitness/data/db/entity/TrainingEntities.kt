package com.don.homefitness.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions", indices = [Index(value = ["status"]), Index(value = ["localDate"])])
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val planId: String?,
    val planNameSnapshot: String,
    val status: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val localDate: String,
    val zoneId: String,
)

@Entity(
    tableName = "session_exercises",
    foreignKeys = [ForeignKey(entity = WorkoutSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId")],
)
data class SessionExerciseEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val nameSnapshot: String,
    val equipmentSnapshot: String,
    val position: Int,
)

@Entity(
    tableName = "session_sets",
    foreignKeys = [ForeignKey(entity = SessionExerciseEntity::class, parentColumns = ["id"], childColumns = ["sessionExerciseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionExerciseId")],
)
data class SessionSetEntity(
    @PrimaryKey val id: String,
    val sessionExerciseId: String,
    val position: Int,
    val mode: String,
    val targetReps: Int?,
    val targetSeconds: Int?,
    val targetWeightGrams: Long?,
    val actualReps: Int?,
    val actualSeconds: Int?,
    val actualWeightGrams: Long?,
    val status: String,
    val loadType: String,
    val modifiedAt: Long?,
)

@Entity(tableName = "rest_states")
data class RestStateEntity(
    @PrimaryKey val sessionId: String,
    val bootMarker: String,
    val deadlineElapsedMs: Long,
    val durationSeconds: Int,
)
