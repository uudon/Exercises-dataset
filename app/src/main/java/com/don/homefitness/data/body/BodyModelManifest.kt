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
)

@Serializable
enum class BodyGender { MALE, FEMALE }

data class BodyModelCheckReport(
    val valid: Boolean,
    val errors: List<String>,
    val modelCount: Int,
    val totalBytes: Long,
)
