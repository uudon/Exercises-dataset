package com.don.homefitness.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.don.homefitness.data.catalog.CatalogExercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(viewModel: CatalogViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedExercise by remember { mutableStateOf<CatalogExercise?>(null) }
    if (selectedExercise != null) {
        ExerciseDetailScreen(
            exercise = selectedExercise!!,
            onBack = { selectedExercise = null },
        )
        return
    }
    Scaffold(topBar = { TopAppBar(title = { Text("动作库") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                label = { Text("搜索动作") },
                singleLine = true,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.equipment == "body weight",
                        onClick = { viewModel.setEquipment(if (state.equipment == "body weight") null else "body weight") },
                        label = { Text("徒手") },
                    )
                }
                item {
                    FilterChip(
                        selected = state.equipment == "dumbbell",
                        onClick = { viewModel.setEquipment(if (state.equipment == "dumbbell") null else "dumbbell") },
                        label = { Text("哑铃") },
                    )
                }
                item { TextButton(onClick = viewModel::clearFilters) { Text("清空筛选") } }
            }
            if (state.exercises.isEmpty()) {
                Text(
                    "没有找到动作，可清空搜索条件后重试。",
                    modifier = Modifier.padding(top = 24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.exercises, key = { it.id }) { exercise ->
                        ExerciseRow(
                            exercise,
                            onClick = { selectedExercise = exercise },
                            onFavoriteClick = { viewModel.toggleFavorite(exercise) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(exercise: CatalogExercise, onClick: () -> Unit, onFavoriteClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(exercise.nameZh, style = MaterialTheme.typography.titleMedium)
            Text(
                "${exercise.bodyPart} · ${exercise.equipment}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        IconButton(
            onClick = onFavoriteClick,
            modifier = Modifier.semantics { contentDescription = "收藏 ${exercise.nameZh}" },
        ) {
            Icon(
                imageVector = if (exercise.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDetailScreen(exercise: CatalogExercise, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise.nameZh) },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                Text("${exercise.bodyPart} · ${exercise.equipment}", style = MaterialTheme.typography.titleMedium)
                Text("目标肌肉：${exercise.target}", modifier = Modifier.padding(top = 8.dp))
                Text("媒体未授权，当前仅展示文字说明。", modifier = Modifier.padding(top = 12.dp))
                Text("动作步骤", modifier = Modifier.padding(top = 20.dp), style = MaterialTheme.typography.titleLarge)
            }
            if (exercise.instructionStepsZh.isEmpty()) {
                item { Text(exercise.instructionsZh.ifBlank { "暂无中文说明" }, modifier = Modifier.padding(top = 8.dp)) }
            } else {
                items(exercise.instructionStepsZh) { step ->
                    Text("• $step", modifier = Modifier.padding(top = 8.dp))
                }
            }
            item {
                Text(
                    "数据来源 commit：${exercise.sourceCommit}",
                    modifier = Modifier.padding(top = 20.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
