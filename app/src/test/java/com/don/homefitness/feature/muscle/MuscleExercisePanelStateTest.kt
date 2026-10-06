package com.don.homefitness.feature.muscle

import com.don.homefitness.data.catalog.CatalogExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleExercisePanelStateTest {
    @Test
    fun keepsPrimaryActionsBeforeSecondaryActions() {
        val state = MusclePanelState(
            muscleGroupId = "chest",
            displayNameZh = "胸部",
            primary = listOf(exercise("primary")),
            secondary = listOf(exercise("secondary")),
        )

        assertEquals(listOf("primary"), state.primary.map(CatalogExercise::id))
        assertEquals(listOf("secondary"), state.secondary.map(CatalogExercise::id))
        assertTrue(state.hasActions)
    }

    @Test
    fun blockedStateExplainsWhyActionsAreUnavailable() {
        val state = MusclePanelState.blocked("chest", "胸部")

        assertFalse(state.hasActions)
        assertEquals("资源授权待确认", state.emptyReason)
    }

    @Test
    fun emptyStateCanExplainEquipmentFilter() {
        val state = MusclePanelState(
            muscleGroupId = "chest",
            displayNameZh = "胸部",
            emptyReason = "家庭模式仅支持徒手和哑铃动作",
        )

        assertFalse(state.hasActions)
        assertEquals("家庭模式仅支持徒手和哑铃动作", state.emptyReason)
    }

    private fun exercise(id: String) = CatalogExercise(
        id = id,
        originalName = id,
        nameZh = id,
        aliasesZh = emptyList(),
        bodyPart = "chest",
        equipment = "body weight",
        target = "pectorals",
        secondaryMuscles = emptyList(),
        instructionsZh = "说明",
        instructionStepsZh = listOf("步骤"),
        requiredEquipment = setOf("body weight"),
        homeEligible = true,
        reviewStatus = "manually-reviewed",
        sourceCommit = "test",
    )
}
