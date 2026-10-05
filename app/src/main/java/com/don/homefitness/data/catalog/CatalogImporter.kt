package com.don.homefitness.data.catalog

import kotlinx.serialization.json.Json

class CatalogImportException(message: String) : IllegalArgumentException(message)

class CatalogImporter(
    private val json: Json = Json { ignoreUnknownKeys = false },
    private val sourceCommit: String,
) {
    fun parse(jsonText: String, overlayText: String): List<CatalogExercise> {
        val source = json.decodeFromString<List<ExerciseDto>>(jsonText)
        val overlay = json.decodeFromString<List<ExerciseOverlayDto>>(overlayText)
        val sourceById = source.associateBy { it.id }
        if (sourceById.size != source.size) {
            throw CatalogImportException("源数据包含重复动作 ID")
        }
        if (source.any { it.id.length != 4 || !it.id.all(Char::isDigit) }) {
            throw CatalogImportException("动作 ID 必须是保留前导零的四位数字")
        }
        val overlayIds = overlay.map { it.id }
        if (overlayIds.toSet().size != overlayIds.size) {
            throw CatalogImportException("overlay 包含重复动作 ID")
        }
        val missingSource = overlayIds.filterNot(sourceById::containsKey)
        if (missingSource.isNotEmpty()) {
            throw CatalogImportException("overlay 引用了不存在的动作: ${missingSource.joinToString()}")
        }
        val invalidReview = overlay.filter { it.reviewStatus != "manually-reviewed" }
        if (invalidReview.isNotEmpty()) {
            throw CatalogImportException("家庭白名单中的动作必须经过人工审核")
        }

        return overlay.map { item ->
            val raw = sourceById.getValue(item.id)
            val instructions = raw.instructions["zh"].orEmpty().trim()
            val steps = raw.instruction_steps["zh"].orEmpty().filter(String::isNotBlank)
            CatalogExercise(
                id = raw.id,
                originalName = raw.name,
                nameZh = item.nameZh.trim().ifEmpty { throw CatalogImportException("动作 ${raw.id} 缺少中文名") },
                aliasesZh = item.aliasesZh.map(String::trim).filter(String::isNotEmpty),
                bodyPart = raw.body_part,
                equipment = raw.equipment,
                target = raw.target,
                secondaryMuscles = raw.secondary_muscles,
                instructionsZh = instructions,
                instructionStepsZh = steps,
                requiredEquipment = item.requiredEquipment.toSet(),
                homeEligible = item.homeEligible,
                reviewStatus = item.reviewStatus,
                sourceCommit = sourceCommit,
            )
        }
    }
}
