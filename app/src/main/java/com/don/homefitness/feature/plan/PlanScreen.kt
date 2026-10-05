package com.don.homefitness.feature.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.don.homefitness.core.model.PlanDraft
import com.don.homefitness.core.model.PlanExerciseDraft
import com.don.homefitness.core.model.PlannedSetDraft
import com.don.homefitness.core.model.SetMode
import com.don.homefitness.core.model.WorkoutPlan
import com.don.homefitness.data.catalog.CatalogExercise
import com.don.homefitness.data.catalog.CatalogRepository
import java.util.Locale

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun PlanScreen(
    planRepository: PlanRepository,
    catalogRepository: CatalogRepository,
    onStart: (String) -> Unit = {},
    onBack: () -> Unit,
) {
    val planViewModel: PlanViewModel = viewModel(factory = PlanViewModelFactory(planRepository))
    val plans by planViewModel.plans.collectAsStateWithLifecycle()
    val exercises by catalogRepository.observeExercises(
        query = "",
        bodyPart = null,
        equipment = null,
        availableEquipment = setOf("body weight", "dumbbell"),
    ).collectAsStateWithLifecycle(emptyList())
    var editingPlan by remember { mutableStateOf<WorkoutPlan?>(null) }
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (creating || editingPlan != null) {
        PlanEditorScreen(
            initialPlan = editingPlan,
            exercises = exercises,
            externalError = error,
            onDismissError = { error = null },
            onCancel = { creating = false; editingPlan = null; error = null },
            onSave = { draft ->
                val current = editingPlan
                if (current == null) {
                    planViewModel.save(draft, { error = it }) {
                        creating = false
                        error = null
                    }
                } else {
                    planViewModel.update(current.id, draft, { error = it }) {
                        editingPlan = null
                        error = null
                    }
                }
            },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的计划") },
                navigationIcon = { TextButton(onClick = onBack) { Text("动作库") } },
                actions = { TextButton(onClick = { creating = true }) { Text("新建") } },
            )
        },
    ) { padding ->
        if (plans.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("还没有计划，创建你的第一个计划。", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { creating = true }, modifier = Modifier.padding(top = 16.dp)) { Text("创建计划") }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(plans, key = { it.id }) { plan ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(plan.name, style = MaterialTheme.typography.titleLarge)
                            Text("${plan.exercises.size} 个动作", modifier = Modifier.padding(top = 4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { editingPlan = plan }) { Text("编辑") }
                                TextButton(onClick = { onStart(plan.id) }) { Text("开始训练") }
                                TextButton(onClick = { planViewModel.duplicate(plan.id) { error = it } }) { Text("复制") }
                                TextButton(onClick = { planViewModel.delete(plan.id) { error = it } }) { Text("删除") }
                            }
                        }
                    }
                }
            }
        }
    }
    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            confirmButton = { TextButton(onClick = { error = null }) { Text("知道了") } },
            title = { Text("计划操作失败") },
            text = { Text(message) },
        )
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
private fun PlanEditorScreen(
    initialPlan: WorkoutPlan?,
    exercises: List<CatalogExercise>,
    externalError: String?,
    onDismissError: () -> Unit,
    onCancel: () -> Unit,
    onSave: (PlanDraft) -> Unit,
) {
    var name by remember(initialPlan?.id) { mutableStateOf(initialPlan?.name.orEmpty()) }
    var selectedExercises by remember(initialPlan?.id) {
        mutableStateOf(initialPlan?.toDraftExercises().orEmpty())
    }
    var showExercisePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }
    val initialDraft = remember(initialPlan?.id) {
        initialPlan?.let { PlanDraft(it.name, it.toDraftExercises()) } ?: PlanDraft("", emptyList())
    }
    val currentDraft = PlanDraft(name, selectedExercises)

    fun saveCurrentDraft() {
        val validation = com.don.homefitness.core.validation.SetValidator.validatePlan(currentDraft)
        if (validation.isValid) onSave(currentDraft) else validationError = validation.errors.first()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialPlan == null) "新建计划" else "编辑计划") },
                navigationIcon = {
                    TextButton(onClick = {
                        if (hasUnsavedChanges(initialDraft, currentDraft)) showExitDialog = true else onCancel()
                    }) { Text("取消") }
                },
                actions = {
                    TextButton(onClick = ::saveCurrentDraft) { Text("保存") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    label = { Text("计划名称") },
                    singleLine = true,
                )
            }
            itemsIndexed(selectedExercises, key = { index, item -> "${item.exerciseId}:$index" }) { index, item ->
                val exercise = exercises.firstOrNull { it.id == item.exerciseId }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(exercise?.nameZh ?: item.exerciseId, style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = {
                                selectedExercises = selectedExercises.toMutableList().also { it.removeAt(index) }
                            }) { Text("×") }
                        }
                        item.sets.forEachIndexed { setIndex, set ->
                            SetEditor(
                                set = set,
                                onChange = { changed ->
                                    selectedExercises = selectedExercises.mapIndexed { currentIndex, current ->
                                        if (currentIndex == index) current.copy(
                                            sets = current.sets.mapIndexed { currentSetIndex, currentSet ->
                                                if (currentSetIndex == setIndex) changed else currentSet
                                            },
                                        ) else current
                                    }
                                },
                            )
                            if (item.sets.size > 1) {
                                TextButton(onClick = {
                                    selectedExercises = selectedExercises.mapIndexed { currentIndex, current ->
                                        if (currentIndex == index) current.copy(sets = current.sets.toMutableList().also { it.removeAt(setIndex) }) else current
                                    }
                                }) { Text("删除第 ${setIndex + 1} 组") }
                            }
                        }
                        TextButton(onClick = {
                            selectedExercises = selectedExercises.mapIndexed { currentIndex, current ->
                                if (currentIndex == index) current.copy(sets = current.sets + PlannedSetDraft(SetMode.REPS, targetReps = 10)) else current
                            }
                        }) { Text("添加训练组") }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                if (index > 0) selectedExercises = selectedExercises.toMutableList().also { it.add(index - 1, it.removeAt(index)) }
                            }) { Text("上移") }
                            TextButton(onClick = {
                                if (index < selectedExercises.lastIndex) selectedExercises = selectedExercises.toMutableList().also { it.add(index + 1, it.removeAt(index)) }
                            }) { Text("下移") }
                        }
                    }
                }
            }
            item { Button(onClick = { showExercisePicker = true }, modifier = Modifier.fillMaxWidth()) { Text("添加动作") } }
        }
    }
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("有未保存的修改") },
            text = { Text("是否保存当前计划后退出？") },
            confirmButton = {
                TextButton(onClick = { showExitDialog = false; saveCurrentDraft() }) { Text("保存") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showExitDialog = false; onCancel() }) { Text("放弃") }
                    TextButton(onClick = { showExitDialog = false }) { Text("继续编辑") }
                }
            },
        )
    }
    if (showExercisePicker) {
        AlertDialog(
            onDismissRequest = { showExercisePicker = false },
            confirmButton = { TextButton(onClick = { showExercisePicker = false }) { Text("完成") } },
            title = { Text("选择动作") },
            text = {
                LazyColumn {
                    items(exercises, key = { it.id }) { exercise ->
                        TextButton(onClick = {
                            selectedExercises = selectedExercises + PlanExerciseDraft(exercise.id)
                            showExercisePicker = false
                        }) { Text(exercise.nameZh) }
                    }
                }
            },
        )
    }
    (validationError ?: externalError)?.let { message ->
        AlertDialog(
            onDismissRequest = { validationError = null; onDismissError() },
            confirmButton = { TextButton(onClick = { validationError = null; onDismissError() }) { Text("知道了") } },
            title = { Text(if (externalError == null) "请检查计划" else "计划操作失败") },
            text = { Text(message) },
        )
    }
}

@Composable
private fun SetEditor(set: PlannedSetDraft, onChange: (PlannedSetDraft) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = set.mode == SetMode.REPS, onClick = { onChange(set.copy(mode = SetMode.REPS, targetSeconds = null)) }, label = { Text("次数") })
        FilterChip(selected = set.mode == SetMode.DURATION, onClick = { onChange(set.copy(mode = SetMode.DURATION, targetReps = null)) }, label = { Text("时长") })
    }
    OutlinedTextField(
        value = (set.targetReps ?: set.targetSeconds ?: "").toString(),
        onValueChange = { value ->
            val number = value.toIntOrNull()
            onChange(if (set.mode == SetMode.REPS) set.copy(targetReps = number) else set.copy(targetSeconds = number))
        },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        label = { Text(if (set.mode == SetMode.REPS) "目标次数" else "目标秒数") },
        singleLine = true,
    )
    OutlinedTextField(
        value = set.targetWeightKg.orEmpty(),
        onValueChange = { onChange(set.copy(targetWeightKg = it.ifBlank { null })) },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        label = { Text("单只哑铃重量（kg，可选）") },
        singleLine = true,
    )
    OutlinedTextField(
        value = set.restSeconds.toString(),
        onValueChange = { it.toIntOrNull()?.let { seconds -> onChange(set.copy(restSeconds = seconds)) } },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        label = { Text("组间休息（秒）") },
        singleLine = true,
    )
}

private fun WorkoutPlan.toDraftExercises(): List<PlanExerciseDraft> = exercises.map { exercise ->
    PlanExerciseDraft(
        exerciseId = exercise.exerciseId,
        sets = exercise.sets.map { set ->
            PlannedSetDraft(
                mode = set.mode,
                targetReps = set.targetReps,
                targetSeconds = set.targetSeconds,
                targetWeightKg = set.targetWeightGrams?.let { "%.2f".format(Locale.US, it / 1000.0) },
                restSeconds = set.restSeconds,
            )
        }.ifEmpty { listOf(PlannedSetDraft(SetMode.REPS, targetReps = 10)) },
    )
}
