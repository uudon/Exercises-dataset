package com.don.homefitness.feature.plan

import com.don.homefitness.core.model.SetMode
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseReplacementTest {
    @Test fun `different modes require confirmation`() { assertTrue(replacementDecision(SetMode.REPS, SetMode.DURATION, false).requiresTargetConfirmation) }
    @Test fun `same mode keeps target`() { assertTrue(!replacementDecision(SetMode.REPS, SetMode.REPS, false).requiresTargetConfirmation) }
}
