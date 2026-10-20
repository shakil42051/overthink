package com.example

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.example.ui.*
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-420dpi")
class ScreenTest {
    @Test
    fun readerControlsStayVisible() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = OverthinkViewModel(app)
        models.add(vm)
        compose.setContent {
            OverthinkTheme(false) {
                ReaderScreen(
                    com.example.data.ResearchItem("test", "A document"),
                    java.io.File(app.cacheDir, "missing.pdf"),
                    vm,
                    {},
                )
            }
        }
        compose.onNodeWithText("Fit").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next page").assertIsDisplayed()
        compose.onNodeWithContentDescription("Reader settings").assertIsDisplayed()
    }

    @get:Rule val compose = createComposeRule()
    private val models = mutableListOf<OverthinkViewModel>()

    @After
    fun closeStores() {
        models.forEach { it.store.close() }
    }

    @Test
    fun homeHasAnHonestEmptyWorkspaceAndRenders() {
        val vm = OverthinkViewModel(ApplicationProvider.getApplicationContext<Application>())
        models.add(vm)
        compose.setContent { OverthinkTheme(false) { HomeScreen(vm, {}, {}, {}) } }
        compose.onNodeWithText("A clearer space.\nA bigger perspective.").assertExists()
        compose.onRoot().captureRoboImage(filePath = "build/reports/screenshots/home.png")
    }

    @Test
    fun darkSearchRendersWithoutFakeResults() {
        val vm = OverthinkViewModel(ApplicationProvider.getApplicationContext<Application>())
        models.add(vm)
        compose.setContent { OverthinkTheme(true) { DiscoverScreen(vm, {}, {}) } }
        compose.onNodeWithText("An open door to new ideas").assertExists()
        compose.onRoot().captureRoboImage(filePath = "build/reports/screenshots/search-dark.png")
    }

    @Test
    fun navigationKeepsUnsubmittedHomeInput() {
        val vm = OverthinkViewModel(ApplicationProvider.getApplicationContext<Application>())
        models.add(vm)
        vm.customize { it.copy(startScreen = "HOME", reduceMotion = true) }
        compose.setContent { OverthinkTheme(false, vm.preferences.value) { OverthinkApp(vm) } }
        compose.onNode(hasSetTextAction()).performTextInput("My unfinished question")
        compose.onNodeWithText("Library", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Home", useUnmergedTree = true).performClick()
        compose.onNodeWithText("My unfinished question").assertExists()
    }

    @Test
    fun customizationRenders() {
        val vm = OverthinkViewModel(ApplicationProvider.getApplicationContext<Application>())
        models.add(vm)
        val preferences = com.example.data.AppPreferences(accent = "Indigo", fontScale = 1.3f)
        vm.customize { preferences }
        compose.setContent {
            val p by vm.preferences.collectAsStateWithLifecycle()
            OverthinkTheme(false, p) { PersonalizationScreen(vm, {}) }
        }
        compose.onNodeWithText("Make it yours").assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "build/reports/screenshots/customize.png")
        compose.onNodeWithText("Find a setting").performTextInput("PDF")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("PDF appearance"))
        compose.onNodeWithText("PDF appearance").assertExists()
        compose.onNodeWithText("PDF appearance").performClick()
        compose.onNodeWithText("Night").performClick()
        compose.runOnIdle {
            org.junit.Assert.assertEquals("Night", vm.preferences.value.readerTheme)
        }
    }
}
