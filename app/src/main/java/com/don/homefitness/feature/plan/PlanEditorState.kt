package com.don.homefitness.feature.plan

import com.don.homefitness.core.model.PlanDraft

fun hasUnsavedChanges(initial: PlanDraft, current: PlanDraft): Boolean = initial != current
