package com.don.homefitness.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun AppNavHost(repository: CatalogRepository, mediaResolver: MediaResolver, planRepository: PlanRepository, trainingRepository: TrainingRepository, historyRepository: HistoryRepository) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "catalog") {
        composable("catalog") {
            CatalogScreen(
                viewModel = viewModel(factory = CatalogViewModelFactory(repository)),
                mediaResolver = mediaResolver,
                onOpenPlans = { navController.navigate("plans") },
                onOpenHistory = { navController.navigate("history") },
            )
        }
        composable("plans") {
            PlanScreen(
                planRepository = planRepository,
                catalogRepository = repository,
                onStart = { planId ->
                    trainingRepository.startSessionAsync(planId) { sessionId -> navController.navigate("training/$sessionId") }
                },
                onBack = { navController.popBackStack() },
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
