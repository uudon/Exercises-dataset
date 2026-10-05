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
data class BackupPlan(val id: String, val name: String, val exerciseIds: List<String>, val sets: List<BackupPlannedSet> = emptyList())

@Serializable
data class BackupPlannedSet(val exerciseId: String, val position: Int, val mode: String, val targetReps: Int?, val targetSeconds: Int?, val targetWeightGrams: Long?, val restSeconds: Int)

@Serializable
data class BackupSession(val id: String, val planNameSnapshot: String, val status: String, val startedAt: Long, val finishedAt: Long? = null, val localDate: String = "", val zoneId: String = "")

data class ValidatedBackup(val document: BackupDocument)

class BackupValidationException(message: String) : IllegalArgumentException(message)
