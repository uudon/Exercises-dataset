package com.don.homefitness.data.body

import com.don.homefitness.data.catalog.CatalogExercise
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyMappingValidatorTest {
    @Test
    fun rejectsDuplicateRegionIdsAndUnknownMuscleGroups() {
        val regions = listOf(region("chest"), region("chest", group = "neck"))

        val result = BodyMappingValidator().validate(
            regions = regions,
            mappings = listOf(mapping("chest", primary = listOf("0001"))),
            catalogExercises = listOf(exercise("0001", target = "pectorals")),
            modelReport = validModelReport(),
            modelNodeIdsByGender = nodesFor("chest"),
        )

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("duplicate regionId") })
        assertTrue(result.errors.any { it.contains("unknown muscleGroupId") })
    }

    @Test
    fun rejectsMissingFemaleMeshNode() {
        val result = BodyMappingValidator().validate(
            regions = listOf(region("chest")),
            mappings = listOf(mapping("chest", primary = listOf("0001"))),
            catalogExercises = listOf(exercise("0001", target = "pectorals")),
            modelReport = validModelReport(),
            modelNodeIdsByGender = mapOf(
                BodyGender.MALE to setOf("male.chest"),
                BodyGender.FEMALE to emptySet(),
            ),
        )

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("FEMALE") && it.contains("mesh node") })
    }

    @Test
    fun rejectsUnknownActionTargetMismatchAndSecondaryMismatch() {
        val result = BodyMappingValidator().validate(
            regions = listOf(region("chest")),
            mappings = listOf(mapping("chest", primary = listOf("9999", "0002"), secondary = listOf("0003"))),
            catalogExercises = listOf(
                exercise("0002", target = "biceps"),
                exercise("0003", target = "triceps", secondary = listOf("shoulders")),
            ),
            modelReport = validModelReport(),
            modelNodeIdsByGender = nodesFor("chest"),
        )

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("unknown exercise ID: 9999") })
        assertTrue(result.errors.any { it.contains("target") && it.contains("0002") })
        assertTrue(result.errors.any { it.contains("secondary_muscles") && it.contains("0003") })
    }

    @Test
    fun rejectsPrimarySecondaryOverlap() {
        val result = BodyMappingValidator().validate(
            regions = listOf(region("chest")),
            mappings = listOf(mapping("chest", primary = listOf("0001"), secondary = listOf("0001"))),
            catalogExercises = listOf(exercise("0001", target = "pectorals", secondary = listOf("chest"))),
            modelReport = validModelReport(),
            modelNodeIdsByGender = nodesFor("chest"),
        )

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("both primary and secondary") })
    }

    @Test
    fun rejectsUnresolvedModelReportEvenWhenMappingsAreOtherwiseValid() {
        val result = BodyMappingValidator().validate(
            regions = listOf(region("chest")),
            mappings = listOf(mapping("chest", primary = listOf("0001"))),
            catalogExercises = listOf(exercise("0001", target = "pectorals")),
            modelReport = BodyModelCheckReport(
                valid = false,
                errors = listOf("licenseStatus is unconfirmed"),
                modelCount = 2,
                totalBytes = 10,
            ),
            modelNodeIdsByGender = nodesFor("chest"),
        )

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("model-check-report") })
    }

    @Test
    fun acceptsExplicitChestAndBackMappingsForBothGenders() {
        val result = BodyMappingValidator().validate(
            regions = listOf(region("chest"), region("back")),
            mappings = listOf(
                mapping("chest", primary = listOf("0001")),
                mapping("back", primary = listOf("0002")),
            ),
            catalogExercises = listOf(
                exercise("0001", target = "pectorals"),
                exercise("0002", target = "lats"),
            ),
            modelReport = validModelReport(),
            modelNodeIdsByGender = nodesFor("chest", "back"),
        )

        assertTrue(result.valid)
        assertTrue(result.errors.isEmpty())
    }

    private fun region(id: String, group: String = id) = MuscleRegion(
        regionId = id,
        muscleGroupId = group,
        displayNameZh = id,
        meshNodeIdsByGender = mapOf(
            BodyGender.MALE to setOf("male.$id"),
            BodyGender.FEMALE to setOf("female.$id"),
        ),
    )

    private fun mapping(group: String, primary: List<String>, secondary: List<String> = emptyList()) =
        MuscleActionMapping(group, primary, secondary)

    private fun exercise(id: String, target: String, secondary: List<String> = emptyList()) = CatalogExercise(
        id = id,
        originalName = id,
        nameZh = id,
        aliasesZh = emptyList(),
        bodyPart = "body",
        equipment = "body weight",
        target = target,
        secondaryMuscles = secondary,
        instructionsZh = "说明",
        instructionStepsZh = listOf("步骤"),
        requiredEquipment = setOf("body weight"),
        homeEligible = true,
        reviewStatus = "manually-reviewed",
        sourceCommit = "test",
    )

    private fun validModelReport() = BodyModelCheckReport(true, emptyList(), 2, 10)

    private fun nodesFor(vararg ids: String) = BodyGender.entries.associateWith { gender ->
        ids.map { "${gender.name.lowercase()}.$it" }.toSet()
    }
}
