package com.don.homefitness.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaManifestTest {
    @Test
    fun validEntriesAreResolvableByActionId() {
        val manifest = MediaManifest(
            entries = listOf(
                MediaManifestEntry(
                    id = "0001",
                    thumbnailPath = "catalog/images/0001.jpg",
                    gifPath = "catalog/videos/0001.gif",
                    thumbnailBytes = 12,
                    gifBytes = 34,
                    thumbnailSha256 = "a",
                    gifSha256 = "b",
                ),
            ),
        )

        val result = validateMediaManifest(manifest, setOf("0001"))

        assertTrue(result.isValid)
        assertEquals("catalog/videos/0001.gif", MediaResolver(manifest).gifPath("0001"))
    }

    @Test
    fun missingAndDuplicateMappingsAreReported() {
        val manifest = MediaManifest(
            entries = listOf(
                MediaManifestEntry("0001", "", "catalog/videos/a.gif", 0, 1, "", "sha"),
                MediaManifestEntry("0001", "catalog/images/a.jpg", "", 1, 0, "sha", ""),
            ),
        )

        val result = validateMediaManifest(manifest, setOf("0001", "0002"))

        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("重复") })
        assertTrue(result.issues.any { it.contains("0002") })
        assertTrue(result.issues.any { it.contains("缩略图") })
    }
}
