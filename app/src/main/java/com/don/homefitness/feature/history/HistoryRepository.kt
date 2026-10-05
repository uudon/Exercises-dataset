package com.don.homefitness.feature.history

import com.don.homefitness.core.model.LoadType
import com.don.homefitness.core.model.PlanDraft
import com.don.homefitness.core.model.PlanExerciseDraft
import com.don.homefitness.core.model.PlannedSetDraft
import com.don.homefitness.core.model.SessionDetail
import com.don.homefitness.core.model.SessionStatus
import com.don.homefitness.core.model.SetMode
import com.don.homefitness.core.model.SetResult
import com.don.homefitness.core.model.SetStatus
import com.don.homefitness.feature.plan.PlanRepository
import com.don.homefitness.feature.training.TrainingRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class HistoryRepository(
    private val trainingRepository: TrainingRepository,
    private val planRepository: PlanRepository,
) {
    fun observeHistory() = trainingRepository.observeHistory()

    suspend fun session(sessionId: String): SessionDetail? = trainingRepository.detail(sessionId)

    suspend fun updateCompletedSet(setId: String, result: SetResult) = trainingRepository.updateCompletedSet(setId, result)

    suspend fun copySessionToPlan(sessionId: String): String {
        val detail = trainingRepository.detail(sessionId) ?: error("训练记录不存在")
        require(detail.status == SessionStatus.COMPLETED) { "只能复制已完成训练" }
        val draft = PlanDraft(
            name = "${detail.planNameSnapshot}（历史副本）".take(40),
            exercises = detail.exercises.map { exercise ->
                PlanExerciseDraft(
                    exerciseId = exercise.exerciseId,
                    sets = exercise.sets.filter { it.status == SetStatus.COMPLETED }.map { set ->
                        PlannedSetDraft(
                            mode = set.mode,
                            targetReps = set.actualReps,
                            targetSeconds = set.actualSeconds,
                            targetWeightKg = set.actualWeightGrams?.let { grams -> "%.2f".format(java.util.Locale.US, grams / 1000.0) },
                            restSeconds = 60,
                        )
                    }.ifEmpty { listOf(PlannedSetDraft(SetMode.REPS, targetReps = 10)) },
                )
            },
        )
        return planRepository.savePlan(draft)
    }
}
