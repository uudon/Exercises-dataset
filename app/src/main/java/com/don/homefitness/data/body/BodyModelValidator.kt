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
            return BodyModelCheckReport(
                valid = false,
                errors = listOf("manifest JSON is invalid: ${error.message ?: "parse error"}"),
                modelCount = 0,
                totalBytes = 0,
                runtimeValid = false,
                structureValid = false,
            )
        }
        val errors = mutableListOf<String>()
        val modelReports = mutableListOf<BodyModelMetrics>()
        var runtimeValid = true
        val genders = manifest.entries.groupingBy(BodyModelEntry::gender).eachCount()
        genders.filterValues { it > 1 }.keys.forEach {
            errors += "$it model is not unique"
            runtimeValid = false
        }
        BodyGender.entries.filterNot(genders::containsKey).forEach {
            errors += "$it model is missing"
            runtimeValid = false
        }
        var totalBytes = 0L
        manifest.entries.forEachIndexed { index, entry ->
            val label = "entry[$index] ${entry.gender}"
            if (!isRelativeAssetPath(entry.assetPath)) {
                errors += "$label must use a relative android_asset path: ${entry.assetPath}"
                runtimeValid = false
                return@forEachIndexed
            }
            if (entry.licenseStatus != CONFIRMED_LICENSE) errors += "$label licenseStatus must be confirmed"
            validateMetadata(entry, label, errors)
            if (entry.bytes < 0) {
                errors += "$label declares a negative byte count"
                runtimeValid = false
            }
            if (!SHA256_PATTERN.matches(entry.sha256)) {
                errors += "$label has an invalid SHA-256"
                runtimeValid = false
            }
            val bytes = try {
                assetReader(entry.assetPath)
            } catch (_: Exception) {
                errors += "$label asset does not exist: ${entry.assetPath}"
                runtimeValid = false
                return@forEachIndexed
            }
            totalBytes += bytes.size.toLong()
            if (bytes.size.toLong() != entry.bytes) {
                errors += "$label byte count mismatch: expected ${entry.bytes}, got ${bytes.size}"
                runtimeValid = false
            }
            if (sha256(bytes) != entry.sha256.lowercase()) {
                errors += "$label SHA-256 mismatch"
                runtimeValid = false
            }
            val inspection = GlbModelInspector.inspect(bytes, entry)
            modelReports += inspection.metrics
            if (inspection.errors.isNotEmpty()) {
                runtimeValid = false
                errors += inspection.errors.map { "$label $it" }
            }
        }
        val authorizationValid = manifest.entries.all { entry ->
            entry.licenseStatus == CONFIRMED_LICENSE &&
                !isBlockedValue(entry.sourceUrlOrRepository) &&
                !isBlockedValue(entry.sourceCommitOrVersion) &&
                !isBlockedValue(entry.license) &&
                !isBlockedValue(entry.attribution) &&
                entry.apkRedistributionAuthorization == CONFIRMED_LICENSE
        }
        val releaseAuthorizationBlocked = !runtimeValid || !authorizationValid
        return BodyModelCheckReport(
            valid = errors.isEmpty(),
            errors = errors,
            modelCount = manifest.entries.size,
            totalBytes = totalBytes,
            runtimeValid = runtimeValid,
            structureValid = runtimeValid,
            models = modelReports,
            licenseStatus = if (authorizationValid) "confirmed" else "blocked",
            authorization = BodyModelAuthorization(
                licenseStatus = if (authorizationValid) "confirmed" else "blocked",
                apkRedistribution = if (authorizationValid) "confirmed" else "blocked",
                releaseGate = if (releaseAuthorizationBlocked) "blocked" else "authorized",
            ),
        )
    }

    fun validateReportIntegrity(
        expected: BodyModelCheckReport,
        reported: BodyModelCheckReport,
    ): List<String> = buildList {
        if (reported.valid != expected.valid) {
            add("model-check-report valid does not match packaged manifest")
        }
        if (reported.runtimeValid != expected.runtimeValid) {
            add("model-check-report runtimeValid does not match packaged assets")
        }
        if (reported.structureValid != expected.structureValid) {
            add("model-check-report structureValid does not match packaged assets")
        }
        if (reported.modelCount != expected.modelCount) {
            add("model-check-report modelCount does not match packaged manifest")
        }
        if (reported.totalBytes != expected.totalBytes) {
            add("model-check-report totalBytes does not match packaged assets")
        }
        if (reported.licenseStatus != expected.licenseStatus) {
            add("model-check-report licenseStatus does not match packaged manifest")
        }
        if (reported.authorization != expected.authorization) {
            add("model-check-report authorization does not match packaged manifest")
        }
        expected.models.sortedBy { it.gender }.zip(reported.models.sortedBy { it.gender }).forEach { (actual, sidecar) ->
            if (actual.gender != sidecar.gender || actual.assetPath != sidecar.assetPath ||
                actual.bytes != sidecar.bytes || actual.meshes != sidecar.meshes ||
                actual.materials != sidecar.materials || actual.triangles != sidecar.triangles ||
                actual.selectableMeshes != sidecar.selectableMeshes ||
                actual.requiredRegions != sidecar.requiredRegions || actual.nodeNames != sidecar.nodeNames
            ) {
                add("model-check-report metrics do not match packaged ${actual.gender} GLB")
            }
            if (actual.licenseStatus != sidecar.licenseStatus ||
                actual.sourceUrlOrRepository != sidecar.sourceUrlOrRepository ||
                actual.sourceCommitOrVersion != sidecar.sourceCommitOrVersion ||
                actual.license != sidecar.license ||
                actual.attribution != sidecar.attribution ||
                actual.apkRedistributionAuthorization != sidecar.apkRedistributionAuthorization
            ) {
                add("model-check-report authorization metadata does not match packaged ${actual.gender} manifest")
            }
        }
        if (reported.models.size != expected.models.size) {
            add("model-check-report model metrics do not match packaged manifest")
        }
    }

    private fun validateMetadata(entry: BodyModelEntry, label: String, errors: MutableList<String>) {
        val metadata = listOf(
            "source URL or repository" to entry.sourceUrlOrRepository,
            "source commit or version" to entry.sourceCommitOrVersion,
            "license" to entry.license,
            "attribution" to entry.attribution,
        )
        metadata.forEach { (name, value) ->
            if (value.isBlank() || isBlockedValue(value)) errors += "$label $name is missing or blocked"
        }
        if (entry.apkRedistributionAuthorization != CONFIRMED_LICENSE) {
            errors += "$label APK redistribution authorization must be confirmed"
        }
    }

    private fun isBlockedValue(value: String): Boolean =
        value.trim().lowercase() in setOf("missing", "blocked", "not supplied", "not confirmed")

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
