package com.don.homefitness.feature.muscle

import com.don.homefitness.core.model.TrainingLocation
import com.don.homefitness.data.body.BodyGender
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class MuscleExplorerViewModelTest {
    @Test
    fun genderChangeRetainsSelectionCameraSceneAndPanel() = runBlocking {
        val viewModel = MuscleExplorerViewModel(FakeRenderer())
        viewModel.selectScene(TrainingLocation.GYM)
        viewModel.selectMuscle("chest")
        viewModel.setPanelExpanded(true)
        viewModel.updateCamera(CameraOrbit(25f, 10f, 2f))

        viewModel.selectGender(BodyGender.FEMALE)

        val state = viewModel.uiState.first()
        assertEquals(BodyGender.FEMALE, state.gender)
        assertEquals("chest", state.selectedMuscleGroupId)
        assertEquals(CameraOrbit(25f, 10f, 2f), state.cameraOrbit)
        assertEquals(TrainingLocation.GYM, state.scene)
        assertEquals(true, state.panelExpanded)
    }

    @Test
    fun resetCameraRetainsSelectionAndGender() = runBlocking {
        val viewModel = MuscleExplorerViewModel(FakeRenderer())
        viewModel.selectGender(BodyGender.FEMALE)
        viewModel.selectMuscle("back")
        viewModel.updateCamera(CameraOrbit(25f, 10f, 2f))

        viewModel.resetCamera()

        val state = viewModel.uiState.first()
        assertEquals(BodyGender.FEMALE, state.gender)
        assertEquals("back", state.selectedMuscleGroupId)
        assertEquals(CameraOrbit.DEFAULT, state.cameraOrbit)
    }

    @Test
    fun modelFailureIsExplicitAndDoesNotPretendToBeReady() = runBlocking {
        val viewModel = MuscleExplorerViewModel(FakeRenderer(loadError = "local GLB missing"))

        viewModel.onResume()

        val state = viewModel.uiState.first()
        assertFalse(state.isModelReady)
        assertEquals("local GLB missing", state.errorMessage)
        assertNull(state.selectedMuscleGroupId)
    }

    private class FakeRenderer(private val loadError: String? = null) : MuscleModelRenderer {
        override var onRegionHit: ((String) -> Unit)? = null
        override var onNodeHit: ((String) -> Unit)? = null
        override var onCameraChanged: ((CameraOrbit) -> Unit)? = null
        override fun load(gender: BodyGender) {
            loadError?.let { throw MuscleModelLoadException(it) }
        }
        override fun setHighlight(muscleGroupId: String?) = Unit
        override fun resetCamera() = Unit
        override fun onResume() = Unit
        override fun onPause() = Unit
        override fun dispose() = Unit
    }
}
