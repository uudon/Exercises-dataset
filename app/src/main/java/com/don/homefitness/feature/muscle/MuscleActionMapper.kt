package com.don.homefitness.feature.muscle

import com.don.homefitness.core.model.TrainingLocation
import com.don.homefitness.data.body.BodyGender
import com.don.homefitness.data.body.BodyMappingCheckReport
import com.don.homefitness.data.body.BodyMappingValidator
import com.don.homefitness.data.body.BodyModelCheckReport
import com.don.homefitness.data.body.BodyModelAuthorization
import com.don.homefitness.data.body.BodyModelValidator
import com.don.homefitness.data.body.MuscleActionMapping
import com.don.homefitness.data.body.MuscleRegion
import com.don.homefitness.data.catalog.CatalogExercise
import com.don.homefitness.data.catalog.CatalogRepository
import com.don.homefitness.data.catalog.filterExercisesAtLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class MuscleActionGroups(
    val primary: List<CatalogExercise>,
    val secondary: List<CatalogExercise>,
)

interface CatalogExerciseSource {
    fun observeExercises(
        query: String,
        bodyPart: String?,
        equipment: String?,
        availableEquipment: Set<String>,
        location: TrainingLocation,
    ): Flow<List<CatalogExercise>>
}

class MuscleActionMapper(
    private val exercises: List<CatalogExercise>,
    private val mappings: List<MuscleActionMapping>,
    private val validation: BodyMappingCheckReport = BodyMappingCheckReport(true, emptyList()),
) {
    val initializationError: String? = validation.errors.takeIf { !validation.valid }?.joinToString("; ")
    val releaseAuthorizationBlocked: Boolean = validation.releaseAuthorizationBlocked

    fun actionsFor(
        muscleGroupId: String,
        location: TrainingLocation,
        availableEquipment: Set<String>,
    ): MuscleActionGroups {
        if (!validation.valid) return MuscleActionGroups(emptyList(), emptyList())
        val mapping = mappings.firstOrNull { it.muscleGroupId == muscleGroupId }
            ?: return MuscleActionGroups(emptyList(), emptyList())
        if (location != TrainingLocation.HOME) return MuscleActionGroups(emptyList(), emptyList())

        val available = filterExercisesAtLocation(
            exercises = exercises,
            location = TrainingLocation.HOME,
            query = "",
            bodyPart = null,
            equipment = null,
            availableEquipment = availableEquipment,
        ).filter { it.equipment in HOME_EQUIPMENT }
            .filter { exercise -> exercise.requiredEquipment.all { it in HOME_EQUIPMENT } }
            .associateBy(CatalogExercise::id)
        return MuscleActionGroups(
            primary = mapping.primaryExerciseIds.mapNotNull(available::get),
            secondary = mapping.secondaryExerciseIds.mapNotNull(available::get),
        )
    }

    fun observeActionsFor(
        repository: CatalogRepository,
        muscleGroupId: String,
        availableEquipment: Set<String>,
    ): Flow<MuscleActionGroups> = observeActionsFor(
        source = object : CatalogExerciseSource {
            override fun observeExercises(
                query: String,
                bodyPart: String?,
                equipment: String?,
                availableEquipment: Set<String>,
                location: TrainingLocation,
            ): Flow<List<CatalogExercise>> = repository.observeExercises(
                query = query,
                bodyPart = bodyPart,
                equipment = equipment,
                availableEquipment = availableEquipment,
                location = location,
            )
        },
        muscleGroupId = muscleGroupId,
        availableEquipment = availableEquipment,
    )

    fun observeActionsFor(
        source: CatalogExerciseSource,
        muscleGroupId: String,
        availableEquipment: Set<String>,
    ): Flow<MuscleActionGroups> = source.observeExercises(
        query = "",
        bodyPart = null,
        equipment = null,
        availableEquipment = availableEquipment,
        location = TrainingLocation.HOME,
    ).map { catalogExercises ->
        MuscleActionMapper(catalogExercises, mappings, validation).actionsFor(
            muscleGroupId = muscleGroupId,
            location = TrainingLocation.HOME,
            availableEquipment = availableEquipment,
        )
    }

    companion object {
        private val HOME_EQUIPMENT = setOf("body weight", "dumbbell")

        fun fromResources(
            regionsJson: String,
            mappingsJson: String,
            modelCheckReportJson: String,
            catalogExercises: List<CatalogExercise>,
            modelNodeIdsByGender: Map<BodyGender, Set<String>>,
            actualModelReport: BodyModelCheckReport? = null,
        ): MuscleActionMapper {
            return try {
                val json = Json { ignoreUnknownKeys = true }
                val regions = json.decodeFromString<List<MuscleRegion>>(regionsJson)
                val mappings = json.decodeFromString<List<MuscleActionMapping>>(mappingsJson)
                val report = json.decodeFromString<ModelCheckReportResource>(modelCheckReportJson)
                val parsedModelReport = BodyModelCheckReport(
                    valid = report.valid,
                    errors = report.errors,
                    modelCount = report.modelCount,
                    totalBytes = report.totalBytes,
                    runtimeValid = report.runtimeValid,
                    structureValid = report.structureValid,
                    models = report.models.map { model ->
                        com.don.homefitness.data.body.BodyModelMetrics(
                            gender = model.gender,
                            assetPath = model.assetPath,
                            bytes = model.bytes,
                            meshes = model.meshes,
                            materials = model.materials,
                            triangles = model.triangles,
                            selectableMeshes = model.selectableMeshes,
                            requiredRegions = model.requiredRegions,
                            nodeNames = model.nodeNames,
                            licenseStatus = model.licenseStatus,
                            sourceUrlOrRepository = model.sourceUrlOrRepository,
                            sourceCommitOrVersion = model.sourceCommitOrVersion,
                            license = model.license,
                            attribution = model.attribution,
                            apkRedistributionAuthorization = model.apkRedistributionAuthorization,
                        )
                    },
                    licenseStatus = report.licenseStatus,
                    authorization = report.authorization,
                )
                val reportErrors = actualModelReport?.let {
                    BodyModelValidator().validateReportIntegrity(it, parsedModelReport)
                }.orEmpty()
                val modelReport = if (reportErrors.isEmpty()) parsedModelReport else {
                    val technicalMismatch = reportErrors.any(::isTechnicalIntegrityError)
                    val expectedReport = actualModelReport
                    parsedModelReport.copy(
                        valid = false,
                        runtimeValid = if (technicalMismatch) false else expectedReport?.runtimeValid ?: parsedModelReport.runtimeValid,
                        structureValid = if (technicalMismatch) false else expectedReport?.structureValid ?: parsedModelReport.structureValid,
                        licenseStatus = expectedReport?.licenseStatus ?: parsedModelReport.licenseStatus,
                        authorization = expectedReport?.authorization ?: parsedModelReport.authorization,
                        errors = parsedModelReport.errors + reportErrors,
                    )
                }
                val validation = BodyMappingValidator().validate(
                    regions = regions,
                    mappings = mappings,
                    catalogExercises = catalogExercises,
                    modelReport = modelReport,
                    modelNodeIdsByGender = modelNodeIdsByGender,
                )
                MuscleActionMapper(catalogExercises, mappings, validation)
            } catch (error: Exception) {
                MuscleActionMapper(
                    exercises = emptyList(),
                    mappings = emptyList(),
                    validation = BodyMappingCheckReport(
                        valid = false,
                        errors = listOf("mapping resource initialization failed: ${error.message ?: "unknown error"}"),
                        releaseAuthorizationBlocked = true,
                    ),
                )
            }
        }

        fun unavailable(reason: String): MuscleActionMapper = MuscleActionMapper(
            exercises = emptyList(),
            mappings = emptyList(),
            validation = BodyMappingCheckReport(
                valid = false,
                errors = listOf(reason),
                releaseAuthorizationBlocked = true,
            ),
        )
    }

}

private fun isTechnicalIntegrityError(error: String): Boolean =
    error.contains("runtimeValid") || error.contains("structureValid") || error.contains("modelCount") ||
        error.contains("totalBytes") || error.contains("metrics do not match")

@Serializable
private data class ModelCheckReportResource(
    val valid: Boolean,
    val runtimeValid: Boolean,
    val structureValid: Boolean,
    val errors: List<String>,
    val modelCount: Int,
    val totalBytes: Long,
    val models: List<ModelMetricsResource>,
    val licenseStatus: String,
    val authorization: BodyModelAuthorization = BodyModelAuthorization(),
)

@Serializable
private data class ModelMetricsResource(
    val gender: BodyGender,
    val assetPath: String,
    val bytes: Long,
    val meshes: Int,
    val materials: Int = 0,
    val triangles: Int,
    val selectableMeshes: Int,
    val requiredRegions: List<String>,
    val nodeNames: List<String>,
    val licenseStatus: String = "unknown",
    val sourceUrlOrRepository: String = "unknown",
    val sourceCommitOrVersion: String = "unknown",
    val license: String = "unknown",
    val attribution: String = "unknown",
    val apkRedistributionAuthorization: String = "unknown",
)
