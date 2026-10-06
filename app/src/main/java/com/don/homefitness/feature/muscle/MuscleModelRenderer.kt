package com.don.homefitness.feature.muscle

import com.don.homefitness.data.body.BodyGender

interface MuscleModelRenderer {
    var onRegionHit: ((regionId: String) -> Unit)?
    var onCameraChanged: ((camera: CameraOrbit) -> Unit)?
    fun load(gender: BodyGender)
    fun setHighlight(muscleGroupId: String?)
    fun resetCamera()
    fun onResume()
    fun onPause()
    fun dispose()
}

class MuscleModelLoadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Local-only GLB boundary. SceneView/Filament is not in the current dependency graph;
 * [SceneViewFilamentBackend] is the exact seam for a later verified dependency spike.
 * No remote fallback or fake rendered output is provided.
 */
class LocalGlbMuscleModelRenderer(
    private val assetPathByGender: Map<BodyGender, String>,
    private val backend: SceneViewFilamentBackend = UnavailableSceneViewFilamentBackend,
) : MuscleModelRenderer {
    override var onRegionHit: ((String) -> Unit)? = null
    override var onCameraChanged: ((CameraOrbit) -> Unit)? = null

    init {
        backend.setRegionHitListener { regionId -> onRegionHit?.invoke(regionId) }
        backend.setCameraListener { camera -> onCameraChanged?.invoke(camera) }
    }

    override fun load(gender: BodyGender) {
        val path = assetPathByGender[gender]
            ?: throw MuscleModelLoadException("No local GLB asset configured for $gender")
        if (!path.endsWith(".glb", ignoreCase = true) || path.startsWith("/") || path.contains("://") || path.contains("\\")) {
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
    override fun onResume() = backend.onResume()
    override fun onPause() = backend.onPause()
    override fun dispose() = backend.dispose()
}

interface SceneViewFilamentBackend {
    fun loadLocalGlb(assetPath: String)
    fun setHighlight(muscleGroupId: String?)
    fun resetCamera()
    fun onResume()
    fun onPause()
    fun dispose()
    fun setRegionHitListener(listener: (String) -> Unit)
    fun setCameraListener(listener: (CameraOrbit) -> Unit)
}

private object UnavailableSceneViewFilamentBackend : SceneViewFilamentBackend {
    override fun loadLocalGlb(assetPath: String): Nothing = throw MuscleModelLoadException(
        "SceneView/Filament backend is unavailable; local GLB rendering is blocked",
    )
    override fun setHighlight(muscleGroupId: String?) = Unit
    override fun resetCamera() = Unit
    override fun onResume() = Unit
    override fun onPause() = Unit
    override fun dispose() = Unit
    override fun setRegionHitListener(listener: (String) -> Unit) = Unit
    override fun setCameraListener(listener: (CameraOrbit) -> Unit) = Unit
}
