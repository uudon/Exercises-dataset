package com.don.homefitness.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.don.homefitness.data.catalog.CatalogRepository
import com.don.homefitness.feature.catalog.CatalogScreen
import com.don.homefitness.feature.catalog.CatalogViewModelFactory

@Composable
fun AppNavHost(repository: CatalogRepository) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "catalog") {
        composable("catalog") {
            CatalogScreen(viewModel(factory = CatalogViewModelFactory(repository)))
        }
    }
}
