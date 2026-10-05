package com.don.homefitness.feature.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.don.homefitness.core.model.PlanDraft
import com.don.homefitness.core.model.WorkoutPlan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlanViewModel(private val repository: PlanRepository) : ViewModel() {
    val plans: StateFlow<List<WorkoutPlan>> = repository.observePlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(draft: PlanDraft, onError: (String) -> Unit, onSaved: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.savePlan(draft) }
                .onSuccess { onSaved() }
                .onFailure { onError(it.message ?: "计划保存失败") }
        }
    }

    fun update(planId: String, draft: PlanDraft, onError: (String) -> Unit, onSaved: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.updatePlan(planId, draft) }
                .onSuccess { onSaved() }
                .onFailure { onError(it.message ?: "计划保存失败") }
        }
    }

    fun duplicate(planId: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.duplicatePlan(planId) }
                .onFailure { onError(it.message ?: "计划复制失败") }
        }
    }

    fun delete(planId: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.deletePlan(planId) }
                .onFailure { onError(it.message ?: "计划删除失败") }
        }
    }
}

class PlanViewModelFactory(private val repository: PlanRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = PlanViewModel(repository) as T
}
