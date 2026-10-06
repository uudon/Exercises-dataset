package com.don.homefitness.data.body

import com.don.homefitness.data.catalog.CatalogExercise
import kotlinx.serialization.json.Json

class BodyMappingValidator(
    private val json: Json = Json { ignoreUnknownKeys = false },
) {
    fun validate(
        regions: List<MuscleRegion>,
        mappings: List<MuscleActionMapping>,
        catalogExercises: List<CatalogExercise>,
        modelReport: BodyModelCheckReport,
        modelNodeIdsByGender: Map<BodyGender, Set<String>>,
    ): BodyMappingCheckReport {
        val errors = mutableListOf<String>()
        val regionsById = regions.groupBy(MuscleRegion::regionId)
        regionsById.filterValues { it.size > 1 }.keys.forEach {
            errors += "duplicate regionId: $it"
        }
        if (!modelReport.valid) errors += "model-check-report is not valid: ${modelReport.errors.joinToString()}"

        regions.forEach { region ->
            if (region.muscleGroupId !in CANONICAL_MUSCLE_GROUP_IDS) {
                errors += "unknown muscleGroupId: ${region.muscleGroupId}"
            }
            if (region.displayNameZh.isBlank()) errors += "region ${region.regionId} has no displayNameZh"
            BodyGender.values().forEach { gender ->
                val nodes = region.meshNodeIdsByGender[gender].orEmpty()
                if (nodes.isEmpty()) {
                    errors += "${gender.name} mesh node set is missing for region ${region.regionId}"
                } else {
                    nodes.filterNot(modelNodeIdsByGender[gender].orEmpty()::contains).forEach { node ->
                        errors += "${gender.name} mesh node is unknown for region ${region.regionId}: $node"
                    }
                }
            }
        }

        val catalogById = catalogExercises.associateBy(CatalogExercise::id)
        if (catalogById.size != catalogExercises.size) errors += "catalog contains duplicate exercise ID"
        val mappingsByGroup = mappings.groupBy(MuscleActionMapping::muscleGroupId)
        mappingsByGroup.filterValues { it.size > 1 }.keys.forEach {
            errors += "duplicate mapping for muscleGroupId: $it"
        }

        mappings.forEach { mapping ->
            if (mapping.muscleGroupId !in CANONICAL_MUSCLE_GROUP_IDS) {
                errors += "unknown muscleGroupId in action mapping: ${mapping.muscleGroupId}"
            }
            val primary = mapping.primaryExerciseIds.toSet()
            val secondary = mapping.secondaryExerciseIds.toSet()
            (primary intersect secondary).forEach { id ->
                errors += "exercise $id appears in both primary and secondary for ${mapping.muscleGroupId}"
            }
            validateExerciseIds(mapping.muscleGroupId, mapping.primaryExerciseIds, catalogById, errors, primaryRole = true)
            validateExerciseIds(mapping.muscleGroupId, mapping.secondaryExerciseIds, catalogById, errors, primaryRole = false)
        }

        return BodyMappingCheckReport(errors.isEmpty(), errors)
    }

    fun validate(
        regionsJson: String,
        mappingsJson: String,
        catalogExercises: List<CatalogExercise>,
        modelReport: BodyModelCheckReport,
        modelNodeIdsByGender: Map<BodyGender, Set<String>>,
    ): BodyMappingCheckReport = try {
        validate(
            regions = json.decodeFromString<List<MuscleRegion>>(regionsJson),
            mappings = json.decodeFromString<List<MuscleActionMapping>>(mappingsJson),
            catalogExercises = catalogExercises,
            modelReport = modelReport,
            modelNodeIdsByGender = modelNodeIdsByGender,
        )
    } catch (error: Exception) {
        BodyMappingCheckReport(false, listOf("mapping resource cannot be parsed: ${error.message ?: "unknown error"}"))
    }

    private fun validateExerciseIds(
        muscleGroupId: String,
        ids: List<String>,
        catalogById: Map<String, CatalogExercise>,
        errors: MutableList<String>,
        primaryRole: Boolean,
    ) {
        ids.forEach { id ->
            val exercise = catalogById[id]
            if (exercise == null) {
                errors += "unknown exercise ID: $id"
                return@forEach
            }
            if (primaryRole) {
                if (exercise.target !in PRIMARY_TARGETS[muscleGroupId].orEmpty()) {
                    errors += "exercise $id target ${exercise.target} does not match $muscleGroupId"
                }
            } else if (!exercise.secondaryMuscles.any { it in SECONDARY_MUSCLE_NAMES[muscleGroupId].orEmpty() }) {
                errors += "exercise $id secondary_muscles do not contain $muscleGroupId"
            }
        }
    }

    companion object {
        val CANONICAL_MUSCLE_GROUP_IDS = linkedSetOf(
            "chest", "shoulders", "back", "biceps", "triceps", "forearms",
            "abs", "glutes", "quadriceps", "hamstrings", "calves",
        )

        private val PRIMARY_TARGETS = mapOf(
            "chest" to setOf("pectorals"),
            "shoulders" to setOf("delts"),
            "back" to setOf("lats", "upper back", "spine"),
            "biceps" to setOf("biceps"),
            "triceps" to setOf("triceps"),
            "forearms" to setOf("forearms"),
            "abs" to setOf("abs"),
            "glutes" to setOf("glutes"),
            "quadriceps" to setOf("quads"),
            "hamstrings" to setOf("hamstrings"),
            "calves" to setOf("calves"),
        )

        private val SECONDARY_MUSCLE_NAMES = mapOf(
            "chest" to setOf("chest"),
            "shoulders" to setOf("shoulders", "deltoids", "deltoid", "upper back"),
            "back" to setOf("back", "lats", "upper back", "rhomboids", "traps", "trapezius"),
            "biceps" to setOf("biceps"),
            "triceps" to setOf("triceps"),
            "forearms" to setOf("forearms"),
            "abs" to setOf("abs", "core", "abdominals", "lower abs", "obliques"),
            "glutes" to setOf("glutes"),
            "quadriceps" to setOf("quadriceps", "quads"),
            "hamstrings" to setOf("hamstrings"),
            "calves" to setOf("calves"),
        )
    }
}
