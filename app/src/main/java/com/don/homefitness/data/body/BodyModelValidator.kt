package com.don.homefitness.data.body

import java.security.MessageDigest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class BodyModelValidator {
    private val json = Json { ignoreUnknownKeys = false }

    fun validate(manifestJson: String, assetReader: (String) -> ByteArray): BodyModelCheckReport {
        val manifest = try {
            json.decodeFromString<BodyModelManifest>(manifestJson)
        } catch (error: SerializationException) {
            return BodyModelCheckReport(false, listOf("manifest JSON is invalid: ${error.message ?: "parse error"}"), 0, 0)
        }
        val errors = mutableListOf<String>()
        val genders = manifest.entries.groupingBy(BodyModelEntry::gender).eachCount()
        genders.filterValues { it > 1 }.keys.forEach { errors += "$it model is not unique" }
        BodyGender.entries.filterNot(genders::containsKey).forEach { errors += "$it model is missing" }
        var totalBytes = 0L
        manifest.entries.forEachIndexed { index, entry ->
            val label = "entry[$index] ${entry.gender}"
            if (!isRelativeAssetPath(entry.assetPath)) {
                errors += "$label must use a relative android_asset path: ${entry.assetPath}"
                return@forEachIndexed
            }
            if (entry.licenseStatus != CONFIRMED_LICENSE) errors += "$label licenseStatus must be confirmed"
            if (entry.bytes < 0) errors += "$label declares a negative byte count"
            if (!SHA256_PATTERN.matches(entry.sha256)) errors += "$label has an invalid SHA-256"
            val bytes = try {
                assetReader(entry.assetPath)
            } catch (_: Exception) {
                errors += "$label asset does not exist: ${entry.assetPath}"
                return@forEachIndexed
            }
            totalBytes += bytes.size.toLong()
            if (bytes.size.toLong() != entry.bytes) errors += "$label byte count mismatch: expected ${entry.bytes}, got ${bytes.size}"
            if (sha256(bytes) != entry.sha256.lowercase()) errors += "$label SHA-256 mismatch"
        }
        return BodyModelCheckReport(errors.isEmpty(), errors, manifest.entries.size, totalBytes)
    }

    private fun isRelativeAssetPath(path: String): Boolean =
        path.isNotBlank() && !path.startsWith('/') && !path.contains('\\') &&
            !path.split('/').any { it == ".." || it.isBlank() } && !SCHEME_PATTERN.containsMatchIn(path)

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        const val CONFIRMED_LICENSE = "confirmed"
        val SHA256_PATTERN = Regex("[0-9a-fA-F]{64}")
        val SCHEME_PATTERN = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")
    }
}
