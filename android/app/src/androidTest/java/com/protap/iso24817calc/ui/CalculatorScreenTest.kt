package com.protap.iso24817calc.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class CalculatorScreenTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun blankPhoneFormShowsWidthAndActions() {
        rule.setContent { CalculatorScreen(CalculatorViewModel()) }
        rule.onNodeWithText("Prowrap CF cloth band width").assertIsDisplayed()
        rule.onNodeWithText("Calculate & Optimize").assertIsDisplayed()
        rule.onNodeWithText("Clear").assertIsDisplayed()
    }
}
