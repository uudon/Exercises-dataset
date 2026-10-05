package com.don.homefitness.core.model

import java.math.BigDecimal
import java.math.RoundingMode

enum class WeightUnit { KG, LB }

object WeightConversion {
    fun toGrams(value: BigDecimal, unit: WeightUnit): Long = when (unit) {
        WeightUnit.KG -> value.multiply(BigDecimal(1000)).setScale(0, RoundingMode.HALF_UP).longValueExact()
        WeightUnit.LB -> value.multiply(BigDecimal("453.59237")).setScale(0, RoundingMode.HALF_UP).longValueExact()
    }

    fun fromGrams(grams: Long, unit: WeightUnit): BigDecimal = when (unit) {
        WeightUnit.KG -> BigDecimal(grams).divide(BigDecimal(1000), 2, RoundingMode.HALF_UP)
        WeightUnit.LB -> BigDecimal(grams).divide(BigDecimal("453.59237"), 2, RoundingMode.HALF_UP)
    }
}
