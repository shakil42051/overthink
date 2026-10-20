package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CustomizationTest {
    @Test
    fun yearFiltersAreExplicitAndOnlyApplyToPapers() {
        val historical = ResearchItem("old", "Historical source", year = 1850)
        val dated =
            ResearchItem("new", "New source", year = 2025, pdfUrl = "https://example.org/p.pdf")
        val undated = ResearchItem("unknown", "Undated source")
        assertTrue(SearchFilter().accepts(historical))
        assertTrue(SearchFilter().accepts(undated))
        assertFalse(SearchFilter(fromYear = 1900).accepts(historical))
        assertFalse(SearchFilter(fromYear = 1900).accepts(undated))
        assertTrue(SearchFilter(fromYear = 1900, pdfOnly = true).accepts(dated))
        assertTrue(
            SearchFilter(fromYear = 2025, pdfOnly = true)
                .accepts(historical.copy(kind = ItemKind.DATASET))
        )
    }

    @Test
    fun exportSurvivesPickerRecreation() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val original = PendingExport(app)
        original.prepare("Research export with a বাংলা title")
        val restored = PendingExport(app)
        val out = java.io.ByteArrayOutputStream()
        restored.copyTo(out)
        assertEquals("Research export with a বাংলা title", out.toString("UTF-8"))
        restored.clear()
        assertThrows(java.io.FileNotFoundException::class.java) { restored.copyTo(out) }
    }

    @Test
    fun oldThemeMigratesAndPreferencesSurviveReopening() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val disk = app.getSharedPreferences("overthink_preferences", 0)
        disk
            .edit()
            .clear()
            .putString("appearance", "Dark")
            .putString("server", "https://example.org")
            .commit()
        val first = PreferencesStore(app)
        assertEquals("Dark", first.preferences.value.theme)
        first.update {
            it.copy(
                accent = "Rose",
                defaultCollection = "Thesis",
                readerTheme = "Sepia",
                readerZoom = 1.5f,
                rememberHistory = false,
            )
        }
        val reopened = PreferencesStore(app)
        assertEquals(first.preferences.value, reopened.preferences.value)
        reopened.reset()
        assertEquals(AppPreferences(), reopened.preferences.value)
        assertEquals("https://example.org", disk.getString("server", null))
    }

    @Test
    fun malformedPreferencesCannotCreateUnsupportedOrUnusableUi() {
        val p =
            AppPreferences.from(
                JSONObject()
                    .put("theme", "unknown")
                    .put("accent", "missing")
                    .put("startScreen", "SETTINGS")
                    .put("fontScale", 100)
                    .put("readerZoom", -10)
                    .put("cardLines", 400)
                    .put("searchYear", -100)
                    .put("defaultCollection", "   ")
            )
        assertEquals("System", p.theme)
        assertEquals("Forest", p.accent)
        assertEquals("HOME", p.startScreen)
        assertEquals(1.3f, p.fontScale)
        assertEquals(1f, p.readerZoom)
        assertEquals(5, p.cardLines)
        assertEquals(0, p.searchYear)
        assertEquals("Reading list", p.defaultCollection)
    }

    @Test
    fun organizationMetadataRoundTripsAndOldBackupsStillLoad() {
        val item =
            ResearchItem(
                "a",
                "A paper",
                favorite = true,
                readingStatus = ReadingStatus.READING,
                tags = listOf("thesis", "methods"),
            )
        assertEquals(item, ResearchItem.from(item.json()))
        val old =
            item.json().apply {
                remove("favorite")
                remove("readingStatus")
                remove("tags")
            }
        val migrated = ResearchItem.from(old)
        assertFalse(migrated.favorite)
        assertEquals(ReadingStatus.TO_READ, migrated.readingStatus)
        assertTrue(migrated.tags.isEmpty())
    }

    @Test
    fun sortingKeepsUnknownYearsAndCitationCountsAtTheEnd() {
        val a = ResearchItem("a", "Zebra", year = 2020, citations = 20, savedAt = 2)
        val b = ResearchItem("b", "Apple", year = 2025, citations = 1, savedAt = 1)
        val c = ResearchItem("c", "Unknown", savedAt = 3)
        val list = listOf(c, a, b)
        assertEquals(listOf(b, a, c), list.sortedForLibrary("Newest year"))
        assertEquals(listOf(a, b, c), list.sortedForSearch("Most cited"))
        assertEquals(listOf(a, b, c), list.sortedForSearch("Oldest year"))
        assertEquals(list, list.sortedForSearch("Relevance"))
    }

    @Test
    fun exportsUseRealMetadataAndDoNotAllowRisRecordInjection() {
        val p =
            ResearchItem(
                "p",
                "Actual title\nER  -",
                authors = "Researcher",
                year = 2025,
                doi = "10.1/example",
                url = "https://example.org/p",
                tags = listOf("methods"),
            )
        val ris = listOf(p).export("RIS")
        assertEquals(1, ris.lines().count { it == "ER  -" })
        assertTrue(ris.contains("DO  - 10.1/example"))
        val markdown = listOf(p).export("Markdown")
        assertTrue(markdown.contains("Tags: methods"))
        assertTrue(markdown.contains("<https://example.org/p>"))
        assertTrue(listOf(p).export("BibTeX").startsWith("@misc"))
    }
}
