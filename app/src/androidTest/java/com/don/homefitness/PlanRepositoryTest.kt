package com.don.homefitness

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.don.homefitness.core.model.PlanDraft
import com.don.homefitness.core.model.PlanExerciseDraft
import com.don.homefitness.core.model.PlannedSetDraft
import com.don.homefitness.core.model.SetMode
import com.don.homefitness.data.db.FitnessDatabase
import com.don.homefitness.feature.plan.PlanRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlanRepositoryTest {
    private lateinit var database: FitnessDatabase
    private lateinit var repository: PlanRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FitnessDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = PlanRepository(database)
    }

    @After
    fun tearDown() { database.close() }

    @Test
    fun savesOrderAndKeepsRepeatedExerciseEntriesIndependent() = runBlocking {
        val id = repository.savePlan(
            PlanDraft(
                name = "家庭训练",
                exercises = listOf(
                    PlanExerciseDraft("0001", listOf(PlannedSetDraft(SetMode.REPS, targetReps = 8))),
                    PlanExerciseDraft("0001", listOf(PlannedSetDraft(SetMode.REPS, targetReps = 12))),
                ),
            ),
        )

        val plan = repository.observePlans().first().single()
        assertEquals(id, plan.id)
        assertEquals(listOf(0, 1), plan.exercises.map { it.position })
        assertEquals(listOf(8, 12), plan.exercises.map { it.sets.single().targetReps })
        assertNotEquals(plan.exercises[0].id, plan.exercises[1].id)
    }

    @Test
    fun duplicateCreatesIndependentPlanAndDeleteDoesNotAffectOtherPlan() = runBlocking {
        val originalId = repository.savePlan(
            PlanDraft("原计划", listOf(PlanExerciseDraft("0001"))),
        )
        val duplicateId = repository.duplicatePlan(originalId)

        assertNotEquals(originalId, duplicateId)
        assertEquals(2, repository.observePlans().first().size)

        repository.deletePlan(originalId)
        val remaining = repository.observePlans().first().single()
        assertEquals(duplicateId, remaining.id)
        assertEquals("原计划（副本）", remaining.name)
    }
}
