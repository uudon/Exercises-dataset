package com.don.homefitness.core.validation

import com.don.homefitness.core.model.PlannedSetDraft
import com.don.homefitness.core.model.PlanDraft
import com.don.homefitness.core.model.PlanExerciseDraft
import com.don.homefitness.core.model.SetMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetValidatorTest {
    @Test
    fun repsRequiresRepsOnlyAndAcceptsTheDocumentedRange() {
        assertTrue(SetValidator.validate(PlannedSetDraft(SetMode.REPS, targetReps = 1)).isValid)
        assertTrue(SetValidator.validate(PlannedSetDraft(SetMode.REPS, targetReps = 999)).isValid)
        assertFalse(SetValidator.validate(PlannedSetDraft(SetMode.REPS)).isValid)
        assertFalse(
            SetValidator.validate(
                PlannedSetDraft(SetMode.REPS, targetReps = 10, targetSeconds = 30),
            ).isValid,
        )
        assertFalse(SetValidator.validate(PlannedSetDraft(SetMode.REPS, targetReps = 1000)).isValid)
    }

    @Test
    fun durationRequiresSecondsOnlyAndAcceptsTheDocumentedRange() {
        assertTrue(SetValidator.validate(PlannedSetDraft(SetMode.DURATION, targetSeconds = 1)).isValid)
        assertTrue(SetValidator.validate(PlannedSetDraft(SetMode.DURATION, targetSeconds = 7200)).isValid)
        assertFalse(SetValidator.validate(PlannedSetDraft(SetMode.DURATION)).isValid)
        assertFalse(SetValidator.validate(PlannedSetDraft(SetMode.DURATION, targetSeconds = 7201)).isValid)
        assertFalse(
            SetValidator.validate(
                PlannedSetDraft(SetMode.DURATION, targetSeconds = 30, targetReps = 10),
            ).isValid,
        )
    }

    @Test
    fun weightIsSingleDumbbellKilogramsWithAtMostTwoDecimalPlaces() {
        assertTrue(SetValidator.validate(PlannedSetDraft(SetMode.REPS, targetReps = 10, targetWeightKg = "20.00")).isValid)
        assertFalse(SetValidator.validate(PlannedSetDraft(SetMode.REPS, targetReps = 10, targetWeightKg = "200.01")).isValid)
        assertFalse(SetValidator.validate(PlannedSetDraft(SetMode.REPS, targetReps = 10, targetWeightKg = "1.001")).isValid)
        assertFalse(SetValidator.validate(PlannedSetDraft(SetMode.REPS, targetReps = 10, targetWeightKg = "-1")).isValid)
    }

    @Test
    fun planRequiresTrimmedNameActionAndAtLeastOneSet() {
        assertTrue(SetValidator.validatePlan(PlanDraft("  A  ", listOf(PlanExerciseDraft("0001")))).isValid)
        assertFalse(SetValidator.validatePlan(PlanDraft("", listOf(PlanExerciseDraft("0001")))).isValid)
        assertFalse(SetValidator.validatePlan(PlanDraft("a".repeat(41), listOf(PlanExerciseDraft("0001")))).isValid)
        assertFalse(SetValidator.validatePlan(PlanDraft("A", emptyList())).isValid)
        assertFalse(SetValidator.validatePlan(PlanDraft("A", listOf(PlanExerciseDraft("0001", emptyList())))).isValid)
        assertFalse(SetValidator.validate(PlannedSetDraft(SetMode.REPS, targetReps = 10, restSeconds = -1)).isValid)
        assertTrue(SetValidator.validate(PlannedSetDraft(SetMode.REPS, targetReps = 10, restSeconds = 600)).isValid)
        assertFalse(SetValidator.validate(PlannedSetDraft(SetMode.REPS, targetReps = 10, restSeconds = 601)).isValid)
    }
}
