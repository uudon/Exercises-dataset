package com.don.homefitness.data.body

import kotlinx.serialization.Serializable

@Serializable
data class BodyModelManifest(val entries: List<BodyModelEntry>)

@Serializable
data class BodyModelEntry(
    val gender: BodyGender,
    val assetPath: String,
    val sha256: String,
    val bytes: Long,
    val licenseStatus: String,
    val sourceUrlOrRepository: String,
    val sourceCommitOrVersion: String,
    val license: String,
    val attribution: String,
    val apkRedistributionAuthorization: String,
    val meshes: Int,
    val materials: Int,
    val triangles: Int,
    val selectableMeshes: Int,
    val requiredRegions: List<String>,
)

@Serializable
enum class BodyGender { MALE, FEMALE }

data class BodyModelCheckReport(
    val valid: Boolean,
    val errors: List<String>,
    val modelCount: Int,
    val totalBytes: Long,
    val runtimeValid: Boolean = true,
    val structureValid: Boolean = true,
    val models: List<BodyModelMetrics> = emptyList(),
    val licenseStatus: String = "unknown",
    val authorization: BodyModelAuthorization = BodyModelAuthorization(),
)

@Serializable
data class BodyModelAuthorization(
    val licenseStatus: String = "unknown",
    val apkRedistribution: String = "unknown",
    val releaseGate: String = "unknown",
)

@Serializable
data class BodyModelMetrics(
    val gender: BodyGender,
    val assetPath: String,
    val bytes: Long,
    val meshes: Int,
    val materials: Int,
    val triangles: Int,
    val selectableMeshes: Int,
    val requiredRegions: List<String>,
    val nodeNames: List<String> = emptyList(),
    val licenseStatus: String = "unknown",
    val sourceUrlOrRepository: String = "unknown",
    val sourceCommitOrVersion: String = "unknown",
    val license: String = "unknown",
    val attribution: String = "unknown",
    val apkRedistributionAuthorization: String = "unknown",
)
