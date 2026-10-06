package com.don.homefitness.feature.muscle

import com.don.homefitness.core.model.TrainingLocation
import com.don.homefitness.data.catalog.CatalogExercise
import com.don.homefitness.data.body.MuscleActionMapping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleActionMapperTest {
    @Test
    fun returnsHomeBodyweightAndDumbbellActionsWithPrimaryFirst() {
        val exercises = listOf(
            exercise("0001", "pectorals", equipment = "body weight"),
            exercise("0002", "triceps", equipment = "dumbbell"),
            exercise("0003", "chest", equipment = "barbell", homeEligible = false),
        )
        val mapper = MuscleActionMapper(
            exercises = exercises,
            mappings = listOf(MuscleActionMapping("chest", listOf("0001"), listOf("0002", "0003"))),
        )

        val result = mapper.actionsFor("chest", TrainingLocation.HOME, setOf("body weight", "dumbbell"))

        assertEquals(listOf("0001"), result.primary.map(CatalogExercise::id))
        assertEquals(listOf("0002"), result.secondary.map(CatalogExercise::id))
    }

    @Test
    fun doesNotFuzzyMatchNamesOrReturnGymOnlyActionsInHomeMode() {
        val mapper = MuscleActionMapper(
            exercises = listOf(
                exercise("0001", "pectorals", nameZh = "胸部训练", equipment = "body weight"),
                exercise("0002", "pectorals", nameZh = "胸肌", equipment = "barbell", homeEligible = false),
            ),
            mappings = listOf(MuscleActionMapping("chest", listOf("0002"), emptyList())),
        )

        val result = mapper.actionsFor("胸部", TrainingLocation.HOME, setOf("body weight", "dumbbell"))

        assertTrue(result.primary.isEmpty())
        assertTrue(result.secondary.isEmpty())
    }

    private fun exercise(
        id: String,
        target: String,
        nameZh: String = id,
        equipment: String,
        homeEligible: Boolean = true,
    ) = CatalogExercise(
        id = id,
        originalName = id,
        nameZh = nameZh,
        aliasesZh = emptyList(),
        bodyPart = "body",
        equipment = equipment,
        target = target,
        secondaryMuscles = emptyList(),
        instructionsZh = "说明",
        instructionStepsZh = listOf("步骤"),
        requiredEquipment = setOf(equipment),
        homeEligible = homeEligible,
        reviewStatus = "manually-reviewed",
        sourceCommit = "test",
    )
}
