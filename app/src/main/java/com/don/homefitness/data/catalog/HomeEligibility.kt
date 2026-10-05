package com.don.homefitness.data.catalog

fun CatalogExercise.isAvailableAtHome(availableEquipment: Set<String>): Boolean =
    homeEligible && requiredEquipment.all(availableEquipment::contains)

fun filterHomeExercises(
    exercises: List<CatalogExercise>,
    query: String,
    bodyPart: String?,
    equipment: String?,
    availableEquipment: Set<String>,
): List<CatalogExercise> {
    val normalizedQuery = query.trim().lowercase()
    return exercises.filter { exercise ->
        exercise.isAvailableAtHome(availableEquipment) &&
            (bodyPart == null || exercise.bodyPart == bodyPart) &&
            (equipment == null || exercise.equipment == equipment) &&
            (normalizedQuery.isEmpty() || listOf(exercise.nameZh, exercise.originalName)
                .plus(exercise.aliasesZh)
                .any { it.lowercase().contains(normalizedQuery) })
    }
}
