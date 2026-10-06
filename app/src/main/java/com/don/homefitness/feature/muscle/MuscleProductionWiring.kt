package com.don.homefitness.feature.muscle

import android.content.Context
import com.don.homefitness.core.model.TrainingLocation
import com.don.homefitness.data.body.BodyGender
import com.don.homefitness.data.body.BodyModelCheckReport
import com.don.homefitness.data.body.BodyModelValidator
import com.don.homefitness.data.catalog.CatalogExercise

private const val ACTION_MAP_RESOURCE_PATH = "body/muscle-action-map.json"
private const val MODEL_CHECK_RESOURCE_PATH = "body/model-check-report.json"
private const val MODEL_MANIFEST_RESOURCE_PATH = "body/model-manifest.json"
private val HOME_EQUIPMENT = setOf("body weight", "dumbbell")

internal fun loadProductionMuscleActionMapper(
    context: Context,
    catalogExercises: List<CatalogExercise>,
): MuscleActionMapper = runCatching {
    val regionsJson = context.readAsset(REGION_RESOURCE_PATH)
    val mappingsJson = context.readAsset(ACTION_MAP_RESOURCE_PATH)
    val modelCheckReportJson = context.readAsset(MODEL_CHECK_RESOURCE_PATH)
    val modelReport = validatePackagedBodyModels(
        manifestJson = context.readAsset(MODEL_MANIFEST_RESOURCE_PATH),
        assetReader = { path -> context.assets.open(path).use { it.readBytes() } },
    )
    val modelNodeIdsByGender = modelReport.models.associate { it.gender to it.nodeNames.toSet() }
    MuscleActionMapper.fromResources(
        regionsJson = regionsJson,
        mappingsJson = mappingsJson,
        modelCheckReportJson = modelCheckReportJson,
        catalogExercises = catalogExercises,
        modelNodeIdsByGender = modelNodeIdsByGender,
        actualModelReport = modelReport,
    )
}.getOrElse { error ->
    MuscleActionMapper.unavailable(
        "肌肉动作资源初始化失败：${error.message ?: "未知错误"}",
    )
}

internal fun validatePackagedBodyModels(
    manifestJson: String,
    assetReader: (String) -> ByteArray,
): BodyModelCheckReport = BodyModelValidator().validate(manifestJson, assetReader)

internal fun MuscleActionMapper.panelStateFor(
    muscleGroupId: String,
    displayNameZh: String,
): MusclePanelState {
    val actions = actionsFor(
        muscleGroupId = muscleGroupId,
        location = TrainingLocation.HOME,
        availableEquipment = HOME_EQUIPMENT,
    )
    return MusclePanelState(
        muscleGroupId = muscleGroupId,
        displayNameZh = displayNameZh,
        primary = actions.primary,
        secondary = actions.secondary,
        emptyReason = when {
            initializationError != null -> initializationError
            actions.primary.isEmpty() && actions.secondary.isEmpty() -> "暂无可用动作"
            else -> null
        },
    )
}

private fun Context.readAsset(path: String): String =
    assets.open(path).bufferedReader().use { it.readText() }
