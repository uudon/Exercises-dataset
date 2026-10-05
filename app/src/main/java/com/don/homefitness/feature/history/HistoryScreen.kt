package com.don.homefitness.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.don.homefitness.core.model.SessionDetail
import kotlinx.coroutines.launch

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun HistoryScreen(repository: HistoryRepository, onBack: () -> Unit) {
    val sessions by repository.observeHistory().collectAsStateWithLifecycle(emptyList())
    var selected by remember { mutableStateOf<SessionDetail?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("训练记录") }, navigationIcon = { TextButton(onClick = onBack) { Text("动作库") } }) }) { padding ->
        if (sessions.isEmpty()) {
            Text("还没有训练记录，完成一次训练后会显示在这里。", Modifier.padding(padding).padding(24.dp))
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessions, key = { it.id }) { session ->
                    Card(onClick = { HistoryActionScope.launch { selected = repository.session(session.id) } }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(session.planName)
                            Text("${session.localDate} · ${session.status}")
                        }
                    }
                }
            }
        }
    }
    selected?.let { detail ->
        SessionDetailContent(detail = detail, onBack = { selected = null }, onCopy = { HistoryActionScope.launch { repository.copySessionToPlan(detail.id) } })
    }
}

@Composable
private fun SessionDetailContent(detail: SessionDetail, onBack: () -> Unit, onCopy: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(detail.planNameSnapshot)
        Text("开始日期：${detail.localDate}")
        detail.exercises.forEach { exercise ->
            Text(exercise.nameSnapshot)
            exercise.sets.filter { it.status.name == "COMPLETED" }.forEach { set -> Text("第 ${set.position + 1} 组：${set.actualReps ?: set.actualSeconds ?: "-"}") }
        }
        TextButton(onClick = onCopy) { Text("复制为新计划") }
        TextButton(onClick = onBack) { Text("返回") }
    }
}

private object HistoryActionScope : kotlinx.coroutines.CoroutineScope by kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate)
