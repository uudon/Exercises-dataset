package com.don.homefitness.feature.plan

import com.don.homefitness.core.model.PlanDraft
import com.don.homefitness.core.model.PlanExerciseDraft
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanEditorStateTest {
    private val initial = PlanDraft("家庭训练", listOf(PlanExerciseDraft("0001")))

    @Test
    fun unchangedDraftDoesNotRequireExitConfirmation() {
        assertFalse(hasUnsavedChanges(initial, initial.copy()))
    }

    @Test
    fun changedDraftRequiresExitConfirmation() {
        assertTrue(hasUnsavedChanges(initial, initial.copy(name = "新的训练")))
    }
}
