package com.don.homefitness.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class WeightConversionTest {
    @Test fun `one pound rounds to 454 grams`() { assertEquals(454L, WeightConversion.toGrams(BigDecimal.ONE, WeightUnit.LB)) }
    @Test fun `kg round trip keeps stored grams`() {
        val grams = WeightConversion.toGrams(BigDecimal("12.50"), WeightUnit.KG)
        assertEquals(grams, WeightConversion.toGrams(WeightConversion.fromGrams(grams, WeightUnit.KG), WeightUnit.KG))
    }
}
