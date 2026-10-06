package com.don.homefitness.feature.muscle

import com.don.homefitness.core.model.TrainingLocation
import com.don.homefitness.data.catalog.CatalogExercise
import com.don.homefitness.data.body.MuscleActionMapping
import com.don.homefitness.data.body.BodyMappingCheckReport
import com.don.homefitness.data.body.BodyModelCheckReport
import com.don.homefitness.data.body.BodyModelMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleActionMapperTest {
    @Test
    fun authorizationBlockedKeepsRuntimeActionsAvailableAndBlocksRelease() {
        val mapper = MuscleActionMapper(
            exercises = listOf(exercise("0001", "pectorals", equipment = "body weight")),
            mappings = listOf(MuscleActionMapping("chest", listOf("0001"), emptyList())),
            validation = BodyMappingCheckReport(
                valid = true,
                errors = emptyList(),
                releaseAuthorizationBlocked = true,
            ),
        )

        val result = mapper.actionsFor("chest", TrainingLocation.HOME, setOf("body weight"))

        assertEquals(listOf("0001"), result.primary.map(CatalogExercise::id))
        assertTrue(mapper.releaseAuthorizationBlocked)
        assertEquals(null, mapper.initializationError)
    }

    @Test
    fun malformedModelStillDisablesActions() {
        val mapper = MuscleActionMapper(
            exercises = listOf(exercise("0001", "pectorals", equipment = "body weight")),
            mappings = listOf(MuscleActionMapping("chest", listOf("0001"), emptyList())),
            validation = BodyMappingCheckReport(
                valid = false,
                errors = listOf("model-check-report structure is not runtime-valid"),
            ),
        )

        assertTrue(mapper.actionsFor("chest", TrainingLocation.HOME, setOf("body weight")).primary.isEmpty())
        assertTrue(mapper.initializationError?.contains("runtime-valid") == true)
    }

    @Test
    fun returnsHomeBodyweightAndDumbbellActionsWithPrimaryFirst() {
        val exercises = listOf(
            exercise("0001", "pectorals", equipment = "body weight"),
            exercise("0002", "triceps", equipment = "dumbbell"),
            exercise("0003", "chest", equipment = "barbell", homeEligible = false),
        )
        val mapper = MuscleActionMapper(
            exercises = exercises,
            mappings = listOf(MuscleActionMapping("chest", listOf("0001"), listOf("0002", "0003"))),
        )

        val result = mapper.actionsFor("chest", TrainingLocation.HOME, setOf("body weight", "dumbbell"))

        assertEquals(listOf("0001"), result.primary.map(CatalogExercise::id))
        assertEquals(listOf("0002"), result.secondary.map(CatalogExercise::id))
    }

    @Test
    fun doesNotFuzzyMatchNamesOrReturnGymOnlyActionsInHomeMode() {
        val mapper = MuscleActionMapper(
            exercises = listOf(
                exercise("0001", "pectorals", nameZh = "胸部训练", equipment = "body weight"),
                exercise("0002", "pectorals", nameZh = "胸肌", equipment = "barbell", homeEligible = false),
            ),
            mappings = listOf(MuscleActionMapping("chest", listOf("0002"), emptyList())),
        )

        val result = mapper.actionsFor("胸部", TrainingLocation.HOME, setOf("body weight", "dumbbell"))

        assertTrue(result.primary.isEmpty())
        assertTrue(result.secondary.isEmpty())
    }

    @Test
    fun rejectsUnsupportedEquipmentEvenWhenSelectedEquipmentIncludesIt() {
        val mapper = MuscleActionMapper(
            exercises = listOf(
                exercise("0000", "pectorals", equipment = "barbell", requiredEquipment = setOf("body weight")),
                exercise("0001", "pectorals", equipment = "body weight", requiredEquipment = setOf("body weight", "barbell")),
                exercise("0002", "pectorals", equipment = "dumbbell", requiredEquipment = setOf("dumbbell", "kettlebell")),
            ),
            mappings = listOf(MuscleActionMapping("chest", listOf("0000", "0001", "0002"), emptyList())),
        )

        val result = mapper.actionsFor(
            "chest",
            TrainingLocation.HOME,
            setOf("body weight", "dumbbell", "barbell", "kettlebell"),
        )

        assertTrue(result.primary.isEmpty())
    }

    @Test
    fun observesCatalogExerciseEmissionsAndMapsEachEmission() = runBlocking {
        val emissions = MutableStateFlow(listOf(exercise("0001", "pectorals", equipment = "body weight")))
        val mapper = MuscleActionMapper(
            exercises = emptyList(),
            mappings = listOf(MuscleActionMapping("chest", listOf("0001"), emptyList())),
        )

        val observed = mapper.observeActionsFor(
            source = FakeCatalogExerciseSource(emissions),
            muscleGroupId = "chest",
            availableEquipment = setOf("body weight"),
        )
        assertEquals(listOf("0001"), observed.first().primary.map(CatalogExercise::id))

        emissions.value = listOf(exercise("0002", "pectorals", equipment = "body weight"))

        assertTrue(observed.first().primary.isEmpty())
    }

    @Test
    fun invalidResourceInitializationReturnsSafeEmptyResultAndError() {
        val mapper = MuscleActionMapper.fromResources(
            regionsJson = "[]",
            mappingsJson = "[]",
            modelCheckReportJson = "{\"valid\":true,\"errors\":[],\"modelCount\":2,\"totalBytes\":10}",
            catalogExercises = listOf(exercise("0001", "pectorals", equipment = "body weight")),
            modelNodeIdsByGender = emptyMap(),
        )

        assertTrue(mapper.initializationError?.contains("resource initialization failed") == true)
        assertTrue(mapper.releaseAuthorizationBlocked)
        assertTrue(mapper.actionsFor("chest", TrainingLocation.HOME, setOf("body weight")).primary.isEmpty())
    }

    @Test
    fun unavailableMapperBlocksReleaseAuthorizationAndReturnsNoActions() {
        val mapper = MuscleActionMapper.unavailable("resources unavailable")

        assertTrue(mapper.releaseAuthorizationBlocked)
        assertTrue(mapper.actionsFor("chest", TrainingLocation.HOME, setOf("body weight")).primary.isEmpty())
        assertTrue(mapper.actionsFor("chest", TrainingLocation.HOME, setOf("body weight")).secondary.isEmpty())
    }

    @Test
    fun staleModelReportDisablesRuntimeActions() {
        val mapper = MuscleActionMapper.fromResources(
            regionsJson = "[]",
            mappingsJson = "[]",
            modelCheckReportJson = """
                {
                  "valid": true,
                  "runtimeValid": true,
                  "structureValid": true,
                  "errors": [],
                  "modelCount": 2,
                  "totalBytes": 10,
                  "models": [],
                  "licenseStatus": "confirmed"
                }
            """.trimIndent(),
            catalogExercises = emptyList(),
            modelNodeIdsByGender = emptyMap(),
            actualModelReport = BodyModelCheckReport(
                valid = true,
                errors = emptyList(),
                modelCount = 2,
                totalBytes = 10,
                runtimeValid = true,
                structureValid = true,
                models = listOf(
                    BodyModelMetrics(
                        gender = com.don.homefitness.data.body.BodyGender.MALE,
                        assetPath = "body/male/body.glb",
                        bytes = 5,
                        meshes = 1,
                        materials = 1,
                        triangles = 1,
                        selectableMeshes = 1,
                        requiredRegions = listOf("abs"),
                    ),
                ),
            ),
        )

        assertTrue(mapper.initializationError?.contains("metrics do not match") == true)
        assertTrue(mapper.actionsFor("chest", TrainingLocation.HOME, setOf("body weight")).primary.isEmpty())
    }

    @Test
    fun staleAuthorizationSidecarIsBlockedWithoutDisablingTechnicalRuntime() {
        val mapper = MuscleActionMapper.fromResources(
            regionsJson = "[]",
            mappingsJson = "[]",
            modelCheckReportJson = """
                {
                  "valid": true,
                  "runtimeValid": true,
                  "structureValid": true,
                  "errors": [],
                  "modelCount": 0,
                  "totalBytes": 0,
                  "models": [],
                  "licenseStatus": "confirmed",
                  "authorization": {
                    "licenseStatus": "confirmed",
                    "apkRedistribution": "confirmed",
                    "releaseGate": "authorized"
                  }
                }
            """.trimIndent(),
            catalogExercises = emptyList(),
            modelNodeIdsByGender = emptyMap(),
            actualModelReport = BodyModelCheckReport(
                valid = false,
                errors = listOf("licenseStatus is not confirmed"),
                modelCount = 0,
                totalBytes = 0,
                runtimeValid = true,
                structureValid = true,
                licenseStatus = "blocked",
                authorization = com.don.homefitness.data.body.BodyModelAuthorization(
                    licenseStatus = "blocked",
                    apkRedistribution = "blocked",
                    releaseGate = "blocked",
                ),
            ),
        )

        assertTrue(mapper.releaseAuthorizationBlocked)
        assertTrue(mapper.initializationError?.contains("runtimeValid") != true)
    }

    private fun exercise(
        id: String,
        target: String,
        nameZh: String = id,
        equipment: String,
        homeEligible: Boolean = true,
        requiredEquipment: Set<String> = setOf(equipment),
    ) = CatalogExercise(
        id = id,
        originalName = id,
        nameZh = nameZh,
        aliasesZh = emptyList(),
        bodyPart = "body",
        equipment = equipment,
        target = target,
        secondaryMuscles = emptyList(),
        instructionsZh = "说明",
        instructionStepsZh = listOf("步骤"),
        requiredEquipment = requiredEquipment,
        homeEligible = homeEligible,
        reviewStatus = "manually-reviewed",
        sourceCommit = "test",
    )

    private class FakeCatalogExerciseSource(
        private val emissions: MutableStateFlow<List<CatalogExercise>>,
    ) : CatalogExerciseSource {
        override fun observeExercises(
            query: String,
            bodyPart: String?,
            equipment: String?,
            availableEquipment: Set<String>,
            location: TrainingLocation,
        ) = emissions
    }
}
