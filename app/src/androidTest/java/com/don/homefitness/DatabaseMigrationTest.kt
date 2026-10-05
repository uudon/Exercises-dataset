package com.don.homefitness

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.don.homefitness.data.db.FitnessDatabase
import com.don.homefitness.data.db.MIGRATION_1_2
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var databaseName: String

    @Before
    fun setUp() {
        databaseName = "migration-${UUID.randomUUID()}.db"
        val file = context.getDatabasePath(databaseName)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                """CREATE TABLE exercises (
                    id TEXT NOT NULL PRIMARY KEY,
                    originalName TEXT NOT NULL,
                    nameZh TEXT NOT NULL,
                    aliasesZh TEXT NOT NULL,
                    bodyPart TEXT NOT NULL,
                    equipment TEXT NOT NULL,
                    target TEXT NOT NULL,
                    secondaryMuscles TEXT NOT NULL,
                    instructionsZh TEXT NOT NULL,
                    instructionStepsZh TEXT NOT NULL,
                    requiredEquipment TEXT NOT NULL,
                    homeEligible INTEGER NOT NULL,
                    reviewStatus TEXT NOT NULL,
                    sourceCommit TEXT NOT NULL
                )""".trimIndent(),
            )
            db.execSQL("CREATE TABLE exercise_favorites (exerciseId TEXT NOT NULL PRIMARY KEY)")
            db.execSQL("INSERT INTO exercises VALUES ('legacy', 'Legacy', '旧动作', '', 'chest', 'body weight', 'chest', '', '', '', '', 1, 'approved', 'legacy')")
            db.execSQL("INSERT INTO exercise_favorites VALUES ('legacy')")
            db.version = 1
        }
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationToVersion2PreservesCatalogAndFavorite() {
        runBlocking {
        val database = Room.databaseBuilder(context, FitnessDatabase::class.java, databaseName)
            .addMigrations(MIGRATION_1_2)
            .build()

        try {
            assertEquals(listOf("legacy"), database.exerciseDao().observeFavoriteIds().first())
            assertEquals("旧动作", database.exerciseDao().observeExercises().first().single().nameZh)
            database.planDao()
        } finally {
            database.close()
        }
        }
    }
}
