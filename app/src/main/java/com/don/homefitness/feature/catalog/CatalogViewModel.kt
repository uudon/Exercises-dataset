package com.don.homefitness.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import com.don.homefitness.data.catalog.CatalogExercise
import com.don.homefitness.data.catalog.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CatalogUiState(
    val query: String = "",
    val bodyPart: String? = null,
    val equipment: String? = null,
    val exercises: List<CatalogExercise> = emptyList(),
)

class CatalogViewModel(private val repository: CatalogRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val bodyPart = MutableStateFlow<String?>(null)
    private val equipment = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CatalogUiState> = combine(query, bodyPart, equipment) { q, part, item ->
        Triple(q, part, item)
    }.flatMapLatest { (q, part, item) ->
        repository.observeExercises(
            query = q,
            bodyPart = part,
            equipment = item,
            availableEquipment = setOf("body weight", "dumbbell"),
        ).map { exercises -> CatalogUiState(q, part, item, exercises) }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState())

    fun setQuery(value: String) { query.value = value }

    fun setBodyPart(value: String?) { bodyPart.value = value }

    fun setEquipment(value: String?) { equipment.value = value }

    fun clearFilters() {
        query.value = ""
        bodyPart.value = null
        equipment.value = null
    }

    fun toggleFavorite(exercise: CatalogExercise) {
        viewModelScope.launch { repository.setFavorite(exercise.id, !exercise.isFavorite) }
    }
}

class CatalogViewModelFactory(
    private val repository: CatalogRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CatalogViewModel(repository) as T
}
