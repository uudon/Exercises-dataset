package com.don.homefitness.feature.muscle

import org.junit.Assert.assertEquals
import org.junit.Test

class MuscleGestureIntentTest {
    @Test
    fun movementBeyondTapSlopIsRotation() {
        assertEquals(
            GestureIntent.ROTATE,
            classifyGesture(10f, 10f, 20f, 10f, scaleDelta = 0f, tapSlopPx = 8f),
        )
    }

    @Test
    fun pinchIsScaleEvenWhenItsPointerMovementIsSmall() {
        assertEquals(
            GestureIntent.SCALE,
            classifyGesture(10f, 10f, 11f, 10f, scaleDelta = 0.2f, tapSlopPx = 8f),
        )
    }

    @Test
    fun movementWithinTapSlopIsTap() {
        assertEquals(
            GestureIntent.TAP,
            classifyGesture(10f, 10f, 13f, 14f, scaleDelta = 0f, tapSlopPx = 8f),
        )
    }
}
