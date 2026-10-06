package com.don.homefitness.feature.muscle

import android.content.Context
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.don.homefitness.data.body.BodyGender
import com.don.homefitness.data.body.MuscleRegion
import com.google.android.filament.Engine
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
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

internal fun shouldHighlightNode(
    nodeId: String?,
    nodeToMuscleGroup: Map<String, String>,
    selectedMuscleGroupId: String?,
): Boolean = selectedMuscleGroupId != null && nodeToMuscleGroup[nodeId] == selectedMuscleGroupId

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
        attachBackendListeners()
    }

    private fun attachBackendListeners() {
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
            attachBackendListeners()
            backend.loadLocalGlb(
                path,
                gender,
                regionMap.nodeToRegionByGender[gender].orEmpty(),
                regionMap.nodeToMuscleGroupByGender[gender].orEmpty(),
            )
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
    override fun dispose() {
        backend.dispose()
        onRegionHit = null
        onNodeHit = null
        onCameraChanged = null
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) = backend.Content(modifier)
}

interface SceneViewFilamentBackend {
    fun loadLocalGlb(
        assetPath: String,
        gender: BodyGender,
        nodeToRegion: Map<String, String>,
        nodeToMuscleGroup: Map<String, String>,
    )
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
    val nodeToMuscleGroupByGender: Map<BodyGender, Map<String, String>>,
) {
    fun muscleGroupIdFor(regionId: String): String? = regionsById[regionId]?.muscleGroupId
    fun regionIdForNode(gender: BodyGender, nodeId: String): String? = nodeToRegionByGender[gender]?.get(nodeId)
    fun muscleGroupIdForNode(gender: BodyGender, nodeId: String): String? = nodeToMuscleGroupByGender[gender]?.get(nodeId)

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
            val nodeToMuscleGroup = nodeToRegion.mapValues { (_, nodes) ->
                nodes.mapValues { (_, regionId) -> regions.first { it.regionId == regionId }.muscleGroupId }
            }
            return ValidatedMuscleRegionMap(regions.associateBy(MuscleRegion::regionId), nodeToRegion, nodeToMuscleGroup)
        }

        fun fromJson(
            jsonText: String,
            modelNodeIdsByGender: Map<BodyGender, Set<String>>,
        ): ValidatedMuscleRegionMap {
            val regions = Json.decodeFromString<List<MuscleRegion>>(jsonText)
            return fromRegions(regions, modelNodeIdsByGender)
        }
    }
}

private val MuscleModelGroupIds = setOf(
    "chest", "shoulders", "back", "biceps", "triceps", "forearms",
    "abs", "glutes", "quadriceps", "hamstrings", "calves",
)

fun loadMuscleRegionMap(context: Context): ValidatedMuscleRegionMap {
    val regionsJson = context.assets.open(REGION_RESOURCE_PATH).bufferedReader().use { it.readText() }
    val modelNodes = BodyGender.entries.associateWith { gender ->
        val path = if (gender == BodyGender.MALE) MALE_MODEL_PATH else FEMALE_MODEL_PATH
        context.assets.open(path).use(::readGlbNodeNames)
    }
    return ValidatedMuscleRegionMap.fromJson(regionsJson, modelNodes)
}

internal fun readGlbNodeNames(input: InputStream): Set<String> {
    val bytes = input.readBytes()
    require(bytes.size >= 12) { "GLB header is truncated" }
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    require(buffer.int == 0x46546C67) { "GLB magic is invalid" }
    require(buffer.int == 2) { "GLB version is unsupported" }
    val declaredLength = buffer.int
    require(declaredLength == bytes.size) { "GLB length does not match the asset" }
    var jsonChunk: ByteArray? = null
    while (buffer.remaining() >= 8) {
        val chunkLength = buffer.int
        val chunkType = buffer.int
        require(chunkLength >= 0 && chunkLength <= buffer.remaining()) { "GLB chunk is truncated" }
        val chunk = ByteArray(chunkLength)
        buffer.get(chunk)
        if (chunkType == 0x4E4F534A) jsonChunk = chunk
    }
    val nodes = Json.parseToJsonElement(
        jsonChunk?.decodeToString()?.trimEnd('\u0000', ' ') ?: error("GLB JSON chunk is missing"),
    ).jsonObject["nodes"]?.jsonArray ?: error("GLB nodes array is missing")
    return nodes.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }.toSet()
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
    onError: (String) -> Unit = {},
    errorContent: @Composable (String) -> Unit = { message -> Text(message) },
) {
    var loadError by remember(renderer, gender) { mutableStateOf<String?>(null) }
    val savedCamera = remember(renderer) { mutableStateOf(CameraOrbit.DEFAULT) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnError by rememberUpdatedState(onError)
    val currentOnCameraChanged = rememberUpdatedState(onCameraChanged)
    val currentGenderState = rememberUpdatedState(gender)
    val lifecycleController = remember(renderer) {
        MuscleModelLifecycleController(renderer, { currentGenderState.value }) { savedCamera.value }
    }
    fun reportError(error: Throwable) {
        val message = error.message ?: "模型加载失败"
        loadError = message
        currentOnError(message)
    }
    fun bindListeners() {
        renderer.onRegionHit = onRegionSelected
        renderer.onCameraChanged = { camera ->
            savedCamera.value = camera
            currentOnCameraChanged.value(camera)
        }
    }
    androidx.compose.runtime.DisposableEffect(renderer, onRegionSelected, onCameraChanged, lifecycleOwner) {
        bindListeners()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) loadError = null
            if (event == Lifecycle.Event.ON_START) bindListeners()
            lifecycleController.onEvent(event, ::reportError)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            renderer.onRegionHit = null
            renderer.onCameraChanged = null
            renderer.dispose()
        }
    }
    LaunchedEffect(renderer, gender, lifecycleOwner) {
        loadError = null
        lifecycleController.restart()
        bindListeners()
        lifecycleController.sync(lifecycleOwner.lifecycle.currentState, ::reportError)
    }
    renderer.setHighlight(selectedMuscleGroupId)
    if (loadError == null) renderer.Content(modifier) else errorContent(loadError!!)
}

@Composable
fun MuscleModelViewport(
    gender: BodyGender,
    selectedMuscleGroupId: String?,
    modifier: Modifier = Modifier,
    onRegionSelected: (String) -> Unit,
    onCameraChanged: (CameraOrbit) -> Unit = {},
    onError: (String) -> Unit = {},
    errorContent: @Composable (String) -> Unit = { message -> Text(message) },
) {
    val renderer = rememberProductionMuscleModelRenderer()
    MuscleModelViewport(renderer, gender, selectedMuscleGroupId, modifier, onRegionSelected, onCameraChanged, onError, errorContent)
}

internal class MuscleModelLifecycleController(
    private val renderer: MuscleModelRenderer,
    private val currentGender: () -> BodyGender,
    private val savedCamera: () -> CameraOrbit,
) {
    private var started = false
    private var resumed = false

    fun restart() {
        if (started) renderer.dispose()
        started = false
        resumed = false
    }

    fun sync(state: Lifecycle.State, onError: (Throwable) -> Unit) {
        if (state.isAtLeast(Lifecycle.State.STARTED)) start(onError)
        if (state.isAtLeast(Lifecycle.State.RESUMED)) resume()
    }

    fun onEvent(event: Lifecycle.Event, onError: (Throwable) -> Unit) {
        when (event) {
            Lifecycle.Event.ON_START -> start(onError)
            Lifecycle.Event.ON_RESUME -> {
                if (!started) start(onError) else resume()
            }
            Lifecycle.Event.ON_PAUSE -> pause()
            Lifecycle.Event.ON_STOP -> stop()
            else -> Unit
        }
    }

    private fun start(onError: (Throwable) -> Unit) {
        if (started) return
        try {
            renderer.load(currentGender())
            renderer.restoreCamera(savedCamera())
            renderer.onResume()
            started = true
            resumed = true
        } catch (error: Exception) {
            started = false
            resumed = false
            onError(error)
        }
    }

    private fun resume() {
        if (!resumed) {
            renderer.onResume()
            resumed = true
        }
    }

    private fun pause() {
        if (resumed) {
            renderer.onPause()
            resumed = false
        }
    }

    private fun stop() {
        if (started) renderer.dispose()
        started = false
        resumed = false
    }
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
    private var nodeToMuscleGroup by mutableStateOf<Map<String, String>>(emptyMap())
    private var nodeHitListener: (String) -> Unit = {}
    private var regionHitListener: (String) -> Unit = {}
    private var cameraListener: (CameraOrbit) -> Unit = {}
    private var materialHighlightController: MaterialHighlightController? = null

    override fun loadLocalGlb(
        assetPath: String,
        gender: BodyGender,
        nodeToRegion: Map<String, String>,
        nodeToMuscleGroup: Map<String, String>,
    ) {
        check(isValidLocalGlbAssetPath(assetPath)) { "SceneView accepts only validated local GLB paths" }
        materialHighlightController?.dispose()
        this.assetPath = assetPath
        this.nodeToRegion = nodeToRegion.toMap()
        this.nodeToMuscleGroup = nodeToMuscleGroup.toMap()
    }

    override fun setHighlight(muscleGroupId: String?) { highlightedMuscleGroupId = muscleGroupId }
    override fun resetCamera() { savedCamera = CameraOrbit.DEFAULT }
    override fun restoreCamera(camera: CameraOrbit) { savedCamera = camera.constrained() }
    override fun onResume() { isResumed = true }
    override fun onPause() { isResumed = false }
    override fun dispose() {
        materialHighlightController?.dispose()
        materialHighlightController = null
        assetPath = null
        nodeToRegion = emptyMap()
        nodeToMuscleGroup = emptyMap()
        isResumed = false
        nodeHitListener = {}
        regionHitListener = {}
        cameraListener = {}
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
        val highlightController = remember(engine) { MaterialHighlightController(engine) }
        materialHighlightController = highlightController

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
                        highlightController.apply(renderableNodes, nodeToMuscleGroup, highlightedMuscleGroupId)
                    },
                )
            }
        }
    }
}

private class MaterialHighlightController(private val engine: Engine) {
    private val originalMaterials = mutableMapOf<RenderableNode, List<MaterialInstance>>()
    private val highlightedMaterials = mutableMapOf<RenderableNode, List<MaterialInstance>>()

    fun apply(
        renderableNodes: List<RenderableNode>,
        nodeToMuscleGroup: Map<String, String>,
        selectedMuscleGroupId: String?,
    ) {
        renderableNodes.forEach { renderable ->
            val originals = originalMaterials.getOrPut(renderable) { renderable.materialInstances.toList() }
            val materials = if (shouldHighlightNode(renderable.name, nodeToMuscleGroup, selectedMuscleGroupId)) {
                highlightedMaterials.getOrPut(renderable) {
                    originals.mapIndexed { index, material ->
                        MaterialInstance.duplicate(material, "highlight-${renderable.name}-$index").also {
                            it.setParameter(HIGHLIGHT_COLOR_PARAMETER, HIGHLIGHT_COLOR[0], HIGHLIGHT_COLOR[1], HIGHLIGHT_COLOR[2], HIGHLIGHT_COLOR[3])
                        }
                    }
                }
            } else {
                highlightedMaterials.remove(renderable)?.forEach(engine::destroyMaterialInstance)
                originals
            }
            renderable.materialInstances = materials
        }
    }

    fun dispose() {
        originalMaterials.forEach { (renderable, originals) -> renderable.materialInstances = originals }
        highlightedMaterials.values.flatten().forEach(engine::destroyMaterialInstance)
        originalMaterials.clear()
        highlightedMaterials.clear()
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
