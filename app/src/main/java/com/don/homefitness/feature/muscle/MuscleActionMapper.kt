package com.don.homefitness.feature.muscle

import com.don.homefitness.core.model.TrainingLocation
import com.don.homefitness.data.body.MuscleActionMapping
import com.don.homefitness.data.catalog.CatalogExercise
import com.don.homefitness.data.catalog.filterExercisesAtLocation

data class MuscleActionGroups(
    val primary: List<CatalogExercise>,
    val secondary: List<CatalogExercise>,
)

class MuscleActionMapper(
    private val exercises: List<CatalogExercise>,
    private val mappings: List<MuscleActionMapping>,
) {
    fun actionsFor(
        muscleGroupId: String,
        location: TrainingLocation,
        availableEquipment: Set<String>,
    ): MuscleActionGroups {
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
            .associateBy(CatalogExercise::id)
        return MuscleActionGroups(
            primary = mapping.primaryExerciseIds.mapNotNull(available::get),
            secondary = mapping.secondaryExerciseIds.mapNotNull(available::get),
        )
    }

    private companion object {
        val HOME_EQUIPMENT = setOf("body weight", "dumbbell")
    }
}
