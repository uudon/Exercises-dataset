package com.don.homefitness

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.don.homefitness.data.body.BodyGender
import com.don.homefitness.feature.muscle.CameraOrbit
import com.don.homefitness.feature.muscle.MuscleExplorerScreen
import com.don.homefitness.feature.muscle.MuscleExplorerViewModel
import com.don.homefitness.feature.muscle.MuscleModelRenderer
import com.don.homefitness.feature.muscle.MuscleModelLoadException
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MuscleExplorerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun defaultHomeShowsExplorerAndChineseMuscleList() {
        setScreen()

        composeRule.onNodeWithText("3D 人体探索").assertIsDisplayed()
        composeRule.onNodeWithText("胸部").assertIsDisplayed()
        composeRule.onNodeWithText("请选择肌肉区域").assertIsDisplayed()
    }

    @Test
    fun genderSwitchPreservesSelectedMuscle() {
        setScreen()

        composeRule.onNodeWithText("胸部").performClick()
        composeRule.onNodeWithContentDescription("切换女性模型").performClick()

        composeRule.onNodeWithText("胸部").assertIsDisplayed()
        composeRule.onNodeWithText("请选择肌肉区域").assertDoesNotExist()
    }

    @Test
    fun catalogIsASecondaryEntry() {
        var opened = false
        setScreen(onOpenCatalog = { opened = true })

        composeRule.onNodeWithContentDescription("打开动作库").performClick()

        assertTrue(opened)
    }

    @Test
    fun modelFailureStillShowsFallbackList() {
        setScreen(initialModelError = "本地模型缺失")

        composeRule.onNodeWithText("3D 模型暂时不可用").assertIsDisplayed()
        composeRule.onNodeWithText("腹部").assertIsDisplayed()
    }

    private fun setScreen(
        onOpenCatalog: () -> Unit = {},
        initialModelError: String? = "测试中禁用 3D 模型",
    ) {
        val viewModel = MuscleExplorerViewModel(FakeRenderer())
        composeRule.setContent {
            MaterialTheme {
                MuscleExplorerScreen(
                    viewModel = viewModel,
                    onOpenCatalog = onOpenCatalog,
                    initialModelError = initialModelError,
                )
            }
        }
    }

    private class FakeRenderer : MuscleModelRenderer {
        override var onRegionHit: ((String) -> Unit)? = null
        override var onNodeHit: ((String) -> Unit)? = null
        override var onCameraChanged: ((CameraOrbit) -> Unit)? = null

        override fun load(gender: BodyGender) = throw MuscleModelLoadException("test")
        override fun setHighlight(muscleGroupId: String?) = Unit
        override fun resetCamera() = Unit
        override fun onResume() = Unit
        override fun onPause() = Unit
        override fun dispose() = Unit
    }
}
