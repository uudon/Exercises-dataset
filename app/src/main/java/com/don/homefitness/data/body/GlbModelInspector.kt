package com.don.homefitness.data.body

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal data class GlbInspection(
    val metrics: BodyModelMetrics,
    val errors: List<String>,
)

internal object GlbModelInspector {
    private val json = Json { ignoreUnknownKeys = true }

    fun inspect(bytes: ByteArray, entry: BodyModelEntry): GlbInspection = runCatching {
        require(bytes.size >= 12) { "GLB header is truncated" }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(buffer.int == 0x46546C67) { "GLB magic is invalid" }
        require(buffer.int == 2) { "GLB version is unsupported" }
        require(buffer.int == bytes.size) { "GLB length does not match the asset" }
        var jsonChunk: ByteArray? = null
        while (buffer.remaining() >= 8) {
            val chunkLength = buffer.int
            val chunkType = buffer.int
            require(chunkLength >= 0 && chunkLength <= buffer.remaining()) { "GLB chunk is truncated" }
            val chunk = ByteArray(chunkLength)
            buffer.get(chunk)
            if (chunkType == JSON_CHUNK) jsonChunk = chunk
        }
        val root = json.parseToJsonElement(
            jsonChunk?.decodeToString()?.trimEnd('\u0000', ' ')
                ?: error("GLB JSON chunk is missing"),
        ).jsonObject
        val nodes = root["nodes"]?.jsonArray ?: error("GLB nodes array is missing")
        val meshes = root["meshes"]?.jsonArray ?: error("GLB meshes array is missing")
        val materials = root["materials"]?.jsonArray.orEmpty()
        val accessors = root["accessors"]?.jsonArray ?: error("GLB accessors array is missing")
        val nodeNames = nodes.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
        val triangleCount = meshes.fold(0) { total, mesh ->
            total + mesh.jsonObject["primitives"]?.jsonArray.orEmpty().fold(0) { meshTotal, primitive ->
                val index = primitive.jsonObject["indices"]?.jsonPrimitive?.content?.toInt()
                    ?: error("GLB primitive is missing indices")
                meshTotal + (accessors[index].jsonObject["count"]?.jsonPrimitive?.content?.toInt()
                    ?: error("GLB index accessor is missing count")
                )
            }
        } / 3
        val selectableMeshes = entry.requiredRegions.sumOf { region ->
            nodeNames.count { it.contains(region, ignoreCase = true) }
        }
        val metrics = BodyModelMetrics(
            gender = entry.gender,
            assetPath = entry.assetPath,
            bytes = bytes.size.toLong(),
            meshes = meshes.size,
            materials = materials.size,
            triangles = triangleCount,
            selectableMeshes = selectableMeshes,
            requiredRegions = entry.requiredRegions.filter { region ->
                nodeNames.any { it.contains(region, ignoreCase = true) }
            },
            nodeNames = nodeNames,
        )
        val errors = buildList {
            if (entry.meshes > 0 && entry.meshes != metrics.meshes) {
                add("${entry.gender} mesh count mismatch: expected ${entry.meshes}, got ${metrics.meshes}")
            }
            if (entry.materials > 0 && entry.materials != metrics.materials) {
                add("${entry.gender} material count mismatch: expected ${entry.materials}, got ${metrics.materials}")
            }
            if (entry.triangles > 0 && entry.triangles != metrics.triangles) {
                add("${entry.gender} triangle count mismatch: expected ${entry.triangles}, got ${metrics.triangles}")
            }
            if (entry.selectableMeshes > 0 && entry.selectableMeshes != metrics.selectableMeshes) {
                add("${entry.gender} selectable mesh count mismatch: expected ${entry.selectableMeshes}, got ${metrics.selectableMeshes}")
            }
            val missingRegions = entry.requiredRegions - metrics.requiredRegions.toSet()
            if (missingRegions.isNotEmpty()) add("${entry.gender} required regions missing: ${missingRegions.joinToString()}")
        }
        GlbInspection(metrics, errors)
    }.getOrElse { error ->
        GlbInspection(
            metrics = BodyModelMetrics(entry.gender, entry.assetPath, bytes.size.toLong(), 0, 0, 0, 0, emptyList()),
            errors = listOf("${entry.gender} GLB structure is invalid: ${error.message ?: "parse error"}"),
        )
    }

    private const val JSON_CHUNK = 0x4E4F534A
}
