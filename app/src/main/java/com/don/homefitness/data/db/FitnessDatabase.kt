package com.don.homefitness.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.don.homefitness.data.db.entity.ExerciseEntity
import com.don.homefitness.data.db.entity.FavoriteEntity

@Database(
    entities = [ExerciseEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
}
