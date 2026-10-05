package com.don.homefitness.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.don.homefitness.data.db.entity.PlanExerciseEntity
import com.don.homefitness.data.db.entity.PlannedSetEntity
import com.don.homefitness.data.db.entity.WorkoutPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Query("SELECT * FROM workout_plans ORDER BY updatedAt DESC")
    fun observePlans(): Flow<List<WorkoutPlanEntity>>
    @Query("SELECT * FROM workout_plans")
    suspend fun allPlans(): List<WorkoutPlanEntity>
    @Query("SELECT * FROM plan_exercises")
    suspend fun allExercises(): List<PlanExerciseEntity>
    @Query("SELECT * FROM planned_sets")
    suspend fun allSets(): List<PlannedSetEntity>

    @Query("SELECT * FROM workout_plans WHERE id = :planId")
    suspend fun findPlan(planId: String): WorkoutPlanEntity?

    @Query("SELECT * FROM plan_exercises WHERE planId = :planId ORDER BY position")
    suspend fun findExercises(planId: String): List<PlanExerciseEntity>

    @Query("SELECT * FROM planned_sets WHERE planExerciseId IN (:planExerciseIds) ORDER BY position")
    suspend fun findSets(planExerciseIds: List<String>): List<PlannedSetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: WorkoutPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<PlanExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<PlannedSetEntity>)

    @Query("DELETE FROM planned_sets WHERE planExerciseId IN (:planExerciseIds)")
    suspend fun deleteSets(planExerciseIds: List<String>)

    @Query("DELETE FROM plan_exercises WHERE planId = :planId")
    suspend fun deleteExercises(planId: String)

    @Query("DELETE FROM workout_plans WHERE id = :planId")
    suspend fun deletePlan(planId: String)
    @Query("DELETE FROM planned_sets")
    suspend fun deleteAllSets()
    @Query("DELETE FROM plan_exercises")
    suspend fun deleteAllExercises()
    @Query("DELETE FROM workout_plans")
    suspend fun deleteAllPlans()

    @Query("UPDATE plan_exercises SET exerciseId = :exerciseId WHERE id = :planExerciseId")
    suspend fun replaceExercise(planExerciseId: String, exerciseId: String)
}
