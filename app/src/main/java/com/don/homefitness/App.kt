package com.don.homefitness

import android.app.Application
import androidx.room.Room
import com.don.homefitness.data.catalog.CatalogImporter
import com.don.homefitness.data.catalog.CatalogRepository
import com.don.homefitness.data.db.FitnessDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {
    val database: FitnessDatabase by lazy {
        Room.databaseBuilder(this, FitnessDatabase::class.java, "home-fitness.db").build()
    }

    val catalogRepository: CatalogRepository by lazy {
        CatalogRepository(database, CatalogImporter(sourceCommit = SOURCE_COMMIT))
    }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val source = assets.open("catalog/exercises.json").bufferedReader().use { it.readText() }
            val overlay = assets.open("catalog/exercise-overlay.json").bufferedReader().use { it.readText() }
            catalogRepository.importCatalog(source, overlay)
        }
    }

    companion object {
        const val SOURCE_COMMIT = "7455efae41b330c265e7cd4b78dfa848e7ce5ebd"
    }
}
