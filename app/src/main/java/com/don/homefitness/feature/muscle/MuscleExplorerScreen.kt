package com.don.homefitness.feature.muscle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.don.homefitness.data.body.BodyGender
import com.don.homefitness.data.body.MuscleRegion
import kotlinx.serialization.json.Json

private const val MUSCLE_REGIONS_ASSET = "body/muscle-regions.json"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuscleExplorerScreen(
    viewModel: MuscleExplorerViewModel,
    onOpenCatalog: () -> Unit,
    onOpenExercise: (String) -> Unit = {},
    onMuscleSelected: (String) -> Unit = {},
    panelStateFor: (muscleGroupId: String, displayNameZh: String) -> MusclePanelState =
        { muscleGroupId, displayNameZh -> MusclePanelState(muscleGroupId, displayNameZh) },
    onAddToPlan: (String) -> Unit = {},
    modelRenderer: LocalGlbMuscleModelRenderer? = null,
    initialModelError: String? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val regions = remember(context) { loadMuscleRegions(context) }
    var viewportError by remember(modelRenderer) { mutableStateOf(initialModelError) }
    var cameraResetGeneration by remember(modelRenderer) { mutableIntStateOf(0) }

    LaunchedEffect(state.gender) {
        viewportError = initialModelError
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("3D 人体探索") },
                actions = {
                    TextButton(
                        onClick = onOpenCatalog,
                        modifier = Modifier.semantics { contentDescription = "打开动作库" },
                    ) { Text("动作库") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = state.gender == BodyGender.MALE,
                    onClick = { viewModel.selectGender(BodyGender.MALE) },
                    label = { Text("男性") },
                    modifier = Modifier.semantics { contentDescription = "切换男性模型" },
                )
                FilterChip(
                    selected = state.gender == BodyGender.FEMALE,
                    onClick = { viewModel.selectGender(BodyGender.FEMALE) },
                    label = { Text("女性") },
                    modifier = Modifier.semantics { contentDescription = "切换女性模型" },
                )
                OutlinedButton(
                    onClick = {
                        viewModel.resetCamera()
                        cameraResetGeneration++
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("恢复视角") }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(horizontal = 16.dp),
            ) {
                if (modelRenderer != null && viewportError == null) {
                    key(cameraResetGeneration) {
                        MuscleModelViewport(
                            renderer = modelRenderer,
                            gender = state.gender,
                            selectedMuscleGroupId = state.selectedMuscleGroupId,
                            modifier = Modifier.fillMaxSize(),
                            onRegionSelected = { muscleGroupId ->
                                viewModel.selectMuscle(muscleGroupId)
                                onMuscleSelected(muscleGroupId)
                            },
                            onCameraChanged = viewModel::updateCamera,
                            onError = { viewportError = it },
                            errorContent = { error -> ModelErrorContent(error) },
                        )
                    }
                } else {
                    ModelErrorContent(viewportError ?: "模型暂不可用")
                }
            }

            Text(
                text = state.selectedMuscleGroupId?.let { id ->
                    regions.firstOrNull { it.muscleGroupId == id }?.displayNameZh
                        ?: "已选择：$id"
                } ?: "请选择肌肉区域",
                modifier = Modifier.padding(horizontal = 16.dp).semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
            state.selectedMuscleGroupId?.let { muscleGroupId ->
                val displayName = regions.firstOrNull { it.muscleGroupId == muscleGroupId }?.displayNameZh
                    ?: muscleGroupId
                MuscleExercisePanel(
                    state = panelStateFor(muscleGroupId, displayName),
                    onExerciseClick = onOpenExercise,
                    onOpenCatalog = onOpenCatalog,
                    onAddToPlan = onAddToPlan,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            MuscleTextList(
                regions = regions,
                selectedId = state.selectedMuscleGroupId,
                onSelect = { muscleGroupId ->
                    viewModel.selectMuscle(muscleGroupId)
                    onMuscleSelected(muscleGroupId)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ModelErrorContent(error: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "3D 模型暂时不可用\n$error\n可使用下方文字列表选择肌肉。",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

internal fun loadMuscleRegions(context: android.content.Context): List<MuscleRegion> =
    context.assets.open(MUSCLE_REGIONS_ASSET).bufferedReader().use { reader ->
        Json.decodeFromString(reader.readText())
    }

class MuscleExplorerViewModelFactory(
    private val renderer: MuscleModelRenderer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MuscleExplorerViewModel(renderer) as T
}

class UnavailableMuscleModelRenderer(
    private val message: String,
) : MuscleModelRenderer {
    override var onRegionHit: ((String) -> Unit)? = null
    override var onNodeHit: ((String) -> Unit)? = null
    override var onCameraChanged: ((CameraOrbit) -> Unit)? = null

    override fun load(gender: BodyGender) = throw MuscleModelLoadException(message)
    override fun setHighlight(muscleGroupId: String?) = Unit
    override fun resetCamera() = Unit
    override fun onResume() = Unit
    override fun onPause() = Unit
    override fun dispose() = Unit
}
