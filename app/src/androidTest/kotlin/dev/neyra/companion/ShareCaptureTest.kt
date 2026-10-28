package dev.neyra.companion

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ShareCaptureTest {
    private val activityRule = ActivityScenarioRule<MainActivity>(
        Intent(ApplicationProvider.getApplicationContext<Context>(), MainActivity::class.java)
            .setAction(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "Synthetic shared launch note"))
    @get:Rule val compose = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>(activityRule) { rule ->
        lateinit var activity: MainActivity
        rule.scenario.onActivity { activity = it }
        activity
    }
    @Test fun sharingOpensAReviewDraftAndRecreationPreservesEditsWithoutSending() {
        compose.waitUntil(5000) { compose.onAllNodesWithText("Synthetic shared launch note").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasSetTextAction()).performTextReplacement("Synthetic shared launch note edited")
        compose.onNodeWithText("Notes").performClick()
        activityRule.scenario.recreate()
        compose.onNodeWithText("Worth keeping.").assertExists()
        compose.onNodeWithText("Capture").performClick()
        compose.onNodeWithText("Synthetic shared launch note edited").assertExists()
        val app = compose.activity.application as NeyraApplication
        assertTrue(app.database.queue().isEmpty())
    }
}
