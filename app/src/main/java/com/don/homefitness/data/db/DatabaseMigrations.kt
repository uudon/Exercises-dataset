package com.don.homefitness.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS workout_plans (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )""".trimIndent(),
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS plan_exercises (
                id TEXT NOT NULL PRIMARY KEY,
                planId TEXT NOT NULL,
                exerciseId TEXT NOT NULL,
                position INTEGER NOT NULL,
                FOREIGN KEY(planId) REFERENCES workout_plans(id) ON DELETE CASCADE
            )""".trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_plan_exercises_planId ON plan_exercises(planId)")
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS planned_sets (
                id TEXT NOT NULL PRIMARY KEY,
                planExerciseId TEXT NOT NULL,
                position INTEGER NOT NULL,
                mode TEXT NOT NULL,
                targetReps INTEGER,
                targetSeconds INTEGER,
                targetWeightGrams INTEGER,
                restSeconds INTEGER NOT NULL,
                FOREIGN KEY(planExerciseId) REFERENCES plan_exercises(id) ON DELETE CASCADE
            )""".trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_planned_sets_planExerciseId ON planned_sets(planExerciseId)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS workout_sessions (id TEXT NOT NULL PRIMARY KEY, planId TEXT, planNameSnapshot TEXT NOT NULL, status TEXT NOT NULL, startedAt INTEGER NOT NULL, finishedAt INTEGER, localDate TEXT NOT NULL, zoneId TEXT NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_status ON workout_sessions(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_localDate ON workout_sessions(localDate)")
        db.execSQL("CREATE TABLE IF NOT EXISTS session_exercises (id TEXT NOT NULL PRIMARY KEY, sessionId TEXT NOT NULL, exerciseId TEXT NOT NULL, nameSnapshot TEXT NOT NULL, equipmentSnapshot TEXT NOT NULL, position INTEGER NOT NULL, FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_session_exercises_sessionId ON session_exercises(sessionId)")
        db.execSQL("CREATE TABLE IF NOT EXISTS session_sets (id TEXT NOT NULL PRIMARY KEY, sessionExerciseId TEXT NOT NULL, position INTEGER NOT NULL, mode TEXT NOT NULL, targetReps INTEGER, targetSeconds INTEGER, targetWeightGrams INTEGER, actualReps INTEGER, actualSeconds INTEGER, actualWeightGrams INTEGER, status TEXT NOT NULL, modifiedAt INTEGER, FOREIGN KEY(sessionExerciseId) REFERENCES session_exercises(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_session_sets_sessionExerciseId ON session_sets(sessionExerciseId)")
        db.execSQL("CREATE TABLE IF NOT EXISTS rest_states (sessionId TEXT NOT NULL PRIMARY KEY, bootMarker TEXT NOT NULL, deadlineElapsedMs INTEGER NOT NULL, durationSeconds INTEGER NOT NULL)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE session_sets ADD COLUMN loadType TEXT NOT NULL DEFAULT 'BODYWEIGHT'")
    }
}
