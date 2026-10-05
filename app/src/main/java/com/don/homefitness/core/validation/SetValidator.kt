package com.don.homefitness.core.validation

import com.don.homefitness.core.model.PlanDraft
import com.don.homefitness.core.model.PlannedSetDraft
import com.don.homefitness.core.model.SetMode
import java.math.BigDecimal
import java.math.RoundingMode

data class ValidationResult(val errors: List<String> = emptyList()) {
    val isValid: Boolean get() = errors.isEmpty()
}

object SetValidator {
    fun validatePlan(draft: PlanDraft): ValidationResult {
        val name = draft.name.trim()
        if (name.length !in 1..40) return ValidationResult(listOf("计划名称需为 1—40 个字符"))
        if (draft.exercises.isEmpty()) return ValidationResult(listOf("计划至少需要一个动作"))
        if (draft.exercises.any { it.sets.isEmpty() }) return ValidationResult(listOf("每个动作至少需要一组"))
        val errors = draft.exercises.flatMap { exercise ->
            exercise.sets.flatMap { set -> validate(set).errors }
        }
        return ValidationResult(errors)
    }

    fun validate(set: PlannedSetDraft): ValidationResult = ValidationResult(
        buildList {
            when (set.mode) {
                SetMode.REPS -> {
                    if (set.targetReps !in 1..999) add("次数需为 1—999")
                    if (set.targetSeconds != null) add("次数模式不能填写时长")
                }
                SetMode.DURATION -> {
                    if (set.targetSeconds !in 1..7200) add("时长需为 1—7200 秒")
                    if (set.targetReps != null) add("时长模式不能填写次数")
                }
            }
            set.targetWeightKg?.let { value ->
                val weight = value.toBigDecimalOrNull()
                if (weight == null || weight < BigDecimal.ZERO || weight > BigDecimal("200") ||
                    weight.scale() > 2 || weight.stripTrailingZeros().scale() > 2
                ) {
                    add("单只哑铃重量需为 0—200 kg，最多两位小数")
                }
            }
            if (set.restSeconds !in 0..600) add("休息时间需为 0—600 秒")
        },
    )

    fun weightToGrams(weightKg: String?): Long? = weightKg?.toBigDecimalOrNull()
        ?.setScale(2, RoundingMode.UNNECESSARY)
        ?.movePointRight(3)
        ?.longValueExact()
}
