package com.don.homefitness.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeEligibilityTest {
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
