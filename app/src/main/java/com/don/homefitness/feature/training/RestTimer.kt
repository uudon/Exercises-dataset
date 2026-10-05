package com.don.homefitness.feature.training

data class RestState(val bootMarker: String, val deadlineElapsedMs: Long, val durationSeconds: Int)

object RestTimer {
    fun remainingSeconds(state: RestState, nowElapsedMs: Long, bootMarker: String): Int {
        if (state.bootMarker != bootMarker) return 0
        return ((state.deadlineElapsedMs - nowElapsedMs + 999) / 1000).coerceIn(0, state.durationSeconds.toLong()).toInt()
    }
}
