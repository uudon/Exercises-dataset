package com.don.homefitness.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.don.homefitness.data.db.entity.RestStateEntity
import com.don.homefitness.data.db.entity.SessionExerciseEntity
import com.don.homefitness.data.db.entity.SessionSetEntity
import com.don.homefitness.data.db.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingDao {
    @Query("SELECT * FROM workout_sessions WHERE status = 'ACTIVE' LIMIT 1")
    fun observeActive(): Flow<WorkoutSessionEntity?>
    @Query("SELECT * FROM workout_sessions WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun findActive(): WorkoutSessionEntity?
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun findSession(id: String): WorkoutSessionEntity?
    @Query("SELECT * FROM workout_sessions")
    suspend fun allSessions(): List<WorkoutSessionEntity>
    @Query("SELECT * FROM session_exercises")
    suspend fun allSessionExercises(): List<SessionExerciseEntity>
    @Query("SELECT * FROM session_sets")
    suspend fun allSessionSets(): List<SessionSetEntity>
    @Query("SELECT s.id, s.planNameSnapshot, s.status, s.startedAt, s.localDate, COUNT(CASE WHEN x.status = 'COMPLETED' THEN 1 END) AS completedSets FROM workout_sessions s LEFT JOIN session_exercises e ON e.sessionId=s.id LEFT JOIN session_sets x ON x.sessionExerciseId=e.id GROUP BY s.id ORDER BY s.startedAt DESC")
    fun observeHistory(): Flow<List<SessionSummaryRow>>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: WorkoutSessionEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSessions(sessions: List<WorkoutSessionEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExercises(exercises: List<SessionExerciseEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSets(sets: List<SessionSetEntity>)
    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY position")
    suspend fun findExercises(sessionId: String): List<SessionExerciseEntity>
    @Query("SELECT * FROM session_exercises WHERE id = :id")
    suspend fun findExercise(id: String): SessionExerciseEntity?
    @Query("SELECT * FROM session_sets WHERE sessionExerciseId IN (:ids) ORDER BY position")
    suspend fun findSets(ids: List<String>): List<SessionSetEntity>
    @Query("SELECT * FROM session_sets WHERE id = :id")
    suspend fun findSet(id: String): SessionSetEntity?
    @Query("SELECT s.id AS sessionId, s.localDate AS localDate FROM workout_sessions s JOIN session_exercises e ON e.sessionId=s.id JOIN session_sets x ON x.sessionExerciseId=e.id WHERE e.exerciseId=:exerciseId AND s.status='COMPLETED' AND s.startedAt < :beforeStartedAt ORDER BY s.startedAt DESC LIMIT 1")
    suspend fun findPreviousSession(exerciseId: String, beforeStartedAt: Long): PreviousSessionRow?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSet(set: SessionSetEntity)
    @Query("UPDATE workout_sessions SET status = :status, finishedAt = :finishedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, finishedAt: Long?)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRestState(state: RestStateEntity)
    @Query("DELETE FROM rest_states WHERE sessionId = :sessionId")
    suspend fun clearRestState(sessionId: String)
    @Query("SELECT * FROM rest_states WHERE sessionId = :sessionId")
    suspend fun findRestState(sessionId: String): RestStateEntity?
    @Query("SELECT COUNT(*) FROM session_sets s JOIN session_exercises e ON s.sessionExerciseId=e.id WHERE e.sessionId=:sessionId AND s.status='COMPLETED'")
    suspend fun completedSetCount(sessionId: String): Int
    @Query("SELECT * FROM session_sets WHERE status='COMPLETED' AND mode=:mode AND loadType=:loadType AND sessionExerciseId IN (SELECT id FROM session_exercises WHERE exerciseId=:exerciseId) AND sessionExerciseId IN (SELECT id FROM session_exercises WHERE sessionId IN (SELECT id FROM workout_sessions WHERE status='COMPLETED' AND startedAt < :beforeStartedAt)) ORDER BY sessionExerciseId, position")
    suspend fun previousSets(exerciseId: String, mode: String, loadType: String, beforeStartedAt: Long): List<SessionSetEntity>
    @Query("SELECT e.actualReps AS actualReps, e.actualSeconds AS actualSeconds, e.actualWeightGrams AS actualWeightGrams, s.localDate AS localDate FROM session_sets e JOIN session_exercises x ON e.sessionExerciseId=x.id JOIN workout_sessions s ON x.sessionId=s.id WHERE x.exerciseId=:exerciseId AND e.status='COMPLETED' AND e.mode=:mode AND e.loadType=:loadType AND s.status='COMPLETED' ORDER BY s.startedAt")
    suspend fun trend(exerciseId: String, mode: String, loadType: String): List<TrendRow>
    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAllSessions()
    @Query("DELETE FROM rest_states")
    suspend fun deleteAllRestStates()
}

data class TrendRow(val actualReps: Int?, val actualSeconds: Int?, val actualWeightGrams: Long?, val localDate: String)
data class SessionSummaryRow(val id: String, val planNameSnapshot: String, val status: String, val startedAt: Long, val localDate: String, val completedSets: Int)
data class PreviousSessionRow(val sessionId: String, val localDate: String)
