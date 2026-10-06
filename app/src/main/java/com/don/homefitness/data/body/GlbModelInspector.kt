package com.don.homefitness.data.body

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal data class GlbInspection(val metrics: BodyModelMetrics, val errors: List<String>)

internal object GlbModelInspector {
    private val json = Json { ignoreUnknownKeys = true }

    fun inspect(bytes: ByteArray, entry: BodyModelEntry): GlbInspection = try {
        val parsed = parse(bytes)
        val root = parsed.json
        val buffers = root.requiredArray("buffers")
        val views = root.requiredArray("bufferViews")
        val accessors = root.requiredArray("accessors")
        val nodes = root.requiredArray("nodes")
        val meshes = root.requiredArray("meshes")
        val materials = root.requiredArray("materials")
        require(buffers.size == 1) { "GLB must contain exactly one buffer" }
        val bufferLength = buffers[0].jsonObject.requiredInt("byteLength")
        require(bufferLength == parsed.bin.size) { "GLB buffer byteLength does not match BIN chunk" }

        views.forEachIndexed { index, view ->
            val objectView = view.jsonObject
            require(objectView.requiredInt("buffer") == 0) { "bufferView[$index] references an invalid buffer" }
            val offset = objectView.optionalInt("byteOffset") ?: 0
            val length = objectView.requiredInt("byteLength")
            require(offset >= 0 && length >= 0 && offset.toLong() + length.toLong() <= bufferLength.toLong()) { "bufferView[$index] exceeds the BIN buffer" }
            objectView.optionalInt("byteStride")?.let { stride ->
                require(stride in 4..252 && stride % 4 == 0) { "bufferView[$index] has an invalid byteStride" }
            }
        }

        val accessorInfo = accessors.mapIndexed { index, accessor -> validateAccessor(accessor.jsonObject, index, views, parsed.bin) }
        val nodeNames = nodes.mapIndexed { index, node ->
            val objectNode = node.jsonObject
            objectNode.optionalInt("mesh")?.also { mesh -> require(mesh in meshes.indices) { "node[$index] references an invalid mesh" } }
            objectNode["name"]?.jsonPrimitive?.content ?: error("node[$index] is missing name")
        }
        val nodeRegionIds = nodes.mapNotNull { it.jsonObject["extras"]?.jsonObject?.get("region_id")?.jsonPrimitive?.content }
        val selectableNodeCount = nodes.count { it.jsonObject["extras"]?.jsonObject?.get("selectable")?.jsonPrimitive?.content == "true" }

        var triangleCount = 0
        meshes.forEachIndexed { meshIndex, mesh ->
            val primitives = mesh.jsonObject.requiredArray("primitives")
            primitives.forEachIndexed { primitiveIndex, primitive ->
                val objectPrimitive = primitive.jsonObject
                val attributes = objectPrimitive["attributes"]?.jsonObject ?: error("mesh[$meshIndex].primitive[$primitiveIndex] attributes are missing")
                val positionAccessor = attributes["POSITION"]?.asInt("POSITION accessor") ?: error("mesh[$meshIndex].primitive[$primitiveIndex] is missing POSITION")
                require(positionAccessor in accessorInfo.indices) { "POSITION accessor is out of bounds" }
                attributes.values.forEach { require(it.asInt("attribute accessor") in accessorInfo.indices) { "attribute accessor is out of bounds" } }
                val indexAccessor = objectPrimitive["indices"]?.asInt("indices accessor") ?: error("mesh[$meshIndex].primitive[$primitiveIndex] is missing indices")
                require(indexAccessor in accessorInfo.indices) { "indices accessor is out of bounds" }
                val indexInfo = accessorInfo[indexAccessor]
                require(indexInfo.componentType in UNSIGNED_INDEX_TYPES && indexInfo.type == "SCALAR") { "mesh[$meshIndex].primitive[$primitiveIndex] has an invalid index accessor" }
                require(indexInfo.count % 3 == 0) { "mesh[$meshIndex].primitive[$primitiveIndex] index count is not triangular" }
                require(objectPrimitive.requiredInt("material") in materials.indices) { "mesh[$meshIndex].primitive[$primitiveIndex] references an invalid material" }
                validateIndexValues(indexInfo, parsed.bin, accessorInfo[positionAccessor].count)
                triangleCount += indexInfo.count / 3
            }
        }

        require(entry.meshes > 0 && entry.materials > 0 && entry.triangles > 0 && entry.selectableMeshes > 0) { "${entry.gender} required metrics must be positive" }
        require(entry.requiredRegions.isNotEmpty()) { "${entry.gender} required regions are missing" }
        val requiredRegions = entry.requiredRegions.filter { region -> nodeRegionIds.any { it.equals(region, ignoreCase = true) } }
        val selectableMeshes = selectableNodeCount
        val metrics = BodyModelMetrics(
            gender = entry.gender,
            assetPath = entry.assetPath,
            bytes = bytes.size.toLong(),
            meshes = meshes.size,
            materials = materials.size,
            triangles = triangleCount,
            selectableMeshes = selectableMeshes,
            requiredRegions = requiredRegions,
            nodeNames = nodeNames,
            licenseStatus = entry.licenseStatus,
            sourceUrlOrRepository = entry.sourceUrlOrRepository,
            sourceCommitOrVersion = entry.sourceCommitOrVersion,
            license = entry.license,
            attribution = entry.attribution,
            apkRedistributionAuthorization = entry.apkRedistributionAuthorization,
        )
        val errors = buildList {
            if (entry.meshes != metrics.meshes) add("${entry.gender} mesh count mismatch: expected ${entry.meshes}, got ${metrics.meshes}")
            if (entry.materials != metrics.materials) add("${entry.gender} material count mismatch: expected ${entry.materials}, got ${metrics.materials}")
            if (entry.triangles != metrics.triangles) add("${entry.gender} triangle count mismatch: expected ${entry.triangles}, got ${metrics.triangles}")
            if (entry.selectableMeshes != metrics.selectableMeshes) add("${entry.gender} selectable mesh count mismatch: expected ${entry.selectableMeshes}, got ${metrics.selectableMeshes}")
            val missingRegions = entry.requiredRegions - metrics.requiredRegions.toSet()
            if (missingRegions.isNotEmpty()) add("${entry.gender} required regions missing: ${missingRegions.joinToString()}")
        }
        GlbInspection(metrics, errors)
    } catch (error: Exception) {
        GlbInspection(BodyModelMetrics(entry.gender, entry.assetPath, bytes.size.toLong(), 0, 0, 0, 0, emptyList()), listOf("${entry.gender} GLB structure is invalid: ${error.message ?: "parse error"}"))
    }

    private data class ParsedGlb(val json: JsonObject, val bin: ByteArray)
    private data class AccessorInfo(val componentType: Int, val count: Int, val type: String, val offset: Int, val stride: Int)

    private fun parse(bytes: ByteArray): ParsedGlb {
        require(bytes.size >= 20) { "GLB header is truncated" }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(buffer.int == 0x46546C67) { "GLB magic is invalid" }
        require(buffer.int == 2) { "GLB version is unsupported" }
        require(buffer.int == bytes.size) { "GLB length does not match the asset" }
        var jsonChunk: ByteArray? = null
        var binChunk: ByteArray? = null
        while (buffer.hasRemaining()) {
            require(buffer.remaining() >= 8) { "GLB chunk header is truncated" }
            val length = buffer.int
            val type = buffer.int
            require(length >= 0 && length <= buffer.remaining()) { "GLB chunk is truncated" }
            val chunk = ByteArray(length)
            buffer.get(chunk)
            when (type) {
                JSON_CHUNK -> require(jsonChunk == null) { "GLB contains duplicate JSON chunks" }.also { jsonChunk = chunk }
                BIN_CHUNK -> require(binChunk == null) { "GLB contains duplicate BIN chunks" }.also { binChunk = chunk }
            }
        }
        val jsonBytes = jsonChunk ?: error("GLB JSON chunk is missing")
        val binBytes = binChunk ?: error("GLB BIN chunk is missing")
        return ParsedGlb(json.parseToJsonElement(jsonBytes.decodeToString().trimEnd('\u0000', ' ')).jsonObject, binBytes)
    }

    private fun validateAccessor(objectAccessor: JsonObject, index: Int, views: JsonArray, bin: ByteArray): AccessorInfo {
        val viewIndex = objectAccessor.requiredInt("bufferView")
        require(viewIndex in views.indices) { "accessor[$index] references an invalid bufferView" }
        val view = views[viewIndex].jsonObject
        val componentType = objectAccessor.requiredInt("componentType")
        val componentBytes = COMPONENT_BYTES[componentType] ?: error("accessor[$index] has an unsupported componentType")
        val type = objectAccessor.requiredString("type")
        val components = TYPE_COMPONENTS[type] ?: error("accessor[$index] has an unsupported type")
        val count = objectAccessor.requiredInt("count")
        require(count >= 0) { "accessor[$index] has a negative count" }
        val viewOffset = view.optionalInt("byteOffset") ?: 0
        val viewLength = view.requiredInt("byteLength")
        val accessorOffset = objectAccessor.optionalInt("byteOffset") ?: 0
        val stride = view.optionalInt("byteStride") ?: (componentBytes * components)
        require(stride >= componentBytes * components) { "accessor[$index] stride is too small" }
        val byteLength = if (count == 0) 0L else (count - 1).toLong() * stride.toLong() + componentBytes.toLong() * components
        val offset = viewOffset.toLong() + accessorOffset.toLong()
        require(offset >= 0 && offset + byteLength <= bin.size.toLong() && offset + byteLength <= viewOffset.toLong() + viewLength.toLong()) { "accessor[$index] exceeds its bufferView or the BIN buffer" }
        require(offset <= Int.MAX_VALUE) { "accessor[$index] offset is too large" }
        return AccessorInfo(componentType, count, type, offset.toInt(), stride)
    }

    private fun validateIndexValues(info: AccessorInfo, bin: ByteArray, vertexCount: Int) {
        val buffer = ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN)
        repeat(info.count) { index ->
            val position = info.offset + index * info.stride
            val value = when (info.componentType) {
                5121 -> bin[position].toInt() and 0xff
                5123 -> buffer.getShort(position).toInt() and 0xffff
                5125 -> buffer.getInt(position)
                else -> error("unsupported index component type")
            }
            require(value in 0 until vertexCount) { "index accessor contains an out-of-bounds index" }
        }
    }

    private fun JsonObject.requiredArray(name: String): JsonArray = this[name]?.jsonArray ?: error("GLB $name array is missing")
    private fun JsonObject.requiredInt(name: String): Int = this[name]?.asInt(name) ?: error("$name is missing")
    private fun JsonObject.requiredString(name: String): String = this[name]?.jsonPrimitive?.content ?: error("$name is missing")
    private fun JsonObject.optionalInt(name: String): Int? = this[name]?.asInt(name)
    private fun JsonElement.asInt(label: String): Int = jsonPrimitive.content.toIntOrNull() ?: error("$label is not an integer")

    private const val JSON_CHUNK = 0x4E4F534A
    private const val BIN_CHUNK = 0x004E4942
    private val UNSIGNED_INDEX_TYPES = setOf(5121, 5123, 5125)
    private val COMPONENT_BYTES = mapOf(5120 to 1, 5121 to 1, 5122 to 2, 5123 to 2, 5125 to 4, 5126 to 4)
    private val TYPE_COMPONENTS = mapOf("SCALAR" to 1, "VEC2" to 2, "VEC3" to 3, "VEC4" to 4, "MAT2" to 4, "MAT3" to 9, "MAT4" to 16)
}
