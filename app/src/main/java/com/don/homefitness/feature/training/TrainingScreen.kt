package com.don.homefitness.feature.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.don.homefitness.core.model.SetMode
import com.don.homefitness.core.model.SetResult
import com.don.homefitness.core.model.SetStatus
import kotlinx.coroutines.launch

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun TrainingScreen(
    repository: TrainingRepository,
    sessionId: String,
    onBack: () -> Unit,
) {
    val active by repository.observeActiveSession().collectAsStateWithLifecycle(null)
    var error by remember { mutableStateOf<String?>(null) }
    var selectedSet by remember { mutableStateOf<String?>(null) }
    val session = active
    Scaffold(topBar = { TopAppBar(title = { Text(session?.planNameSnapshot ?: "训练中") }, navigationIcon = { TextButton(onClick = onBack) { Text("退出") } }) }) { padding ->
        if (session == null) {
            Text("正在恢复训练…", Modifier.padding(padding).padding(24.dp))
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(session.exercises, key = { it.id }) { exercise ->
                    Text(exercise.nameSnapshot)
                    exercise.sets.forEach { set ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("第 ${set.position + 1} 组 · ${if (set.mode == SetMode.REPS) "${set.targetReps ?: "-"} 次" else "${set.targetSeconds ?: "-"} 秒"}")
                            when (set.status) {
                                SetStatus.COMPLETED -> Text("已完成")
                                SetStatus.SKIPPED -> Text("已跳过")
                                SetStatus.PENDING -> {
                                    TextButton(onClick = { selectedSet = set.id }) { Text("记录") }
                                    TextButton(onClick = { repository.skipSetAsync(set.id) { error = it } }) { Text("跳过") }
                                }
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { repository.finishAsync(session.id) { error = it } }) { Text("完成训练") }
                        TextButton(onClick = { repository.cancelAsync(session.id) { error = it } }) { Text("取消训练") }
                    }
                }
            }
        }
    }
    selectedSet?.let { setId -> SetResultDialog(onDismiss = { selectedSet = null }, onSave = { result -> repository.saveSetAsync(setId, result) { error = it }; selectedSet = null }) }
    error?.let { message -> AlertDialog(onDismissRequest = { error = null }, confirmButton = { TextButton(onClick = { error = null }) { Text("知道了") } }, title = { Text("训练操作失败") }, text = { Text(message) }) }
}

@Composable
private fun SetResultDialog(onDismiss: () -> Unit, onSave: (SetResult) -> Unit) {
    var reps by remember { mutableStateOf("") }
    var seconds by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("记录本组") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(reps, { reps = it }, label = { Text("完成次数") })
            OutlinedTextField(seconds, { seconds = it }, label = { Text("完成秒数（时长动作）") })
        }
    }, confirmButton = { TextButton(onClick = { onSave(SetResult(if (seconds.isBlank()) SetMode.REPS else SetMode.DURATION, reps.toIntOrNull(), seconds.toIntOrNull())) }) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

private fun TrainingRepository.saveSetAsync(id: String, result: SetResult, onError: (String) -> Unit) = TrainingActionScope.launch { runCatching { saveSet(id, result) }.onFailure { onError(it.message ?: "保存失败") } }
private fun TrainingRepository.skipSetAsync(id: String, onError: (String) -> Unit) = TrainingActionScope.launch { runCatching { skipSet(id) }.onFailure { onError(it.message ?: "跳过失败") } }
private fun TrainingRepository.finishAsync(id: String, onError: (String) -> Unit) = TrainingActionScope.launch { runCatching { finishSession(id) }.onFailure { onError(it.message ?: "完成失败") } }
private fun TrainingRepository.cancelAsync(id: String, onError: (String) -> Unit) = TrainingActionScope.launch { runCatching { cancelSession(id) }.onFailure { onError(it.message ?: "取消失败") } }

private object TrainingActionScope : kotlinx.coroutines.CoroutineScope by kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate)
