package com.don.homefitness.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Test
import com.don.homefitness.core.model.TrainingLocation

class HomeEligibilityTest {
    @Test fun `gym mode allows reviewed equipment actions`() {
        val exercise = CatalogExercise(
            id = "0001",
            originalName = "Barbell Row",
            nameZh = "杠铃划船",
            aliasesZh = emptyList(),
            bodyPart = "back",
            equipment = "barbell",
            target = "back",
            secondaryMuscles = emptyList(),
            instructionsZh = "",
            instructionStepsZh = emptyList(),
            requiredEquipment = setOf("barbell"),
            homeEligible = false,
            reviewStatus = "manually-reviewed",
            sourceCommit = "test",
            isFavorite = false,
        )
        assertEquals(1, filterExercisesAtLocation(listOf(exercise), TrainingLocation.GYM, "", null, null, emptySet()).size)
        assertEquals(0, filterExercisesAtLocation(listOf(exercise), TrainingLocation.HOME, "", null, null, emptySet()).size)
    }
    @Test
    fun equipmentAndBodyPartFiltersUseIntersection() {
        val exercises = listOf(
            exercise("0001", "chest", setOf("body weight")),
            exercise("0002", "back", setOf("dumbbell")),
            exercise("0003", "chest", setOf("pull-up bar")),
        )

        val result = filterHomeExercises(
            exercises = exercises,
            query = "",
            bodyPart = "chest",
            equipment = "body weight",
            availableEquipment = setOf("body weight", "dumbbell"),
        )

        assertEquals(listOf("0001"), result.map(CatalogExercise::id))
    }

    private fun exercise(id: String, bodyPart: String, equipment: Set<String>) = CatalogExercise(
        id = id,
        originalName = id,
        nameZh = id,
        aliasesZh = emptyList(),
        bodyPart = bodyPart,
        equipment = equipment.single(),
        target = "target",
        secondaryMuscles = emptyList(),
        instructionsZh = "说明",
        instructionStepsZh = listOf("步骤"),
        requiredEquipment = equipment,
        homeEligible = true,
        reviewStatus = "manually-reviewed",
        sourceCommit = "test",
    )
}
