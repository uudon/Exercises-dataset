package com.don.homefitness.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.don.homefitness.data.db.entity.ExerciseEntity
import com.don.homefitness.data.db.entity.FavoriteEntity
import com.don.homefitness.data.db.entity.PlanExerciseEntity
import com.don.homefitness.data.db.entity.PlannedSetEntity
import com.don.homefitness.data.db.entity.WorkoutPlanEntity

@Database(
    entities = [
        ExerciseEntity::class,
        FavoriteEntity::class,
        WorkoutPlanEntity::class,
        PlanExerciseEntity::class,
        PlannedSetEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun planDao(): PlanDao
}
