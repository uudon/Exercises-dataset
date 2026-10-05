package com.don.homefitness.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogImporterTest {
    private val importer = CatalogImporter(sourceCommit = "test-commit")

    @Test
    fun importsOnlyReviewedOverlayEntriesAndKeepsLeadingZeroId() {
        val result = importer.parse(sourceJson("0001"), overlayJson("0001"))

        assertEquals(1, result.size)
        assertEquals("0001", result.single().id)
        assertEquals(listOf("步骤一", "步骤二"), result.single().instructionStepsZh)
    }

    @Test
    fun fallsBackToChineseParagraphWhenStepsAreMissing() {
        val exercise = importer.parse(sourceJson("0001", steps = "[]"), overlayJson("0001")).single()

        assertEquals("中文说明", exercise.instructionsZh)
        assertTrue(exercise.instructionStepsZh.isEmpty())
    }

    @Test
    fun rejectsDuplicateOverlayIds() {
        val error = runCatching {
            importer.parse(sourceJson("0001"), "[${overlayEntry("0001")},${overlayEntry("0001") }]")
        }.exceptionOrNull()

        assertTrue(error is CatalogImportException)
        assertEquals("overlay 包含重复动作 ID", error?.message)
    }

    private fun sourceJson(id: String, steps: String = "[\"步骤一\",\"步骤二\"]") =
        """[{"id":"$id","name":"Push-up","category":"chest","body_part":"chest","equipment":"body weight","instructions":{"zh":"中文说明"},"instruction_steps":{"zh":$steps},"muscle_group":"chest","secondary_muscles":[],"target":"pectorals","media_id":"x","image":"images/x.jpg","gif_url":"videos/x.gif","attribution":"x","created_at":"2026-01-01T00:00:00Z"}]"""

    private fun overlayJson(id: String) = "[${overlayEntry(id)}]"

    private fun overlayEntry(id: String) =
        """{"id":"$id","nameZh":"俯卧撑","aliasesZh":["俯卧撑"],"requiredEquipment":["body weight"],"homeEligible":true,"reviewStatus":"manually-reviewed"}"""
}
