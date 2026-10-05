package com.don.homefitness.feature.catalog

import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import com.don.homefitness.data.catalog.CatalogExercise
import com.don.homefitness.data.catalog.MediaResolver
import com.don.homefitness.core.model.TrainingLocation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    mediaResolver: MediaResolver,
    onOpenPlans: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedExercise by remember { mutableStateOf<CatalogExercise?>(null) }
    if (selectedExercise != null) {
        ExerciseDetailScreen(
            exercise = selectedExercise!!,
            mediaResolver = mediaResolver,
            onBack = { selectedExercise = null },
        )
        return
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("动作库") },
                actions = { TextButton(onClick = onOpenHistory) { Text("记录") }; TextButton(onClick = onOpenPlans) { Text("我的计划") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                label = { Text("搜索动作") },
                singleLine = true,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = state.location == TrainingLocation.HOME, onClick = { viewModel.setLocation(TrainingLocation.HOME) }, label = { Text("家庭") }) }
                item { FilterChip(selected = state.location == TrainingLocation.GYM, onClick = { viewModel.setLocation(TrainingLocation.GYM) }, label = { Text("健身房") }) }
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
                            thumbnailPath = mediaResolver.thumbnailPath(exercise.id),
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
private fun ExerciseRow(
    exercise: CatalogExercise,
    thumbnailPath: String?,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (thumbnailPath == null) {
            Text("无图", modifier = Modifier.size(64.dp).padding(8.dp))
        } else {
            LocalAssetImage(
                path = thumbnailPath,
                contentDescription = exercise.nameZh,
                modifier = Modifier.size(64.dp),
            )
        }
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
private fun ExerciseDetailScreen(
    exercise: CatalogExercise,
    mediaResolver: MediaResolver,
    onBack: () -> Unit,
) {
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
                val gifPath = mediaResolver.gifPath(exercise.id)
                if (gifPath == null) {
                    Text("本地 GIF 资源缺失，请查看资源校验报告。", modifier = Modifier.padding(top = 12.dp))
                } else {
                    LocalGifImage(
                        path = gifPath,
                        contentDescription = "${exercise.nameZh} 动作示范",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(MaterialTheme.shapes.large),
                    )
                }
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

@Composable
private fun LocalAssetImage(path: String, contentDescription: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val requestStartedAt = remember(path) { SystemClock.elapsedRealtime() }
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(assetUri(path))
            .listener(onSuccess = { _, _ ->
                Log.i("MediaTiming", "thumbnail path=$path loadMs=${SystemClock.elapsedRealtime() - requestStartedAt}")
            })
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}

@Composable
private fun LocalGifImage(path: String, contentDescription: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED)
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isResumed = true
                Lifecycle.Event.ON_PAUSE -> {
                    isResumed = false
                    Log.i("MediaTiming", "gif paused path=$path")
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (isResumed) {
        val requestStartedAt = remember(path) { SystemClock.elapsedRealtime() }
        val imageLoader = remember(context) {
            ImageLoader.Builder(context)
                .components { add(GifDecoder.Factory()) }
                .build()
        }
        DisposableEffect(imageLoader) {
            onDispose {
                imageLoader.shutdown()
                Log.i("MediaTiming", "gif loader released path=$path")
            }
        }
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(assetUri(path))
                .listener(onSuccess = { _, _ ->
                    Log.i("MediaTiming", "gif path=$path loadMs=${SystemClock.elapsedRealtime() - requestStartedAt}")
                })
                .build(),
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        )
    } else {
        Text("动作详情可见时播放本地 GIF", modifier = modifier.padding(8.dp))
    }
}

private fun assetUri(path: String): String = "file:///android_asset/$path"
