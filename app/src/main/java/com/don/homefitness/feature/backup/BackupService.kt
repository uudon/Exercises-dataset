package com.don.homefitness.feature.backup

import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

class BackupService(
    private val json: Json = Json { ignoreUnknownKeys = false; prettyPrint = true },
) {
    fun encode(document: BackupDocument): String = json.encodeToString(BackupDocument.serializer(), document)

    fun exportBackup(output: OutputStream, document: BackupDocument) {
        output.bufferedWriter().use { it.write(encode(document)) }
    }

    fun validateBackup(input: InputStream): ValidatedBackup {
        val document = try {
            json.decodeFromString(BackupDocument.serializer(), input.bufferedReader().use { it.readText() })
        } catch (error: Exception) {
            throw BackupValidationException("备份文件不是有效 JSON：${error.message ?: "格式错误"}")
        }
        require(document.formatVersion == 1) { "不支持的备份版本：${document.formatVersion}" }
        val allIds = document.plans.map { it.id } + document.sessions.map { it.id }
        require(allIds.size == allIds.toSet().size) { "备份中存在重复记录 ID" }
        require(document.plans.all { it.id.isNotBlank() && it.name.trim().isNotEmpty() }) { "备份包含无效计划" }
        require(document.favorites.all { it.isNotBlank() }) { "备份包含无效收藏动作" }
        return ValidatedBackup(document)
    }
}
