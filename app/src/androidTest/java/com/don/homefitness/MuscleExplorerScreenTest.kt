package com.don.homefitness

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.MaterialTheme
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.runtime.Composable
import com.don.homefitness.data.body.BodyGender
import com.don.homefitness.feature.muscle.CameraOrbit
import com.don.homefitness.feature.muscle.LocalGlbMuscleModelRenderer
import com.don.homefitness.feature.muscle.MuscleExplorerScreen
import com.don.homefitness.feature.muscle.MuscleExplorerViewModel
import com.don.homefitness.feature.muscle.MuscleModelLoadException
import com.don.homefitness.feature.muscle.MuscleModelRenderer
import com.don.homefitness.feature.muscle.SceneViewFilamentBackend
import com.don.homefitness.feature.muscle.loadMuscleRegionMap
import org.junit.Assert.assertEquals
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
        composeRule.onNodeWithText("请选择肌肉区域").assertIsNotDisplayed()
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

    @Test
    fun successfulRendererWiresViewportAndReset() {
        val backend = FakeBackend()
        val renderer = LocalGlbMuscleModelRenderer(
            assetPathByGender = mapOf(BodyGender.MALE to "body/male/body.glb"),
            regionMap = loadMuscleRegionMap(
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext,
            ),
            backend = backend,
        )
        setScreen(renderer = renderer, modelRenderer = renderer, initialModelError = null)

        composeRule.onNodeWithTag("fake viewport").assertIsDisplayed()
        composeRule.onNodeWithText("胸部").performClick()
        val restoreCountBeforeReset = backend.restoreCount
        composeRule.onNodeWithText("恢复视角").performClick()
        composeRule.waitForIdle()

        assertEquals(1, backend.resetCount)
        assertTrue(backend.restoreCount > restoreCountBeforeReset)
        assertEquals(CameraOrbit.DEFAULT, backend.lastRestoredCamera)
    }

    private fun setScreen(
        onOpenCatalog: () -> Unit = {},
        initialModelError: String? = "测试中禁用 3D 模型",
        renderer: MuscleModelRenderer = FakeRenderer(),
        modelRenderer: LocalGlbMuscleModelRenderer? = null,
    ) {
        val viewModel = MuscleExplorerViewModel(renderer)
        composeRule.setContent {
            MaterialTheme {
                MuscleExplorerScreen(
                    viewModel = viewModel,
                    onOpenCatalog = onOpenCatalog,
                    initialModelError = initialModelError,
                    modelRenderer = modelRenderer,
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

    private class FakeBackend : SceneViewFilamentBackend {
        var resetCount = 0
        var restoreCount = 0
        var lastRestoredCamera: CameraOrbit? = null

        override fun loadLocalGlb(
            assetPath: String,
            gender: BodyGender,
            nodeToRegion: Map<String, String>,
            nodeToMuscleGroup: Map<String, String>,
        ) = Unit

        override fun setHighlight(muscleGroupId: String?) = Unit
        override fun resetCamera() { resetCount++ }
        override fun restoreCamera(camera: CameraOrbit) {
            restoreCount++
            lastRestoredCamera = camera
        }
        override fun onResume() = Unit
        override fun onPause() = Unit
        override fun dispose() = Unit
        override fun setNodeHitListener(listener: (String) -> Unit) = Unit
        override fun setRegionHitListener(listener: (String) -> Unit) = Unit
        override fun setCameraListener(listener: (CameraOrbit) -> Unit) = Unit

        @Composable
        override fun Content(modifier: Modifier) {
            androidx.compose.foundation.layout.Box(modifier.testTag("fake viewport"))
        }
    }
}
