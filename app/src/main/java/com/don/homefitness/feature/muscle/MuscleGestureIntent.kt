package com.don.homefitness.feature.muscle

import kotlin.math.hypot

enum class GestureIntent { TAP, ROTATE, SCALE }

fun classifyGesture(
    downX: Float,
    downY: Float,
    upX: Float,
    upY: Float,
    scaleDelta: Float,
    tapSlopPx: Float,
): GestureIntent = when {
    kotlin.math.abs(scaleDelta) > 0.01f -> GestureIntent.SCALE
    hypot(upX - downX, upY - downY) > tapSlopPx -> GestureIntent.ROTATE
    else -> GestureIntent.TAP
}
