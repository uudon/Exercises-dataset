package com.don.homefitness

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.don.homefitness.data.catalog.CatalogExercise
import com.don.homefitness.feature.muscle.MuscleExercisePanel
import com.don.homefitness.feature.muscle.MusclePanelState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MuscleExercisePanelTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun actionClickUsesCatalogIdAndAddPlanUsesSameId() {
        var openedId: String? = null
        var addedId: String? = null
        composeRule.setContent {
            MaterialTheme {
                MuscleExercisePanel(
                    state = MusclePanelState(
                        muscleGroupId = "chest",
                        displayNameZh = "胸部",
                        primary = listOf(exercise("catalog-1", "卧推")),
                        secondary = listOf(exercise("catalog-2", "飞鸟")),
                    ),
                    onExerciseClick = { openedId = it },
                    onOpenCatalog = {},
                    onAddToPlan = { addedId = it },
                )
            }
        }

        composeRule.onNodeWithText("卧推").performClick()
        composeRule.onAllNodesWithText("加入计划")[0].performClick()

        assertEquals("catalog-1", openedId)
        assertEquals("catalog-1", addedId)
    }

    @Test
    fun blockedStateShowsReasonAndCatalogFallback() {
        var openedCatalog = false
        composeRule.setContent {
            MaterialTheme {
                MuscleExercisePanel(
                    state = MusclePanelState.blocked("chest", "胸部"),
                    onExerciseClick = {},
                    onOpenCatalog = { openedCatalog = true },
                )
            }
        }

        composeRule.onNodeWithText("胸部").assertIsDisplayed()
        composeRule.onNodeWithText("资源授权待确认").assertIsDisplayed()
        composeRule.onNodeWithText("打开动作库").performClick()
        assertTrue(openedCatalog)
    }

    private fun exercise(id: String, name: String) = CatalogExercise(
        id = id,
        originalName = id,
        nameZh = name,
        aliasesZh = emptyList(),
        bodyPart = "chest",
        equipment = "body weight",
        target = "pectorals",
        secondaryMuscles = emptyList(),
        instructionsZh = "说明",
        instructionStepsZh = listOf("步骤"),
        requiredEquipment = setOf("body weight"),
        homeEligible = true,
        reviewStatus = "manually-reviewed",
        sourceCommit = "test",
    )
}
