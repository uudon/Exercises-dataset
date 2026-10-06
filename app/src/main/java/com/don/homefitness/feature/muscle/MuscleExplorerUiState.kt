package com.don.homefitness.feature.muscle

import com.don.homefitness.core.model.TrainingLocation
import com.don.homefitness.data.body.BodyGender

/** Camera values are logical state; no SceneView or Filament type crosses this boundary. */
data class CameraOrbit(
    val azimuth: Float = 0f,
    val elevation: Float = 0f,
    val distance: Float = DEFAULT_DISTANCE,
) {
    fun constrained(): CameraOrbit = copy(
        elevation = elevation.coerceIn(MIN_ELEVATION, MAX_ELEVATION),
        distance = distance.coerceIn(MIN_DISTANCE, MAX_DISTANCE),
    )

    companion object {
        const val DEFAULT_DISTANCE = 1f
        const val MIN_ZOOM = 0.7f
        const val DEFAULT_ZOOM = 1f
        const val MAX_ZOOM = 2.2f
        const val MIN_DISTANCE = 0.45454547f
        const val MAX_DISTANCE = 1.4285715f
        const val MIN_ELEVATION = -80f
        const val MAX_ELEVATION = 80f
        val DEFAULT = CameraOrbit()
    }
}

data class MuscleExplorerUiState(
    val gender: BodyGender = BodyGender.MALE,
    val selectedMuscleGroupId: String? = null,
    val cameraOrbit: CameraOrbit = CameraOrbit.DEFAULT,
    val view: BodyView = BodyView.FRONT,
    val scene: TrainingLocation = TrainingLocation.HOME,
    val isModelReady: Boolean = false,
    val errorMessage: String? = null,
    val panelExpanded: Boolean = false,
) {
    val zoom: Float get() = (CameraOrbit.DEFAULT_DISTANCE / cameraOrbit.distance).coerceIn(MIN_ZOOM, MAX_ZOOM)

    fun selectGender(gender: BodyGender): MuscleExplorerUiState = copy(gender = gender)
    fun selectMuscle(muscleGroupId: String?): MuscleExplorerUiState = copy(
        selectedMuscleGroupId = muscleGroupId,
        panelExpanded = muscleGroupId != null,
    )
    fun selectView(view: BodyView): MuscleExplorerUiState = copy(view = view)
    fun updateCamera(camera: CameraOrbit): MuscleExplorerUiState = copy(cameraOrbit = camera)
    fun zoomBy(scaleFactor: Float): MuscleExplorerUiState =
        updateCamera(cameraOrbit.copy(distance = cameraOrbit.distance / scaleFactor.coerceAtLeast(0.01f)).constrained())
    fun resetView(): MuscleExplorerUiState = copy(cameraOrbit = CameraOrbit.DEFAULT, view = BodyView.FRONT)

    companion object {
        const val MIN_ZOOM = CameraOrbit.MIN_ZOOM
        const val DEFAULT_ZOOM = CameraOrbit.DEFAULT_ZOOM
        const val MAX_ZOOM = CameraOrbit.MAX_ZOOM
    }
}

enum class BodyView { FRONT, SIDE, BACK }
