package com.don.homefitness.feature.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupDocument(
    val formatVersion: Int = 1,
    val exportedAt: Long,
    val favorites: List<String> = emptyList(),
    val plans: List<BackupPlan> = emptyList(),
    val sessions: List<BackupSession> = emptyList(),
)

@Serializable
data class BackupPlan(val id: String, val name: String, val exerciseIds: List<String>)

@Serializable
data class BackupSession(val id: String, val planNameSnapshot: String, val status: String, val startedAt: Long)

data class ValidatedBackup(val document: BackupDocument)

class BackupValidationException(message: String) : IllegalArgumentException(message)
