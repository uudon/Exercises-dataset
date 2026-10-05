package com.don.homefitness.feature.plan

import androidx.room.withTransaction
import com.don.homefitness.core.model.PlanDraft
import com.don.homefitness.core.model.PlannedExercise
import com.don.homefitness.core.model.PlannedSet
import com.don.homefitness.core.model.SetMode
import com.don.homefitness.core.model.WorkoutPlan
import com.don.homefitness.core.validation.SetValidator
import com.don.homefitness.data.db.FitnessDatabase
import com.don.homefitness.data.db.entity.PlanExerciseEntity
import com.don.homefitness.data.db.entity.PlannedSetEntity
import com.don.homefitness.data.db.entity.WorkoutPlanEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import java.util.UUID

class PlanRepository(private val database: FitnessDatabase) {
    private val dao = database.planDao()

    suspend fun savePlan(draft: PlanDraft): String {
        val validation = SetValidator.validatePlan(draft)
        require(validation.isValid) { validation.errors.first() }
        val now = System.currentTimeMillis()
        val planId = UUID.randomUUID().toString()
        savePlanInternal(planId, draft, now, now)
        return planId
    }

    fun observePlans(): Flow<List<WorkoutPlan>> = dao.observePlans().map { plans ->
        plans.map { plan -> loadPlan(plan) }
    }

    suspend fun duplicatePlan(planId: String): String {
        val original = loadPlan(dao.findPlan(planId) ?: error("计划不存在"))
        val duplicate = PlanDraft(
            name = "${original.name}（副本）".take(40),
            exercises = original.exercises.map { exercise ->
                com.don.homefitness.core.model.PlanExerciseDraft(
                    exerciseId = exercise.exerciseId,
                    sets = exercise.sets.map { set ->
                        com.don.homefitness.core.model.PlannedSetDraft(
                            mode = set.mode,
                            targetReps = set.targetReps,
                            targetSeconds = set.targetSeconds,
                            targetWeightKg = set.targetWeightGrams?.let { grams -> "%.2f".format(Locale.US, grams / 1000.0) },
                            restSeconds = set.restSeconds,
                        )
                    },
                )
            },
        )
        return savePlan(duplicate)
    }

    suspend fun deletePlan(planId: String) {
        database.withTransaction {
            val exercises = dao.findExercises(planId)
            val exerciseIds = exercises.map(PlanExerciseEntity::id)
            if (exerciseIds.isNotEmpty()) dao.deleteSets(exerciseIds)
            dao.deleteExercises(planId)
            dao.deletePlan(planId)
        }
    }

    suspend fun updatePlan(planId: String, draft: PlanDraft) {
        val validation = SetValidator.validatePlan(draft)
        require(validation.isValid) { validation.errors.first() }
        val existing = dao.findPlan(planId) ?: error("计划不存在")
        val now = System.currentTimeMillis()
        savePlanInternal(planId, draft, existing.createdAt, now)
    }

    private suspend fun savePlanInternal(planId: String, draft: PlanDraft, createdAt: Long, updatedAt: Long) {
        database.withTransaction {
            dao.insertPlan(WorkoutPlanEntity(planId, draft.name.trim(), createdAt, updatedAt))
            val oldExercises = dao.findExercises(planId)
            val oldExerciseIds = oldExercises.map(PlanExerciseEntity::id)
            if (oldExerciseIds.isNotEmpty()) dao.deleteSets(oldExerciseIds)
            dao.deleteExercises(planId)
            val exerciseEntities = draft.exercises.mapIndexed { position, draftExercise ->
                PlanExerciseEntity(UUID.randomUUID().toString(), planId, draftExercise.exerciseId, position)
            }
            dao.insertExercises(exerciseEntities)
            dao.insertSets(
                draft.exercises.zip(exerciseEntities).flatMap { (draftExercise, exercise) ->
                    draftExercise.sets.mapIndexed { position, set ->
                        PlannedSetEntity(
                            id = UUID.randomUUID().toString(),
                            planExerciseId = exercise.id,
                            position = position,
                            mode = set.mode.name,
                            targetReps = set.targetReps,
                            targetSeconds = set.targetSeconds,
                            targetWeightGrams = SetValidator.weightToGrams(set.targetWeightKg),
                            restSeconds = set.restSeconds,
                        )
                    }
                },
            )
        }
    }

    private suspend fun loadPlan(entity: WorkoutPlanEntity): WorkoutPlan {
        val exercises = dao.findExercises(entity.id)
        val setsByExercise = if (exercises.isEmpty()) emptyMap() else {
            dao.findSets(exercises.map(PlanExerciseEntity::id)).groupBy(PlannedSetEntity::planExerciseId)
        }
        return WorkoutPlan(
            id = entity.id,
            name = entity.name,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            exercises = exercises.map { exercise ->
                PlannedExercise(
                    id = exercise.id,
                    exerciseId = exercise.exerciseId,
                    position = exercise.position,
                    sets = setsByExercise[exercise.id].orEmpty().map { set ->
                        PlannedSet(
                            id = set.id,
                            position = set.position,
                            mode = SetMode.valueOf(set.mode),
                            targetReps = set.targetReps,
                            targetSeconds = set.targetSeconds,
                            targetWeightGrams = set.targetWeightGrams,
                            restSeconds = set.restSeconds,
                        )
                    },
                )
            },
        )
    }
}
