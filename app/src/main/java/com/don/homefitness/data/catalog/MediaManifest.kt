package com.don.homefitness.data.catalog

import kotlinx.serialization.Serializable

@Serializable
data class MediaManifest(
    val entries: List<MediaManifestEntry>,
)

@Serializable
data class MediaManifestEntry(
    val id: String,
    val thumbnailPath: String,
    val gifPath: String,
    val thumbnailBytes: Long,
    val gifBytes: Long,
    val thumbnailSha256: String,
    val gifSha256: String,
)

data class MediaManifestValidation(
    val isValid: Boolean,
    val issues: List<String>,
)

fun validateMediaManifest(
    manifest: MediaManifest,
    actionIds: Set<String>,
): MediaManifestValidation {
    val issues = buildList {
        val grouped = manifest.entries.groupBy(MediaManifestEntry::id)
        grouped.filterValues { it.size > 1 }.keys.forEach { id ->
            add("动作 $id 存在重复媒体映射")
        }
        actionIds.filterNot(grouped::containsKey).forEach { id ->
            add("动作 $id 缺少媒体映射")
        }
        manifest.entries.forEach { entry ->
            if (entry.thumbnailPath.isBlank() || entry.thumbnailBytes <= 0 || entry.thumbnailSha256.isBlank()) {
                add("动作 ${entry.id} 缺少有效缩略图")
            }
            if (entry.gifPath.isBlank() || entry.gifBytes <= 0 || entry.gifSha256.isBlank()) {
                add("动作 ${entry.id} 缺少有效 GIF")
            }
            if (!entry.thumbnailPath.startsWith("catalog/images/")) {
                add("动作 ${entry.id} 缩略图路径错误")
            }
            if (!entry.gifPath.startsWith("catalog/videos/")) {
                add("动作 ${entry.id} GIF 路径错误")
            }
        }
    }
    return MediaManifestValidation(issues.isEmpty(), issues)
}

class MediaResolver(private val manifest: MediaManifest) {
    fun resolve(exerciseId: String): MediaManifestEntry? =
        manifest.entries.firstOrNull { it.id == exerciseId }

    fun thumbnailPath(exerciseId: String): String? = resolve(exerciseId)?.thumbnailPath

    fun gifPath(exerciseId: String): String? = resolve(exerciseId)?.gifPath
}

interface GifPlayer {
    fun play(gifPath: String)
    fun stop()
    fun release()
}

class MediaPlaybackController(private val player: GifPlayer) {
    private var activeExerciseId: String? = null

    fun onDetailVisible(exerciseId: String, gifPath: String = exerciseId) {
        if (activeExerciseId == exerciseId) return
        releaseCurrent()
        activeExerciseId = exerciseId
        player.play(gifPath)
    }

    fun onDetailHidden() = releaseCurrent()

    fun onBackground() = releaseCurrent()

    fun onListVisible() = Unit

    private fun releaseCurrent() {
        if (activeExerciseId == null) return
        player.stop()
        player.release()
        activeExerciseId = null
    }
}
