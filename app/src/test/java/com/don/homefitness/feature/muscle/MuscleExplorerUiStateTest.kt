package com.don.homefitness.feature.muscle

import com.don.homefitness.data.body.BodyGender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleExplorerUiStateTest {
    @Test
    fun defaultsToMaleFrontAtDefaultZoom() {
        val state = MuscleExplorerUiState()

        assertEquals(BodyGender.MALE, state.gender)
        assertEquals(BodyView.FRONT, state.view)
        assertEquals(MuscleExplorerUiState.DEFAULT_ZOOM, state.zoom)
    }

    @Test
    fun genderCanBeSwitchedWithoutChangingTheCurrentView() {
        val sideView = MuscleExplorerUiState().selectView(BodyView.SIDE)

        val femaleView = sideView.selectGender(BodyGender.FEMALE)

        assertEquals(BodyGender.FEMALE, femaleView.gender)
        assertEquals(BodyView.SIDE, femaleView.view)
        assertEquals(sideView.zoom, femaleView.zoom)
    }

    @Test
    fun supportsEachCanonicalBodyView() {
        val state = MuscleExplorerUiState()

        assertEquals(BodyView.FRONT, state.selectView(BodyView.FRONT).view)
        assertEquals(BodyView.SIDE, state.selectView(BodyView.SIDE).view)
        assertEquals(BodyView.BACK, state.selectView(BodyView.BACK).view)
    }

    @Test
    fun zoomIsClampedToSafeViewerBounds() {
        val state = MuscleExplorerUiState()

        val zoomedIn = state.zoomBy(100f)
        val zoomedOut = zoomedIn.zoomBy(0.001f)

        assertEquals(MuscleExplorerUiState.MAX_ZOOM, zoomedIn.zoom)
        assertEquals(MuscleExplorerUiState.MIN_ZOOM, zoomedOut.zoom)
        assertTrue(MuscleExplorerUiState.MIN_ZOOM < MuscleExplorerUiState.DEFAULT_ZOOM)
        assertTrue(MuscleExplorerUiState.DEFAULT_ZOOM < MuscleExplorerUiState.MAX_ZOOM)
    }

    @Test
    fun resetRestoresDefaultCameraWithoutDiscardingSelectedGender() {
        val changed = MuscleExplorerUiState()
            .selectGender(BodyGender.FEMALE)
            .selectView(BodyView.BACK)
            .zoomBy(2f)

        val reset = changed.resetView()

        assertEquals(BodyGender.FEMALE, reset.gender)
        assertEquals(BodyView.FRONT, reset.view)
        assertEquals(MuscleExplorerUiState.DEFAULT_ZOOM, reset.zoom)
    }
}
