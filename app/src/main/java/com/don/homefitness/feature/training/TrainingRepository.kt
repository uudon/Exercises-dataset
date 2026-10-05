package com.don.homefitness.feature.training

import androidx.room.withTransaction
import com.don.homefitness.core.model.LoadType
import com.don.homefitness.core.model.PlannedSet
import com.don.homefitness.core.model.SessionDetail
import com.don.homefitness.core.model.SessionExercise
import com.don.homefitness.core.model.SessionSet
import com.don.homefitness.core.model.SessionStatus
import com.don.homefitness.core.model.SetMode
import com.don.homefitness.core.model.SetResult
import com.don.homefitness.core.model.SetStatus
import com.don.homefitness.core.model.SessionSummary
import com.don.homefitness.core.model.TrendPoint
import com.don.homefitness.data.db.FitnessDatabase
import com.don.homefitness.data.db.entity.RestStateEntity
import com.don.homefitness.data.db.entity.SessionExerciseEntity
import com.don.homefitness.data.db.entity.SessionSetEntity
import com.don.homefitness.data.db.entity.WorkoutSessionEntity
import com.don.homefitness.feature.plan.PlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class TrainingRepository(
    private val database: FitnessDatabase,
    private val planRepository: PlanRepository,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    private val dao = database.trainingDao()

    suspend fun startSession(planId: String): String = database.withTransaction {
        check(dao.findActive() == null) { "已有进行中的训练，请先继续或取消" }
        val plan = planRepository.getPlan(planId) ?: error("计划不存在")
        require(plan.exercises.isNotEmpty() && plan.exercises.all { it.sets.isNotEmpty() }) { "计划至少需要一个动作和一组训练" }
        val startedAt = now()
        val sessionId = UUID.randomUUID().toString()
        dao.insertSession(WorkoutSessionEntity(sessionId, plan.id, plan.name, SessionStatus.ACTIVE.name, startedAt, null, localDate(startedAt), zone().id))
        val exercises = plan.exercises.map { exercise -> SessionExerciseEntity(UUID.randomUUID().toString(), sessionId, exercise.exerciseId, exercise.exerciseId, "", exercise.position) }
        dao.insertExercises(exercises)
        dao.insertSets(plan.exercises.zip(exercises).flatMap { (source, target) -> source.sets.map { it.toEntity(target.id) } })
        sessionId
    }

    fun observeActiveSession(): Flow<SessionDetail?> = dao.observeActive().flatMapLatest { session ->
        if (session == null) flowOf(null) else flow { emit(loadDetail(session)) }
    }

    suspend fun saveSet(sessionSetId: String, result: SetResult) {
        val current = dao.findSet(sessionSetId) ?: error("训练组不存在")
        val sessionExercise = dao.findExercise(current.sessionExerciseId) ?: error("训练动作不存在")
        require(current.status == SetStatus.PENDING.name) { "该训练组已经处理" }
        require(result.mode == SetMode.valueOf(current.mode)) { "记录模式不匹配" }
        database.withTransaction {
            dao.updateSet(current.copy(actualReps = result.reps, actualSeconds = result.seconds, actualWeightGrams = result.weightGrams, status = SetStatus.COMPLETED.name, loadType = result.loadType.name, modifiedAt = now()))
            dao.clearRestState(sessionExercise.sessionId)
        }
    }

    suspend fun skipSet(sessionSetId: String) {
        val current = dao.findSet(sessionSetId) ?: error("训练组不存在")
        dao.updateSet(current.copy(status = SetStatus.SKIPPED.name, modifiedAt = now()))
    }

    suspend fun finishSession(sessionId: String) = database.withTransaction {
        require(dao.completedSetCount(sessionId) > 0) { "至少完成一组后才能结束训练" }
        dao.updateStatus(sessionId, SessionStatus.COMPLETED.name, now())
        dao.clearRestState(sessionId)
    }

    suspend fun cancelSession(sessionId: String) {
        dao.updateStatus(sessionId, SessionStatus.CANCELLED.name, now())
        dao.clearRestState(sessionId)
    }

    suspend fun saveRestState(sessionId: String, state: RestState) = dao.saveRestState(RestStateEntity(sessionId, state.bootMarker, state.deadlineElapsedMs, state.durationSeconds))
    suspend fun restState(sessionId: String): RestState? = dao.findRestState(sessionId)?.let { RestState(it.bootMarker, it.deadlineElapsedMs, it.durationSeconds) }
    suspend fun detail(sessionId: String): SessionDetail? = dao.findSession(sessionId)?.let { loadDetail(it) }

    suspend fun updateCompletedSet(setId: String, result: SetResult) {
        val current = dao.findSet(setId) ?: error("训练组不存在")
        require(current.status == SetStatus.COMPLETED.name) { "只能编辑已完成训练组" }
        dao.updateSet(current.copy(actualReps = result.reps, actualSeconds = result.seconds, actualWeightGrams = result.weightGrams, loadType = result.loadType.name, modifiedAt = now()))
    }

    suspend fun trend(exerciseId: String, loadType: LoadType, mode: SetMode): List<TrendPoint> = dao.trend(exerciseId, mode.name, loadType.name).map { row ->
        TrendPoint(row.localDate, if (mode == SetMode.REPS) (row.actualReps ?: 0).toLong() else (row.actualSeconds ?: 0).toLong(), mode, loadType)
    }

    fun observeHistory(): Flow<List<SessionSummary>> = dao.observeHistory().map { sessions ->
        sessions.map { session -> SessionSummary(session.id, session.planNameSnapshot, SessionStatus.valueOf(session.status), session.startedAt, session.localDate, session.completedSets) }
    }

    suspend fun previousPerformance(exerciseId: String, beforeStartedAt: Long): Pair<String, List<SessionSet>>? {
        val previous = dao.findPreviousSession(exerciseId, beforeStartedAt) ?: return null
        val detail = dao.findExercises(previous.sessionId).firstOrNull { it.exerciseId == exerciseId } ?: return null
        return previous.sessionId to dao.findSets(listOf(detail.id)).filter { it.status == SetStatus.COMPLETED.name }.map(::toModel)
    }

    private suspend fun loadDetail(session: WorkoutSessionEntity): SessionDetail {
        val exercises = dao.findExercises(session.id)
        val sets = if (exercises.isEmpty()) emptyMap() else dao.findSets(exercises.map { it.id }).groupBy { it.sessionExerciseId }
        return SessionDetail(session.id, session.planId, session.planNameSnapshot, SessionStatus.valueOf(session.status), session.startedAt, session.finishedAt, session.localDate, session.zoneId, exercises.map { exercise -> SessionExercise(exercise.id, exercise.sessionId, exercise.exerciseId, exercise.nameSnapshot, exercise.equipmentSnapshot, exercise.position, sets[exercise.id].orEmpty().map(::toModel)) })
    }

    private fun localDate(time: Long): String = Instant.ofEpochMilli(time).atZone(zone()).toLocalDate().toString()
    private fun PlannedSet.toEntity(exerciseId: String): SessionSetEntity = SessionSetEntity(UUID.randomUUID().toString(), exerciseId, position, mode.name, targetReps, targetSeconds, targetWeightGrams, null, null, null, SetStatus.PENDING.name, if (targetWeightGrams == null) LoadType.BODYWEIGHT.name else LoadType.EXTERNAL.name, null)
    private fun toModel(entity: SessionSetEntity) = SessionSet(entity.id, entity.sessionExerciseId, entity.position, SetMode.valueOf(entity.mode), entity.targetReps, entity.targetSeconds, entity.targetWeightGrams, entity.actualReps, entity.actualSeconds, entity.actualWeightGrams, SetStatus.valueOf(entity.status), LoadType.valueOf(entity.loadType), entity.modifiedAt)
}
