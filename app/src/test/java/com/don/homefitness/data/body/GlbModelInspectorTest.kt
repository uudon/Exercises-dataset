package com.don.homefitness.data.body

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlbModelInspectorTest {
    @Test
    fun generatedMaleGlbFixtureHasRequiredGeometryAndMetrics() {
        val path = listOf(
            Path.of("src/main/assets/body/male/body.glb"),
            Path.of("app/src/main/assets/body/male/body.glb"),
        ).first(Files::exists)
        val bytes = Files.readAllBytes(path)
        val entry = BodyModelEntry(
            gender = BodyGender.MALE,
            assetPath = "body/male/body.glb",
            sha256 = "0".repeat(64),
            bytes = bytes.size.toLong(),
            licenseStatus = "confirmed",
            sourceUrlOrRepository = "fixture",
            sourceCommitOrVersion = "fixture",
            license = "fixture",
            attribution = "fixture",
            apkRedistributionAuthorization = "confirmed",
            meshes = 36,
            materials = 36,
            triangles = 28928,
            selectableMeshes = 20,
            requiredRegions = listOf("abs", "back", "biceps", "calves", "chest", "forearms", "glutes", "hamstrings", "quadriceps", "shoulders", "triceps"),
        )

        val inspection = GlbModelInspector.inspect(bytes, entry)

        assertTrue(inspection.errors.joinToString(), inspection.errors.isEmpty())
        assertEquals(36, inspection.metrics.meshes)
        assertEquals(28928, inspection.metrics.triangles)
        assertEquals(11, inspection.metrics.requiredRegions.size)
        assertTrue(inspection.metrics.nodeNames.contains("biceps_left"))
    }
}
