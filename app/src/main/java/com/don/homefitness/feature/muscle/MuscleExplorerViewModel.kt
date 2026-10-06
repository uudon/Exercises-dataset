package com.don.homefitness.feature.muscle

import androidx.lifecycle.ViewModel
import com.don.homefitness.core.model.TrainingLocation
import com.don.homefitness.data.body.BodyGender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MuscleExplorerViewModel(private val renderer: MuscleModelRenderer) : ViewModel() {
    private val _uiState = MutableStateFlow(MuscleExplorerUiState())
    val uiState: StateFlow<MuscleExplorerUiState> = _uiState.asStateFlow()

    init {
        renderer.onRegionHit = ::selectMuscle
        renderer.onCameraChanged = ::updateCamera
    }

    fun selectGender(gender: BodyGender) {
        _uiState.value = _uiState.value.selectGender(gender).copy(isModelReady = false, errorMessage = null)
        loadCurrentModelIfReady()
    }

    fun selectMuscle(muscleGroupId: String?) {
        _uiState.value = _uiState.value.selectMuscle(muscleGroupId)
        renderer.setHighlight(muscleGroupId)
    }

    fun selectScene(scene: TrainingLocation) { _uiState.value = _uiState.value.copy(scene = scene) }
    fun setPanelExpanded(expanded: Boolean) { _uiState.value = _uiState.value.copy(panelExpanded = expanded) }
    fun updateCamera(camera: CameraOrbit) { _uiState.value = _uiState.value.updateCamera(camera) }

    fun resetCamera() {
        renderer.resetCamera()
        _uiState.value = _uiState.value.resetView()
    }

    fun onResume() {
        loadCurrentModelIfReady()
        renderer.onResume()
    }

    fun onPause() = renderer.onPause()

    fun onStop() {
        renderer.dispose()
        _uiState.value = _uiState.value.copy(isModelReady = false)
    }

    override fun onCleared() {
        renderer.dispose()
        super.onCleared()
    }

    private fun loadCurrentModelIfReady() {
        try {
            renderer.load(_uiState.value.gender)
            _uiState.value = _uiState.value.copy(isModelReady = true, errorMessage = null)
            renderer.setHighlight(_uiState.value.selectedMuscleGroupId)
        } catch (error: MuscleModelLoadException) {
            _uiState.value = _uiState.value.copy(isModelReady = false, errorMessage = error.message ?: "模型加载失败")
        } catch (error: Exception) {
            _uiState.value = _uiState.value.copy(isModelReady = false, errorMessage = "模型加载失败：${error.message ?: "未知错误"}")
        }
    }
}
