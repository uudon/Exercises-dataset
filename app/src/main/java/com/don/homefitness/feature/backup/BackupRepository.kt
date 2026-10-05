package com.don.homefitness.feature.backup

import androidx.room.withTransaction
import com.don.homefitness.data.db.FitnessDatabase
import com.don.homefitness.data.db.entity.FavoriteEntity
import com.don.homefitness.data.db.entity.PlanExerciseEntity
import com.don.homefitness.data.db.entity.PlannedSetEntity
import com.don.homefitness.data.db.entity.SessionExerciseEntity
import com.don.homefitness.data.db.entity.SessionSetEntity
import com.don.homefitness.data.db.entity.WorkoutSessionEntity

class BackupRepository(private val database: FitnessDatabase) {
    suspend fun snapshot(): BackupDocument {
        val plans = database.planDao().allPlans()
        val exercises = database.planDao().allExercises()
        val sets = database.planDao().allSets()
        val setsByExercise = sets.groupBy { it.planExerciseId }
        return BackupDocument(
            exportedAt = System.currentTimeMillis(),
            favorites = database.exerciseDao().allFavoriteIds(),
            plans = plans.map { plan ->
                val planExercises = exercises.filter { it.planId == plan.id }
                BackupPlan(plan.id, plan.name, planExercises.map { it.exerciseId }, planExercises.flatMap { exercise -> setsByExercise[exercise.id].orEmpty().map { set -> BackupPlannedSet(exercise.id, set.position, set.mode, set.targetReps, set.targetSeconds, set.targetWeightGrams, set.restSeconds) } })
            },
            sessions = database.trainingDao().allSessions().filter { it.status != "ACTIVE" }.map { session -> BackupSession(session.id, session.planNameSnapshot, session.status, session.startedAt, session.finishedAt, session.localDate, session.zoneId) },
        )
    }

    suspend fun restore(validated: ValidatedBackup) = database.withTransaction {
        require(database.trainingDao().findActive() == null) { "有活动训练时不能恢复备份" }
        database.exerciseDao().deleteAllFavorites()
        database.exerciseDao().insertFavorites(validated.document.favorites.map(::FavoriteEntity))
        database.planDao().deleteAllSets()
        database.planDao().deleteAllExercises()
        database.planDao().deleteAllPlans()
        validated.document.plans.forEach { plan ->
            database.planDao().insertPlan(com.don.homefitness.data.db.entity.WorkoutPlanEntity(plan.id, plan.name, 0, 0))
            val planExercises = plan.exerciseIds.mapIndexed { position, exerciseId -> PlanExerciseEntity("${plan.id}-$position", plan.id, exerciseId, position) }
            database.planDao().insertExercises(planExercises)
            val exerciseIdsByPosition = planExercises.associateBy { it.position }
            database.planDao().insertSets(plan.sets.mapIndexed { index, set ->
                val exercise = exerciseIdsByPosition[index.coerceAtMost(planExercises.lastIndex)] ?: error("备份训练组动作关系无效")
                PlannedSetEntity("${plan.id}-set-$index", exercise.id, set.position, set.mode, set.targetReps, set.targetSeconds, set.targetWeightGrams, set.restSeconds)
            })
        }
        database.trainingDao().deleteAllSessions()
        database.trainingDao().insertSessions(validated.document.sessions.map { session -> WorkoutSessionEntity(session.id, null, session.planNameSnapshot, session.status, session.startedAt, session.finishedAt, session.localDate, session.zoneId) })
    }
}
