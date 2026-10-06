package com.don.homefitness.feature.muscle

import com.don.homefitness.data.body.BodyGender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun rendererRejectsTraversalBlankSegmentsBackslashesAndSchemes() {
        listOf(
            "body/../body.glb",
            "body//body.glb",
            "body\\body.glb",
            "content://body.glb",
            " body/body.glb",
            "body/./body.glb",
        ).forEach { path -> assertFalse(path, isValidLocalGlbAssetPath(path)) }
    }

    @Test
    fun rendererMapsRegionHitToValidatedMuscleGroupAndExposesNodeHit() {
        val backend = RecordingBackend()
        val renderer = LocalGlbMuscleModelRenderer(
            assetPathByGender = mapOf(BodyGender.MALE to "body/male/body.glb"),
            backend = backend,
            regionIdToMuscleGroupId = mapOf("torso.region" to "chest"),
        )
        var nodeId: String? = null
        var muscleGroupId: String? = null
        renderer.onNodeHit = { nodeId = it }
        renderer.onRegionHit = { muscleGroupId = it }

        backend.emitHit("torso_node", "torso.region")

        assertEquals("torso_node", nodeId)
        assertEquals("chest", muscleGroupId)
    }

    @Test
    fun cameraIsRestoredAfterDisposeAndResume() {
        val renderer = RecordingRenderer()
        val viewModel = MuscleExplorerViewModel(renderer)
        val camera = CameraOrbit(42f, 18f, 1.2f)
        viewModel.updateCamera(camera)
        viewModel.onStop()
        viewModel.onResume()

        assertEquals(camera, renderer.restoredCamera)
    }

    @Test
    fun repeatedPauseStopResumeIsIdempotentForLogicalState() {
        val renderer = RecordingRenderer()
        val viewModel = MuscleExplorerViewModel(renderer)
        viewModel.selectMuscle("chest")
        viewModel.onPause()
        viewModel.onPause()
        viewModel.onStop()
        viewModel.onStop()
        viewModel.onResume()
        viewModel.onResume()

        assertEquals("chest", viewModel.uiState.value.selectedMuscleGroupId)
        assertTrue(viewModel.uiState.value.isModelReady)
    }

    private class RecordingRenderer : MuscleModelRenderer {
        val events = mutableListOf<String>()
        var restoredCamera: CameraOrbit? = null
        override var onRegionHit: ((String) -> Unit)? = null
        override var onNodeHit: ((String) -> Unit)? = null
        override var onCameraChanged: ((CameraOrbit) -> Unit)? = null
        override fun load(gender: BodyGender) {
            events += "load:$gender"
        }
        override fun setHighlight(muscleGroupId: String?) = Unit
        override fun resetCamera() = Unit
        override fun restoreCamera(camera: CameraOrbit) { restoredCamera = camera }
        override fun onResume() { events += "resume" }
        override fun onPause() { events += "pause" }
        override fun dispose() { events += "dispose" }
    }

    private class RecordingBackend : SceneViewFilamentBackend {
        private var nodeListener: (String) -> Unit = {}
        private var regionListener: (String) -> Unit = {}
        override fun loadLocalGlb(assetPath: String) = Unit
        override fun setHighlight(muscleGroupId: String?) = Unit
        override fun resetCamera() = Unit
        override fun restoreCamera(camera: CameraOrbit) = Unit
        override fun onResume() = Unit
        override fun onPause() = Unit
        override fun dispose() = Unit
        override fun setNodeHitListener(listener: (String) -> Unit) { nodeListener = listener }
        override fun setRegionHitListener(listener: (String) -> Unit) { regionListener = listener }
        override fun setCameraListener(listener: (CameraOrbit) -> Unit) = Unit
        fun emitHit(nodeId: String, regionId: String) {
            nodeListener(nodeId)
            regionListener(regionId)
        }
    }
}
