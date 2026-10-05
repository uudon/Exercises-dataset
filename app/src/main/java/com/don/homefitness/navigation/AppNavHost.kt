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
import com.don.homefitness.feature.catalog.CatalogScreen
import com.don.homefitness.feature.catalog.CatalogViewModelFactory

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun AppNavHost(repository: CatalogRepository, mediaResolver: MediaResolver, planRepository: PlanRepository) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "catalog") {
        composable("catalog") {
            CatalogScreen(
                viewModel = viewModel(factory = CatalogViewModelFactory(repository)),
                mediaResolver = mediaResolver,
                onOpenPlans = { navController.navigate("plans") },
            )
        }
        composable("plans") {
            PlanScreen(
                planRepository = planRepository,
                catalogRepository = repository,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
