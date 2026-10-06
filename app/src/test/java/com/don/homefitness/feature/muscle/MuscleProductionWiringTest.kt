package com.don.homefitness.feature.muscle

import com.don.homefitness.data.body.BodyGender
import com.don.homefitness.data.catalog.CatalogImporter
import java.io.File
import java.io.FileInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleProductionWiringTest {
    @Test
    fun validCommittedFixturesProducePanelActionsByExactCatalogId() {
        val assets = File("src/main/assets")
        val catalog = CatalogImporter(sourceCommit = "test").parse(
            jsonText = assets.resolve("catalog/exercises.json").readText(),
            overlayText = assets.resolve("catalog/exercise-overlay.json").readText(),
        )
        val regionsJson = assets.resolve("body/muscle-regions.json").readText()
        val mappingsJson = assets.resolve("body/muscle-action-map.json").readText()
        val validModelReport = """{"valid":true,"errors":[],"modelCount":2,"totalBytes":1535812}"""
        val modelNodes = mapOf(
            BodyGender.MALE to FileInputStream(assets.resolve("body/male/body.glb")).use(::readGlbNodeNames),
            BodyGender.FEMALE to FileInputStream(assets.resolve("body/female/body.glb")).use(::readGlbNodeNames),
        )

        val mapper = MuscleActionMapper.fromResources(
            regionsJson = regionsJson,
            mappingsJson = mappingsJson,
            modelCheckReportJson = validModelReport,
            catalogExercises = catalog,
            modelNodeIdsByGender = modelNodes,
        )

        val panel = mapper.panelStateFor("chest", "胸部")

        assertEquals(null, mapper.initializationError)
        assertEquals(listOf("1273"), panel.primary.map { it.id })
        assertEquals(listOf("0259"), panel.secondary.map { it.id })
        assertTrue(panel.emptyReason == null)
    }
}
