package com.don.homefitness.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val originalName: String,
    val nameZh: String,
    val aliasesZh: String,
    val bodyPart: String,
    val equipment: String,
    val target: String,
    val secondaryMuscles: String,
    val instructionsZh: String,
    val instructionStepsZh: String,
    val requiredEquipment: String,
    val homeEligible: Boolean,
    val reviewStatus: String,
    val sourceCommit: String,
)
