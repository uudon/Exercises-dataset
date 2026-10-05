package com.don.homefitness.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "plan_exercises",
    foreignKeys = [ForeignKey(
        entity = WorkoutPlanEntity::class,
        parentColumns = ["id"],
        childColumns = ["planId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("planId")],
)
data class PlanExerciseEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val exerciseId: String,
    val position: Int,
)

@Entity(
    tableName = "planned_sets",
    foreignKeys = [ForeignKey(
        entity = PlanExerciseEntity::class,
        parentColumns = ["id"],
        childColumns = ["planExerciseId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("planExerciseId")],
)
data class PlannedSetEntity(
    @PrimaryKey val id: String,
    val planExerciseId: String,
    val position: Int,
    val mode: String,
    val targetReps: Int?,
    val targetSeconds: Int?,
    val targetWeightGrams: Long?,
    val restSeconds: Int,
)
