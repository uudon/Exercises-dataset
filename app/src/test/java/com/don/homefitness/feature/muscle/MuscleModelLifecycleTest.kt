package com.don.homefitness.feature.muscle

import com.don.homefitness.data.body.BodyGender
import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.io.ByteArrayInputStream
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
            regionMap = testRegionMap(),
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
            regionMap = testRegionMap(),
            backend = backend,
        )
        var nodeId: String? = null
        var muscleGroupId: String? = null
        renderer.onNodeHit = { nodeId = it }
        renderer.onRegionHit = { muscleGroupId = it }

        backend.emitHit("torso", "back")

        assertEquals("torso", nodeId)
        assertEquals("back", muscleGroupId)
    }

    @Test
    fun rendererDisposeClearsPublicListeners() {
        val backend = RecordingBackend()
        val renderer = LocalGlbMuscleModelRenderer(
            assetPathByGender = mapOf(BodyGender.MALE to "body/male/body.glb"),
            regionMap = testRegionMap(),
            backend = backend,
        )
        var hitCount = 0
        renderer.onRegionHit = { hitCount++ }

        renderer.dispose()
        backend.emitHit("torso", "back")

        assertEquals(0, hitCount)
    }

    @Test
    fun rendererForwardsSceneViewModelLoadFailuresToErrorListener() {
        val backend = RecordingBackend()
        val renderer = LocalGlbMuscleModelRenderer(
            assetPathByGender = mapOf(BodyGender.MALE to "body/male/body.glb"),
            regionMap = testRegionMap(),
            backend = backend,
        )
        val errors = mutableListOf<Throwable>()
        renderer.setErrorListener(errors::add)

        backend.emitError(IllegalStateException("missing GLB"))

        assertEquals("missing GLB", errors.single().message)
    }

    @Test
    fun resourceMapValidatesBothGenderNodeSets() {
        val map = testRegionMap()

        assertEquals("chest", map.regionIdForNode(BodyGender.MALE, "chest_left"))
        assertEquals("chest", map.regionIdForNode(BodyGender.FEMALE, "sports_bra_left"))
        assertEquals("chest", map.muscleGroupIdFor("chest"))
    }

    @Test
    fun resourceMapRejectsMissingGenderNodeSet() {
        val resource = java.io.File("src/main/assets/body/muscle-regions.json").readText()
            .replace("\"FEMALE\":[\"chest_left\",\"chest_right\",\"sports_bra_left\",\"sports_bra_right\"]", "\"FEMALE\":[]")

        val error = runCatching { ValidatedMuscleRegionMap.fromJson(resource, actualModelNodes()) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun malformedRegionJsonIsRejectedBeforeRendererCreation() {
        val error = runCatching {
            ValidatedMuscleRegionMap.fromJson("[{not-json}]", actualModelNodes())
        }.exceptionOrNull()

        assertTrue(error != null)
    }

    @Test
    fun missingGlbIsRejectedBeforeNodeMapCreation() {
        val error = runCatching {
            readGlbNodeNames(ByteArrayInputStream(byteArrayOf()))
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("GLB header is truncated", error?.message)
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

    @Test
    fun lifecycleEventsLoadRestorePauseAndDisposeOnlyOnce() {
        val renderer = RecordingRenderer()
        val controller = MuscleModelLifecycleController(renderer, { BodyGender.FEMALE }, { CameraOrbit(7f, 8f, 1.1f) })
        val errors = mutableListOf<Throwable>()

        controller.onEvent(Lifecycle.Event.ON_START, errors::add)
        controller.onEvent(Lifecycle.Event.ON_START, errors::add)
        controller.onEvent(Lifecycle.Event.ON_RESUME, errors::add)
        controller.onEvent(Lifecycle.Event.ON_RESUME, errors::add)
        controller.onEvent(Lifecycle.Event.ON_PAUSE, errors::add)
        controller.onEvent(Lifecycle.Event.ON_PAUSE, errors::add)
        controller.onEvent(Lifecycle.Event.ON_STOP, errors::add)
        controller.onEvent(Lifecycle.Event.ON_STOP, errors::add)

        assertTrue(errors.isEmpty())
        assertEquals(listOf("load:FEMALE", "resume", "pause", "dispose"), renderer.events)
        assertEquals(CameraOrbit(7f, 8f, 1.1f), renderer.restoredCamera)
    }

    @Test
    fun lifecycleLoadFailureIsReportedAndDoesNotEscape() {
        val renderer = RecordingRenderer().also { it.loadFailure = IllegalStateException("broken GLB") }
        val controller = MuscleModelLifecycleController(renderer, { BodyGender.MALE }, { CameraOrbit.DEFAULT })
        val errors = mutableListOf<Throwable>()

        controller.onEvent(Lifecycle.Event.ON_START, errors::add)

        assertEquals("broken GLB", errors.single().message)
        assertEquals(listOf("load:MALE"), renderer.events)
    }

    @Test
    fun regionIdAndMuscleGroupIdRemainSeparate() {
        val resource = java.io.File("src/main/assets/body/muscle-regions.json").readText()
            .replace("\"regionId\":\"chest\",\"muscleGroupId\":\"chest\"", "\"regionId\":\"upper_chest\",\"muscleGroupId\":\"chest\"")
        val map = ValidatedMuscleRegionMap.fromJson(resource, actualModelNodes())

        assertEquals("upper_chest", map.regionIdForNode(BodyGender.MALE, "chest_left"))
        assertEquals("chest", map.muscleGroupIdForNode(BodyGender.MALE, "chest_left"))
        assertTrue(shouldHighlightNode("chest_left", map.nodeToMuscleGroupByGender[BodyGender.MALE].orEmpty(), "chest"))
        assertFalse(shouldHighlightNode("chest_left", map.nodeToMuscleGroupByGender[BodyGender.MALE].orEmpty(), "upper_chest"))
    }

    @Test
    fun packagedGlbsProvideTheNodesUsedByTheRegionMap() {
        val regions = java.io.File("src/main/assets/body/muscle-regions.json").readText()
        val map = ValidatedMuscleRegionMap.fromJson(regions, actualModelNodes())

        assertEquals("torso", map.nodeToRegionByGender[BodyGender.MALE]?.entries?.first { it.value == "back" }?.key)
        assertTrue(map.nodeToRegionByGender[BodyGender.FEMALE]?.containsKey("sports_bra_left") == true)
    }

    private class RecordingRenderer : MuscleModelRenderer {
        val events = mutableListOf<String>()
        var restoredCamera: CameraOrbit? = null
        override var onRegionHit: ((String) -> Unit)? = null
        override var onNodeHit: ((String) -> Unit)? = null
        override var onCameraChanged: ((CameraOrbit) -> Unit)? = null
        var loadFailure: Throwable? = null
        override fun load(gender: BodyGender) {
            events += "load:$gender"
            loadFailure?.let { throw it }
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
        private var errorListener: (Throwable) -> Unit = {}
        override fun loadLocalGlb(assetPath: String, gender: BodyGender, nodeToRegion: Map<String, String>, nodeToMuscleGroup: Map<String, String>) = Unit
        override fun setHighlight(muscleGroupId: String?) = Unit
        override fun resetCamera() = Unit
        override fun restoreCamera(camera: CameraOrbit) = Unit
        override fun onResume() = Unit
        override fun onPause() = Unit
        override fun dispose() = Unit
        override fun setNodeHitListener(listener: (String) -> Unit) { nodeListener = listener }
        override fun setRegionHitListener(listener: (String) -> Unit) { regionListener = listener }
        override fun setCameraListener(listener: (CameraOrbit) -> Unit) = Unit
        override fun setErrorListener(listener: (Throwable) -> Unit) { errorListener = listener }
        @Composable
        override fun Content(modifier: Modifier) = Unit
        fun emitHit(nodeId: String, regionId: String) {
            nodeListener(nodeId)
            regionListener(regionId)
        }
        fun emitError(error: Throwable) = errorListener(error)
    }

    private fun testRegionMap(): ValidatedMuscleRegionMap = ValidatedMuscleRegionMap.fromJson(
        java.io.File("src/main/assets/body/muscle-regions.json").readText(),
        actualModelNodes(),
    )

    private fun actualModelNodes(): Map<BodyGender, Set<String>> = mapOf(
        BodyGender.MALE to java.io.FileInputStream("src/main/assets/body/male/body.glb").use(::readGlbNodeNames),
        BodyGender.FEMALE to java.io.FileInputStream("src/main/assets/body/female/body.glb").use(::readGlbNodeNames),
    )
}
