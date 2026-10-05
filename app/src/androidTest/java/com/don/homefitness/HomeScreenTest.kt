package com.don.homefitness

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeScreenShowsWelcomeContent() {
        composeRule.setContent { HomeScreen() }

        composeRule.onNodeWithText("家庭健身").assertIsDisplayed()
        composeRule.onNodeWithText("离线动作库即将就绪").assertIsDisplayed()
    }
}
