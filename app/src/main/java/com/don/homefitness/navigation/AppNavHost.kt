package com.don.homefitness.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.don.homefitness.data.catalog.CatalogRepository
import com.don.homefitness.data.catalog.MediaResolver
import com.don.homefitness.feature.plan.PlanRepository
import com.don.homefitness.feature.plan.PlanScreen
import com.don.homefitness.feature.training.TrainingRepository
import com.don.homefitness.feature.training.TrainingScreen
import com.don.homefitness.feature.history.HistoryRepository
import com.don.homefitness.feature.history.HistoryScreen
import kotlinx.coroutines.launch
import com.don.homefitness.feature.catalog.CatalogScreen
import com.don.homefitness.feature.catalog.CatalogViewModelFactory
import com.don.homefitness.feature.muscle.MuscleExplorerScreen
import com.don.homefitness.feature.muscle.MuscleExplorerViewModel
import com.don.homefitness.feature.muscle.MuscleExplorerViewModelFactory
import com.don.homefitness.feature.muscle.UnavailableMuscleModelRenderer
import com.don.homefitness.feature.muscle.rememberProductionMuscleModelRenderer

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun AppNavHost(repository: CatalogRepository, mediaResolver: MediaResolver, planRepository: PlanRepository, trainingRepository: TrainingRepository, historyRepository: HistoryRepository) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "muscle") {
        composable("muscle") {
            val rendererState = rememberProductionMuscleModelRenderer()
            val renderer = rendererState.renderer ?: remember(rendererState.errorMessage) {
                UnavailableMuscleModelRenderer(rendererState.errorMessage ?: "模型资源初始化失败")
            }
            val muscleViewModel: MuscleExplorerViewModel = viewModel(
                factory = MuscleExplorerViewModelFactory(renderer),
            )
            MuscleExplorerScreen(
                viewModel = muscleViewModel,
                modelRenderer = rendererState.renderer,
                initialModelError = rendererState.errorMessage,
                onOpenCatalog = { navController.navigate("catalog") },
                onOpenExercise = { exerciseId -> navController.navigate("catalog?exerciseId=$exerciseId") },
                onAddToPlan = { exerciseId -> navController.navigate("plans?exerciseId=$exerciseId") },
            )
        }
        composable(
            route = "catalog?exerciseId={exerciseId}",
            arguments = listOf(navArgument("exerciseId") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { entry ->
            CatalogScreen(
                viewModel = viewModel(factory = CatalogViewModelFactory(repository)),
                mediaResolver = mediaResolver,
                onOpenPlans = { navController.navigate("plans") },
                onOpenHistory = { navController.navigate("history") },
                initialExerciseId = entry.arguments?.getString("exerciseId"),
                onAddToPlan = { exerciseId -> navController.navigate("plans?exerciseId=$exerciseId") },
            )
        }
        composable(
            route = "plans?exerciseId={exerciseId}",
            arguments = listOf(navArgument("exerciseId") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { entry ->
            PlanScreen(
                planRepository = planRepository,
                catalogRepository = repository,
                onStart = { planId ->
                    trainingRepository.startSessionAsync(planId) { sessionId -> navController.navigate("training/$sessionId") }
                },
                onBack = { navController.popBackStack() },
                initialExerciseId = entry.arguments?.getString("exerciseId"),
            )
        }
        composable("training/{sessionId}") { entry ->
            TrainingScreen(trainingRepository, entry.arguments?.getString("sessionId").orEmpty()) { navController.popBackStack() }
        }
        composable("history") { HistoryScreen(historyRepository) { navController.popBackStack() } }
    }
}

private fun TrainingRepository.startSessionAsync(planId: String, onSuccess: (String) -> Unit) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate).launch {
        runCatching { startSession(planId) }.onSuccess(onSuccess)
    }
}
