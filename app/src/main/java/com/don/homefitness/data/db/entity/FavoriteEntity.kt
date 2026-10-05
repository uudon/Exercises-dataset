package com.don.homefitness.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_favorites")
data class FavoriteEntity(@PrimaryKey val exerciseId: String)
