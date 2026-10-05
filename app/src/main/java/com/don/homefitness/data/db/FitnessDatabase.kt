package com.don.homefitness.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.don.homefitness.data.db.entity.ExerciseEntity
import com.don.homefitness.data.db.entity.FavoriteEntity
import com.don.homefitness.data.db.entity.PlanExerciseEntity
import com.don.homefitness.data.db.entity.PlannedSetEntity
import com.don.homefitness.data.db.entity.WorkoutPlanEntity
import com.don.homefitness.data.db.entity.WorkoutSessionEntity
import com.don.homefitness.data.db.entity.SessionExerciseEntity
import com.don.homefitness.data.db.entity.SessionSetEntity
import com.don.homefitness.data.db.entity.RestStateEntity

@Database(
    entities = [
        ExerciseEntity::class,
        FavoriteEntity::class,
        WorkoutPlanEntity::class,
        PlanExerciseEntity::class,
        PlannedSetEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        SessionSetEntity::class,
        RestStateEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun planDao(): PlanDao
    abstract fun trainingDao(): TrainingDao
}
