package com.don.homefitness.data.catalog

import androidx.room.withTransaction
import com.don.homefitness.data.db.FitnessDatabase
import com.don.homefitness.data.db.entity.ExerciseEntity
import com.don.homefitness.data.db.entity.FavoriteEntity
import com.don.homefitness.core.model.TrainingLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class CatalogRepository(
    private val database: FitnessDatabase,
    private val importer: CatalogImporter,
) {
    private val dao = database.exerciseDao()

    suspend fun importCatalog(json: String, overlay: String): ImportResult {
        val parsed = try {
            importer.parse(json, overlay)
        } catch (error: CatalogImportException) {
            return ImportResult(
                acceptedCount = 0,
                rejectedCount = 1,
                errors = listOf(ImportError(null, error.message ?: "动作目录校验失败")),
            )
        } catch (error: Exception) {
            return ImportResult(
                acceptedCount = 0,
                rejectedCount = 1,
                errors = listOf(ImportError(null, "动作目录无法解析：${error.message ?: "未知错误"}")),
            )
        }

        database.withTransaction {
            dao.deleteAllExercises()
            dao.replaceExercises(parsed.map(::toEntity))
        }
        return ImportResult(parsed.size, 0, emptyList())
    }

    fun observeExercises(
        query: String,
        bodyPart: String?,
        equipment: String?,
        availableEquipment: Set<String>,
        location: TrainingLocation = TrainingLocation.HOME,
    ): Flow<List<CatalogExercise>> =
        combine(dao.observeExercises(), dao.observeFavoriteIds()) { entities, favoriteIds ->
            filterExercisesAtLocation(
                exercises = entities.map { it.toModel(favoriteIds.contains(it.id)) },
                location = location,
                query = query,
                bodyPart = bodyPart,
                equipment = equipment,
                availableEquipment = availableEquipment,
            )
        }

    suspend fun setFavorite(exerciseId: String, favorite: Boolean) {
        if (favorite) dao.addFavorite(FavoriteEntity(exerciseId)) else dao.removeFavorite(exerciseId)
    }

    private fun toEntity(exercise: CatalogExercise): ExerciseEntity = ExerciseEntity(
        id = exercise.id,
        originalName = exercise.originalName,
        nameZh = exercise.nameZh,
        aliasesZh = exercise.aliasesZh.joinToString(DELIMITER),
        bodyPart = exercise.bodyPart,
        equipment = exercise.equipment,
        target = exercise.target,
        secondaryMuscles = exercise.secondaryMuscles.joinToString(DELIMITER),
        instructionsZh = exercise.instructionsZh,
        instructionStepsZh = exercise.instructionStepsZh.joinToString(DELIMITER),
        requiredEquipment = exercise.requiredEquipment.joinToString(DELIMITER),
        homeEligible = exercise.homeEligible,
        reviewStatus = exercise.reviewStatus,
        sourceCommit = exercise.sourceCommit,
    )

    private fun ExerciseEntity.toModel(favorite: Boolean): CatalogExercise = CatalogExercise(
        id = id,
        originalName = originalName,
        nameZh = nameZh,
        aliasesZh = aliasesZh.splitValues(),
        bodyPart = bodyPart,
        equipment = equipment,
        target = target,
        secondaryMuscles = secondaryMuscles.splitValues(),
        instructionsZh = instructionsZh,
        instructionStepsZh = instructionStepsZh.splitValues(),
        requiredEquipment = requiredEquipment.splitValues().toSet(),
        homeEligible = homeEligible,
        reviewStatus = reviewStatus,
        sourceCommit = sourceCommit,
        isFavorite = favorite,
    )

    private fun String.splitValues(): List<String> =
        if (isEmpty()) emptyList() else split(DELIMITER)

    companion object {
        private const val DELIMITER = "\u001F"
    }
}
