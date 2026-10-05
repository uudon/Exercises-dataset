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
