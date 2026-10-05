package com.don.homefitness.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.don.homefitness.data.db.entity.ExerciseEntity
import com.don.homefitness.data.db.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY nameZh")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceExercises(exercises: List<ExerciseEntity>)

    @Query("DELETE FROM exercises")
    suspend fun deleteAllExercises()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM exercise_favorites WHERE exerciseId = :exerciseId")
    suspend fun removeFavorite(exerciseId: String)

    @Query("SELECT exerciseId FROM exercise_favorites")
    fun observeFavoriteIds(): Flow<List<String>>
}
