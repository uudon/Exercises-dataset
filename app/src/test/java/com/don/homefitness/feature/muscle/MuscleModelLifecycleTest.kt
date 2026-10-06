package com.don.homefitness.feature.muscle

import com.don.homefitness.data.body.BodyGender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleModelLifecycleTest {
    @Test
    fun pauseAndStopForwardToRendererWithoutChangingLogicalState() {
        val renderer = RecordingRenderer()
        val viewModel = MuscleExplorerViewModel(renderer)
        viewModel.selectMuscle("abs")
        viewModel.onResume()

        viewModel.onPause()
        viewModel.onStop()

        assertEquals(listOf("load:MALE", "resume", "pause", "dispose"), renderer.events)
        assertEquals("abs", viewModel.uiState.value.selectedMuscleGroupId)
    }

    @Test
    fun regionHitIsTheOnlyRendererCallbackThatSelectsARegion() {
        val renderer = RecordingRenderer()
        val viewModel = MuscleExplorerViewModel(renderer)
        viewModel.onResume()

        renderer.onRegionHit?.invoke("calves")

        assertEquals("calves", viewModel.uiState.value.selectedMuscleGroupId)
    }

    @Test
    fun rendererRejectsRemoteModelPathsInsteadOfFallingBackToNetwork() {
        val renderer = LocalGlbMuscleModelRenderer(
            assetPathByGender = mapOf(BodyGender.MALE to "https://example.invalid/body.glb"),
        )

        val error = runCatching { renderer.load(BodyGender.MALE) }.exceptionOrNull()

        assertTrue(error is MuscleModelLoadException)
        assertTrue(error?.message?.contains("local GLB") == true)
    }

    private class RecordingRenderer : MuscleModelRenderer {
        val events = mutableListOf<String>()
        override var onRegionHit: ((String) -> Unit)? = null
        override var onCameraChanged: ((CameraOrbit) -> Unit)? = null
        override fun load(gender: BodyGender) {
            events += "load:$gender"
        }
        override fun setHighlight(muscleGroupId: String?) = Unit
        override fun resetCamera() = Unit
        override fun onResume() { events += "resume" }
        override fun onPause() { events += "pause" }
        override fun dispose() { events += "dispose" }
    }
}
