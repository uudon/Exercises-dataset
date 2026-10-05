package com.don.homefitness.feature.plan

import com.don.homefitness.core.model.SetMode

data class ReplacementDecision(val requiresTargetConfirmation: Boolean, val reason: String? = null)

fun replacementDecision(oldMode: SetMode, newMode: SetMode, confirmed: Boolean): ReplacementDecision {
    if (oldMode == newMode) return ReplacementDecision(false)
    return if (confirmed) ReplacementDecision(false) else ReplacementDecision(true, "动作记录模式变化，请重新确认训练目标")
}
