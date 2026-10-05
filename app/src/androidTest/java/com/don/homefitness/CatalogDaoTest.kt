package com.don.homefitness

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.don.homefitness.data.db.FitnessDatabase
import com.don.homefitness.data.db.entity.ExerciseEntity
import com.don.homefitness.data.db.entity.FavoriteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogDaoTest {
    private lateinit var database: FitnessDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FitnessDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() { database.close() }

    @Test
    fun replacingCatalogDoesNotDeleteFavoriteRows() = runBlocking {
        val dao = database.exerciseDao()
        dao.replaceExercises(listOf(exercise("0001", "原动作")))
        dao.addFavorite(FavoriteEntity("0001"))
        dao.deleteAllExercises()
        dao.replaceExercises(listOf(exercise("0001", "更新动作")))

        assertEquals(listOf("0001"), dao.observeFavoriteIds().first())
        assertEquals("更新动作", dao.observeExercises().first().single().nameZh)
    }

    private fun exercise(id: String, name: String) = ExerciseEntity(
        id = id, originalName = name, nameZh = name, aliasesZh = "",
        bodyPart = "chest", equipment = "body weight", target = "chest",
        secondaryMuscles = "", instructionsZh = "说明", instructionStepsZh = "步骤",
        requiredEquipment = "body weight", homeEligible = true,
        reviewStatus = "manually-reviewed", sourceCommit = "test",
    )
}
