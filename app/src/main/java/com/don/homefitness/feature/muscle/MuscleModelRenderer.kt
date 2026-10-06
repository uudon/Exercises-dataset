package com.don.homefitness.feature.muscle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.don.homefitness.data.body.BodyGender
import io.github.sceneview.FrameRatePolicy
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
    private val backend: SceneViewFilamentBackend = ComposeSceneViewFilamentBackend(),
    regionIdToMuscleGroupId: Map<String, String> = DEFAULT_REGION_TO_MUSCLE_GROUP,
) : MuscleModelRenderer {
    override var onRegionHit: ((regionId: String) -> Unit)? = null
    override var onNodeHit: ((nodeId: String) -> Unit)? = null
    override var onCameraChanged: ((camera: CameraOrbit) -> Unit)? = null
    private val regionMapper = ValidatedRegionMuscleGroupMapper(regionIdToMuscleGroupId)

    init {
        backend.setNodeHitListener { nodeId -> onNodeHit?.invoke(nodeId) }
        backend.setRegionHitListener { regionId ->
            regionMapper.muscleGroupIdFor(regionId)?.let { onRegionHit?.invoke(it) }
        }
        backend.setCameraListener { camera -> onCameraChanged?.invoke(camera) }
    }

    override fun load(gender: BodyGender) {
        val path = assetPathByGender[gender]
            ?: throw MuscleModelLoadException("No local GLB asset configured for $gender")
        if (!isValidLocalGlbAssetPath(path)) {
            throw MuscleModelLoadException("Only relative local GLB assets are supported: $path")
        }
        try {
            backend.loadLocalGlb(path)
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
}

interface SceneViewFilamentBackend {
    fun loadLocalGlb(assetPath: String)
    fun setHighlight(muscleGroupId: String?)
    fun resetCamera()
    fun restoreCamera(camera: CameraOrbit)
    fun onResume()
    fun onPause()
    fun dispose()
    fun setNodeHitListener(listener: (String) -> Unit)
    fun setRegionHitListener(listener: (String) -> Unit)
    fun setCameraListener(listener: (CameraOrbit) -> Unit)
}

class ValidatedRegionMuscleGroupMapper(mapping: Map<String, String>) {
    private val mapping = mapping.toMap().also { values ->
        require(values.isNotEmpty()) { "region mapping must not be empty" }
        require(values.keys.none { it.isBlank() }) { "region mapping contains a blank regionId" }
        require(values.values.all { it in CANONICAL_MUSCLE_GROUP_IDS }) {
            "region mapping contains an unknown muscleGroupId"
        }
    }

    fun muscleGroupIdFor(regionId: String): String? = mapping[regionId]

    companion object {
        val CANONICAL_MUSCLE_GROUP_IDS = setOf(
            "chest", "shoulders", "back", "biceps", "triceps", "forearms",
            "abs", "glutes", "quadriceps", "hamstrings", "calves",
        )
    }
}

fun isValidLocalGlbAssetPath(path: String): Boolean {
    if (path.isBlank() || path != path.trim() || !path.endsWith(".glb", ignoreCase = true)) return false
    if (path.startsWith("/") || path.contains('\\') || path.contains("://")) return false
    if (Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(path)) return false
    return path.split('/').none { it.isBlank() || it == "." || it == ".." }
}

private val DEFAULT_REGION_TO_MUSCLE_GROUP = mapOf(
    "chest" to "chest", "shoulders" to "shoulders", "back" to "back",
    "biceps" to "biceps", "triceps" to "triceps", "forearms" to "forearms",
    "abs" to "abs", "glutes" to "glutes", "quadriceps" to "quadriceps",
    "hamstrings" to "hamstrings", "calves" to "calves",
    "chest_left" to "chest", "chest_right" to "chest",
    "sports_bra_left" to "chest", "sports_bra_right" to "chest",
    "shoulders_left" to "shoulders", "shoulders_right" to "shoulders",
    "torso" to "back", "biceps_left" to "biceps", "biceps_right" to "biceps",
    "triceps_left" to "triceps", "triceps_right" to "triceps",
    "forearms_left" to "forearms", "forearms_right" to "forearms",
    "glutes_left" to "glutes", "glutes_right" to "glutes",
    "quadriceps_left" to "quadriceps", "quadriceps_right" to "quadriceps",
    "hamstrings_left" to "hamstrings", "hamstrings_right" to "hamstrings",
    "calves_left" to "calves", "calves_right" to "calves",
)

/** SceneView backend for the local-only Compose model viewport. */
class ComposeSceneViewFilamentBackend : SceneViewFilamentBackend {
    private var assetPath by mutableStateOf<String?>(null)
    private var savedCamera by mutableStateOf(CameraOrbit.DEFAULT)
    private var isResumed by mutableStateOf(true)
    private var highlightedMuscleGroupId by mutableStateOf<String?>(null)
    private var nodeHitListener: (String) -> Unit = {}
    private var regionHitListener: (String) -> Unit = {}
    private var cameraListener: (CameraOrbit) -> Unit = {}

    override fun loadLocalGlb(assetPath: String) {
        check(isValidLocalGlbAssetPath(assetPath)) { "SceneView accepts only validated local GLB paths" }
        this.assetPath = assetPath
    }

    override fun setHighlight(muscleGroupId: String?) { highlightedMuscleGroupId = muscleGroupId }
    override fun resetCamera() { savedCamera = CameraOrbit.DEFAULT }
    override fun restoreCamera(camera: CameraOrbit) { savedCamera = camera.constrained() }
    override fun onResume() { isResumed = true }
    override fun onPause() { isResumed = false }
    override fun dispose() {
        assetPath = null
        isResumed = false
    }
    override fun setNodeHitListener(listener: (String) -> Unit) { nodeHitListener = listener }
    override fun setRegionHitListener(listener: (String) -> Unit) { regionHitListener = listener }
    override fun setCameraListener(listener: (CameraOrbit) -> Unit) { cameraListener = listener }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        val modelPath = assetPath
        val engine = rememberEngine()
        val cameraNode = rememberCameraNode(engine)
        val modelLoader = rememberModelLoader(engine)
        val modelInstance = modelPath?.let { rememberModelInstance(modelLoader, it) }
        val cameraPosition = savedCamera.toPosition()

        DisposableEffect(cameraNode, cameraPosition) {
            cameraNode.worldPosition = cameraPosition
            onDispose { }
        }

        val gestureListener = rememberOnGestureListener(
            onSingleTapConfirmed = { _, node ->
                node?.name?.let { nodeId ->
                    nodeHitListener(nodeId)
                    regionHitListener(nodeId)
                }
            },
        )
        SceneView(
            modifier = modifier,
            cameraNode = cameraNode,
            cameraManipulator = rememberCameraManipulator(orbitHomePosition = cameraPosition),
            frameRatePolicy = FrameRatePolicy.OnDemand(),
            onGestureListener = gestureListener,
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
                    apply = { nodes.forEach { it.isTouchable = true } },
                )
            }
        }
        highlightedMuscleGroupId
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
