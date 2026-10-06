package com.don.homefitness.feature.muscle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.don.homefitness.data.catalog.CatalogExercise

data class MusclePanelState(
    val muscleGroupId: String,
    val displayNameZh: String,
    val primary: List<CatalogExercise> = emptyList(),
    val secondary: List<CatalogExercise> = emptyList(),
    val emptyReason: String? = null,
) {
    val hasActions: Boolean get() = primary.isNotEmpty() || secondary.isNotEmpty()

    companion object {
        fun blocked(muscleGroupId: String, displayNameZh: String): MusclePanelState = MusclePanelState(
            muscleGroupId = muscleGroupId,
            displayNameZh = displayNameZh,
            emptyReason = "资源授权待确认",
        )
    }
}

@Composable
fun MuscleExercisePanel(
    state: MusclePanelState,
    onExerciseClick: (String) -> Unit,
    onOpenCatalog: () -> Unit,
    onAddToPlan: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(state.displayNameZh, style = MaterialTheme.typography.titleLarge)
            if (state.hasActions) {
                ActionGroup(
                    label = "主要动作",
                    exercises = state.primary,
                    onExerciseClick = onExerciseClick,
                    onAddToPlan = onAddToPlan,
                )
                ActionGroup(
                    label = "辅助动作",
                    exercises = state.secondary,
                    onExerciseClick = onExerciseClick,
                    onAddToPlan = onAddToPlan,
                )
            } else {
                Text(state.emptyReason ?: "暂无可用动作")
                TextButton(onClick = onOpenCatalog) { Text("打开动作库") }
            }
        }
    }
}

@Composable
private fun ActionGroup(
    label: String,
    exercises: List<CatalogExercise>,
    onExerciseClick: (String) -> Unit,
    onAddToPlan: (String) -> Unit,
) {
    if (exercises.isEmpty()) return
    Text(label, style = MaterialTheme.typography.titleMedium)
    exercises.forEach { exercise ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                onClick = { onExerciseClick(exercise.id) },
                modifier = Modifier.semantics { contentDescription = "查看 ${exercise.nameZh}" },
            ) { Text(exercise.nameZh) }
            TextButton(onClick = { onAddToPlan(exercise.id) }) { Text("加入计划") }
        }
    }
}
