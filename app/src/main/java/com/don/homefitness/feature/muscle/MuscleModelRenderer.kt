package com.don.homefitness.feature.muscle

import android.content.Context
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.don.homefitness.data.body.BodyGender
import com.don.homefitness.data.body.MuscleRegion
import com.google.android.filament.MaterialInstance
import io.github.sceneview.FrameRatePolicy
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.ModelNode.RenderableNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener
import kotlinx.serialization.json.Json
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val MALE_MODEL_PATH = "body/male/body.glb"
private const val FEMALE_MODEL_PATH = "body/female/body.glb"
private const val REGION_RESOURCE_PATH = "body/muscle-regions.json"
private const val HIGHLIGHT_COLOR_PARAMETER = "baseColorFactor"
private val HIGHLIGHT_COLOR = floatArrayOf(0.95f, 0.25f, 0.12f, 1f)

interface MuscleModelRenderer {
    var onRegionHit: ((regionId: String) -> Unit)?
    var onNodeHit: ((nodeId: String) -> Unit)?
    var onCameraChanged: ((camera: CameraOrbit) -> Unit)?
    fun load(gender: BodyGender)
    fun setHighlight(muscleGroupId: String?)
    fun resetCamera()
    fun restoreCamera(camera: CameraOrbit) {}
    fun onResume()
    fun onPause()
    fun dispose()
}

class MuscleModelLoadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class LocalGlbMuscleModelRenderer(
    private val assetPathByGender: Map<BodyGender, String>,
    private val regionMap: ValidatedMuscleRegionMap,
    private val backend: SceneViewFilamentBackend = ComposeSceneViewFilamentBackend(),
) : MuscleModelRenderer {
    override var onRegionHit: ((regionId: String) -> Unit)? = null
    override var onNodeHit: ((nodeId: String) -> Unit)? = null
    override var onCameraChanged: ((camera: CameraOrbit) -> Unit)? = null

    init {
        backend.setNodeHitListener { nodeId -> onNodeHit?.invoke(nodeId) }
        backend.setRegionHitListener { regionId -> onRegionHit?.invoke(regionId) }
        backend.setCameraListener { camera -> onCameraChanged?.invoke(camera) }
    }

    override fun load(gender: BodyGender) {
        val path = assetPathByGender[gender]
            ?: throw MuscleModelLoadException("No local GLB asset configured for $gender")
        if (!isValidLocalGlbAssetPath(path)) {
            throw MuscleModelLoadException("Only relative local GLB assets are supported: $path")
        }
        try {
            backend.loadLocalGlb(path, gender, regionMap.nodeToRegionByGender[gender].orEmpty())
        } catch (error: MuscleModelLoadException) {
            throw error
        } catch (error: Exception) {
            throw MuscleModelLoadException("Unable to load local GLB asset: $path", error)
        }
    }

    override fun setHighlight(muscleGroupId: String?) = backend.setHighlight(muscleGroupId)
    override fun resetCamera() = backend.resetCamera()
    override fun restoreCamera(camera: CameraOrbit) = backend.restoreCamera(camera)
    override fun onResume() = backend.onResume()
    override fun onPause() = backend.onPause()
    override fun dispose() = backend.dispose()

    @Composable
    fun Content(modifier: Modifier = Modifier) = backend.Content(modifier)
}

interface SceneViewFilamentBackend {
    fun loadLocalGlb(assetPath: String, gender: BodyGender, nodeToRegion: Map<String, String>)
    fun setHighlight(muscleGroupId: String?)
    fun resetCamera()
    fun restoreCamera(camera: CameraOrbit)
    fun onResume()
    fun onPause()
    fun dispose()
    fun setNodeHitListener(listener: (String) -> Unit)
    fun setRegionHitListener(listener: (String) -> Unit)
    fun setCameraListener(listener: (CameraOrbit) -> Unit)
    @Composable
    fun Content(modifier: Modifier = Modifier)
}

class ValidatedMuscleRegionMap private constructor(
    private val regionsById: Map<String, MuscleRegion>,
    val nodeToRegionByGender: Map<BodyGender, Map<String, String>>,
) {
    fun muscleGroupIdFor(regionId: String): String? = regionsById[regionId]?.muscleGroupId
    fun regionIdForNode(gender: BodyGender, nodeId: String): String? = nodeToRegionByGender[gender]?.get(nodeId)

    companion object {
        fun fromRegions(
            regions: List<MuscleRegion>,
            modelNodeIdsByGender: Map<BodyGender, Set<String>>,
        ): ValidatedMuscleRegionMap {
            require(regions.map(MuscleRegion::regionId).distinct().size == regions.size) {
                "region resource contains duplicate regionId"
            }
            require(regions.map(MuscleRegion::muscleGroupId).toSet() == MuscleModelGroupIds) {
                "region resource must cover the 11 canonical muscle groups"
            }
            require(regions.size == MuscleModelGroupIds.size) {
                "region resource must have one region per muscle group"
            }
            val nodeToRegion = BodyGender.entries.associateWith { gender ->
                val modelNodes = modelNodeIdsByGender[gender].orEmpty()
                require(modelNodes.isNotEmpty()) { "$gender model node set is empty" }
                buildMap {
                    regions.forEach { region ->
                        val nodes = region.meshNodeIdsByGender[gender].orEmpty()
                        require(nodes.isNotEmpty()) { "$gender mesh node set is missing for ${region.regionId}" }
                        require(nodes.all { it in modelNodes }) {
                            "$gender mesh node set for ${region.regionId} contains an unknown node"
                        }
                        nodes.forEach { node ->
                            require(put(node, region.regionId) == null) { "$gender node is mapped twice: $node" }
                        }
                    }
                }
            }
            return ValidatedMuscleRegionMap(regions.associateBy(MuscleRegion::regionId), nodeToRegion)
        }

        fun fromJson(jsonText: String): ValidatedMuscleRegionMap {
            val regions = Json.decodeFromString<List<MuscleRegion>>(jsonText)
            val modelNodes = BodyGender.entries.associateWith { gender ->
                regions.flatMap { it.meshNodeIdsByGender[gender].orEmpty() }.toSet()
            }
            return fromRegions(regions, modelNodes)
        }
    }
}

private val MuscleModelGroupIds = setOf(
    "chest", "shoulders", "back", "biceps", "triceps", "forearms",
    "abs", "glutes", "quadriceps", "hamstrings", "calves",
)

fun loadMuscleRegionMap(context: Context): ValidatedMuscleRegionMap = context.assets.open(REGION_RESOURCE_PATH).bufferedReader().use {
    ValidatedMuscleRegionMap.fromJson(it.readText())
}

fun isValidLocalGlbAssetPath(path: String): Boolean {
    if (path.isBlank() || path != path.trim() || !path.endsWith(".glb", ignoreCase = true)) return false
    if (path.startsWith("/") || path.contains('\\') || path.contains("://")) return false
    if (Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(path)) return false
    return path.split('/').none { it.isBlank() || it == "." || it == ".." }
}

@Composable
fun rememberProductionMuscleModelRenderer(): LocalGlbMuscleModelRenderer {
    val context = LocalContext.current
    val regionMap = remember(context) { loadMuscleRegionMap(context) }
    return remember(regionMap) {
        LocalGlbMuscleModelRenderer(
            assetPathByGender = mapOf(
                BodyGender.MALE to MALE_MODEL_PATH,
                BodyGender.FEMALE to FEMALE_MODEL_PATH,
            ),
            regionMap = regionMap,
        )
    }
}

@Composable
fun MuscleModelViewport(
    renderer: LocalGlbMuscleModelRenderer,
    gender: BodyGender,
    selectedMuscleGroupId: String?,
    modifier: Modifier = Modifier,
    onRegionSelected: (String) -> Unit,
    onCameraChanged: (CameraOrbit) -> Unit = {},
) {
    androidx.compose.runtime.DisposableEffect(renderer, onRegionSelected, onCameraChanged) {
        renderer.onRegionHit = onRegionSelected
        renderer.onCameraChanged = onCameraChanged
        onDispose {
            renderer.onRegionHit = null
            renderer.onCameraChanged = null
            renderer.dispose()
        }
    }
    LaunchedEffect(renderer, gender) { renderer.load(gender) }
    renderer.setHighlight(selectedMuscleGroupId)
    renderer.Content(modifier)
}

@Composable
fun MuscleModelViewport(
    gender: BodyGender,
    selectedMuscleGroupId: String?,
    modifier: Modifier = Modifier,
    onRegionSelected: (String) -> Unit,
    onCameraChanged: (CameraOrbit) -> Unit = {},
) {
    val renderer = rememberProductionMuscleModelRenderer()
    MuscleModelViewport(renderer, gender, selectedMuscleGroupId, modifier, onRegionSelected, onCameraChanged)
}

internal class GestureIntentBridge(private val tapSlopPx: Float) {
    private var downX = 0f
    private var downY = 0f
    private var scaleDelta = 0f
    private var intent = GestureIntent.TAP

    fun onDown(event: MotionEvent) {
        onDown(event.x, event.y)
    }

    fun onDown(x: Float, y: Float) {
        downX = x
        downY = y
        scaleDelta = 0f
        intent = GestureIntent.TAP
    }

    fun onMove(event: MotionEvent) {
        onMove(event.x, event.y)
    }

    fun onMove(x: Float, y: Float) {
        intent = classifyGesture(downX, downY, x, y, scaleDelta, tapSlopPx)
    }

    fun onScale(delta: Float) {
        scaleDelta = delta
        intent = GestureIntent.SCALE
    }

    fun canSelect(event: MotionEvent): Boolean = canSelect(event.x, event.y)

    fun canSelect(x: Float, y: Float): Boolean = classifyGesture(
        downX, downY, x, y, scaleDelta, tapSlopPx,
    ) == GestureIntent.TAP && intent == GestureIntent.TAP
}

class ComposeSceneViewFilamentBackend : SceneViewFilamentBackend {
    private var assetPath by mutableStateOf<String?>(null)
    private var savedCamera by mutableStateOf(CameraOrbit.DEFAULT)
    private var isResumed by mutableStateOf(true)
    private var highlightedMuscleGroupId by mutableStateOf<String?>(null)
    private var nodeToRegion by mutableStateOf<Map<String, String>>(emptyMap())
    private var nodeHitListener: (String) -> Unit = {}
    private var regionHitListener: (String) -> Unit = {}
    private var cameraListener: (CameraOrbit) -> Unit = {}

    override fun loadLocalGlb(assetPath: String, gender: BodyGender, nodeToRegion: Map<String, String>) {
        check(isValidLocalGlbAssetPath(assetPath)) { "SceneView accepts only validated local GLB paths" }
        this.assetPath = assetPath
        this.nodeToRegion = nodeToRegion.toMap()
    }

    override fun setHighlight(muscleGroupId: String?) { highlightedMuscleGroupId = muscleGroupId }
    override fun resetCamera() { savedCamera = CameraOrbit.DEFAULT }
    override fun restoreCamera(camera: CameraOrbit) { savedCamera = camera.constrained() }
    override fun onResume() { isResumed = true }
    override fun onPause() { isResumed = false }
    override fun dispose() {
        assetPath = null
        nodeToRegion = emptyMap()
        isResumed = false
    }
    override fun setNodeHitListener(listener: (String) -> Unit) { nodeHitListener = listener }
    override fun setRegionHitListener(listener: (String) -> Unit) { regionHitListener = listener }
    override fun setCameraListener(listener: (CameraOrbit) -> Unit) { cameraListener = listener }

    @Composable
    override fun Content(modifier: Modifier) {
        val modelPath = assetPath
        val engine = rememberEngine()
        val cameraNode = rememberCameraNode(engine)
        val modelLoader = rememberModelLoader(engine)
        val modelInstance = modelPath?.let { rememberModelInstance(modelLoader, it) }
        val cameraPosition = savedCamera.toPosition()
        val gestureBridge = remember { GestureIntentBridge(tapSlopPx = 8f) }
        val highlightController = remember(engine) { MaterialHighlightController() }

        SceneView(
            modifier = modifier,
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            cameraManipulator = rememberCameraManipulator(orbitHomePosition = cameraPosition),
            frameRatePolicy = FrameRatePolicy.OnDemand(),
            onGestureListener = rememberOnGestureListener(
                onDown = { event, _ -> gestureBridge.onDown(event) },
                onScroll = { _, event, _, _ -> gestureBridge.onMove(event) },
                onScale = { _, _, _ -> gestureBridge.onScale(1f) },
                onSingleTapConfirmed = { event, node ->
                    if (gestureBridge.canSelect(event)) {
                        node?.name?.let { nodeId ->
                            nodeHitListener(nodeId)
                            nodeToRegion[nodeId]?.let(regionHitListener)
                        }
                    }
                },
            ),
            onFrame = {
                val camera = cameraNode.worldPosition.toCameraOrbit()
                if (camera != savedCamera) {
                    savedCamera = camera
                    cameraListener(camera)
                }
            },
        ) {
            if (isResumed) modelInstance?.let { instance ->
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 1f,
                    autoAnimate = false,
                    isEditable = false,
                    apply = {
                        nodes.forEach { it.isTouchable = true }
                        highlightController.apply(renderableNodes, nodeToRegion, highlightedMuscleGroupId)
                    },
                )
            }
        }
    }
}

private class MaterialHighlightController {
    private val originalMaterials = mutableMapOf<RenderableNode, List<MaterialInstance>>()
    private val highlightedMaterials = mutableMapOf<RenderableNode, List<MaterialInstance>>()

    fun apply(
        renderableNodes: List<RenderableNode>,
        nodeToRegion: Map<String, String>,
        selectedMuscleGroupId: String?,
    ) {
        renderableNodes.forEach { renderable ->
            val originals = originalMaterials.getOrPut(renderable) { renderable.materialInstances.toList() }
            val materials = if (nodeToRegion[renderable.name] == selectedMuscleGroupId && selectedMuscleGroupId != null) {
                highlightedMaterials.getOrPut(renderable) {
                    originals.mapIndexed { index, material ->
                        MaterialInstance.duplicate(material, "highlight-${renderable.name}-$index").also {
                            it.setParameter(HIGHLIGHT_COLOR_PARAMETER, HIGHLIGHT_COLOR[0], HIGHLIGHT_COLOR[1], HIGHLIGHT_COLOR[2], HIGHLIGHT_COLOR[3])
                        }
                    }
                }
            } else {
                originals
            }
            renderable.materialInstances = materials
        }
    }
}

private fun CameraOrbit.toPosition(): Position {
    val azimuthRadians = Math.toRadians(azimuth.toDouble())
    val elevationRadians = Math.toRadians(elevation.toDouble())
    val horizontal = distance * cos(elevationRadians).toFloat()
    return Position(
        x = horizontal * sin(azimuthRadians).toFloat(),
        y = distance * sin(elevationRadians).toFloat(),
        z = horizontal * cos(azimuthRadians).toFloat(),
    )
}

private fun Position.toCameraOrbit(): CameraOrbit {
    val distance = sqrt(x * x + y * y + z * z).coerceAtLeast(0.001f)
    return CameraOrbit(
        azimuth = Math.toDegrees(atan2(x.toDouble(), z.toDouble())).toFloat(),
        elevation = Math.toDegrees(asin((y / distance).toDouble())).toFloat(),
        distance = distance,
    ).constrained()
}
