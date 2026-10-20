package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** Device preferences are deliberately separate from account workspaces and credentials. */
data class AppPreferences(
    val theme: String = "System",
    val accent: String = "Forest",
    val typography: String = "Modern",
    val fontScale: Float = 1f,
    val density: String = "Comfortable",
    val corners: String = "Rounded",
    val reduceMotion: Boolean = false,
    val motionSpeed: String = "Balanced",
    val haptics: Boolean = true,
    val startScreen: String = "HOME",
    val homeSpotlight: Boolean = true,
    val homeLibrary: Boolean = true,
    val homeProjects: Boolean = true,
    val homeHistory: Boolean = true,
    val cardAuthors: Boolean = true,
    val cardAbstract: Boolean = true,
    val cardMetadata: Boolean = true,
    val cardLines: Int = 3,
    val defaultCollection: String = "Reading list",
    val librarySort: String = "Recently saved",
    val searchKind: String = "PAPER",
    val searchProvider: String = "All sources",
    val searchYear: Int = 0,
    val searchPdfOnly: Boolean = false,
    val searchSort: String = "Relevance",
    val rememberHistory: Boolean = true,
    val readerTheme: String = "Original",
    val readerZoom: Float = 1f,
    val readerKeepAwake: Boolean = false,
    val readerRememberPage: Boolean = true,
    val readerDoubleTap: Boolean = true,
    val readerFocus: Boolean = false,
    val exportFormat: String = "BibTeX",
    val confirmRemoval: Boolean = true,
) {
    fun json() =
        JSONObject().apply {
            put("theme", theme)
            put("accent", accent)
            put("typography", typography)
            put("fontScale", fontScale)
            put("density", density)
            put("corners", corners)
            put("reduceMotion", reduceMotion)
            put("motionSpeed", motionSpeed)
            put("haptics", haptics)
            put("startScreen", startScreen)
            put("homeSpotlight", homeSpotlight)
            put("homeLibrary", homeLibrary)
            put("homeProjects", homeProjects)
            put("homeHistory", homeHistory)
            put("cardAuthors", cardAuthors)
            put("cardAbstract", cardAbstract)
            put("cardMetadata", cardMetadata)
            put("cardLines", cardLines)
            put("defaultCollection", defaultCollection)
            put("librarySort", librarySort)
            put("searchKind", searchKind)
            put("searchProvider", searchProvider)
            put("searchYear", searchYear)
            put("searchPdfOnly", searchPdfOnly)
            put("searchSort", searchSort)
            put("rememberHistory", rememberHistory)
            put("readerTheme", readerTheme)
            put("readerZoom", readerZoom)
            put("readerKeepAwake", readerKeepAwake)
            put("readerRememberPage", readerRememberPage)
            put("readerDoubleTap", readerDoubleTap)
            put("readerFocus", readerFocus)
            put("exportFormat", exportFormat)
            put("confirmRemoval", confirmRemoval)
        }

    companion object {
        val librarySorts = listOf("Recently saved", "Title A–Z", "Newest year", "Most cited")
        val searchSorts = listOf("Relevance", "Newest year", "Oldest year", "Most cited")

        fun from(j: JSONObject): AppPreferences {
            fun choice(key: String, values: List<String>, fallback: String) =
                j.optString(key, fallback).takeIf { it in values } ?: fallback
            fun number(key: String, fallback: Float, min: Float, max: Float) =
                j.optDouble(key, fallback.toDouble()).toFloat().let {
                    if (it.isFinite()) it.coerceIn(min, max) else fallback
                }
            return AppPreferences(
                theme = choice("theme", listOf("System", "Light", "Dark"), "System"),
                accent =
                    choice(
                        "accent",
                        listOf("Forest", "Ocean", "Indigo", "Rose", "Amber"),
                        "Forest",
                    ),
                typography = choice("typography", listOf("Modern", "Editorial"), "Modern"),
                fontScale = number("fontScale", 1f, .85f, 1.3f),
                density = choice("density", listOf("Comfortable", "Compact"), "Comfortable"),
                corners = choice("corners", listOf("Rounded", "Soft", "Crisp"), "Rounded"),
                reduceMotion = j.optBoolean("reduceMotion"),
                motionSpeed =
                    choice("motionSpeed", listOf("Quick", "Balanced", "Relaxed"), "Balanced"),
                haptics = j.optBoolean("haptics", true),
                startScreen =
                    choice("startScreen", listOf("HOME", "SEARCH", "LIBRARY", "PROJECTS"), "HOME"),
                homeSpotlight = j.optBoolean("homeSpotlight", true),
                homeLibrary = j.optBoolean("homeLibrary", true),
                homeProjects = j.optBoolean("homeProjects", true),
                homeHistory = j.optBoolean("homeHistory", true),
                cardAuthors = j.optBoolean("cardAuthors", true),
                cardAbstract = j.optBoolean("cardAbstract", true),
                cardMetadata = j.optBoolean("cardMetadata", true),
                cardLines = j.optInt("cardLines", 3).coerceIn(1, 5),
                defaultCollection =
                    j.optString("defaultCollection", "Reading list").trim().take(80).ifBlank {
                        "Reading list"
                    },
                librarySort = choice("librarySort", librarySorts, "Recently saved"),
                searchKind = choice("searchKind", ItemKind.entries.map { it.name }, "PAPER"),
                searchProvider =
                    choice(
                        "searchProvider",
                        listOf("All sources", "OpenAlex", "Crossref", "arXiv"),
                        "All sources",
                    ),
                searchYear = j.optInt("searchYear", 0).takeIf { it == 0 || it in 1500..2100 } ?: 0,
                searchPdfOnly = j.optBoolean("searchPdfOnly"),
                searchSort = choice("searchSort", searchSorts, "Relevance"),
                rememberHistory = j.optBoolean("rememberHistory", true),
                readerTheme =
                    choice("readerTheme", listOf("Original", "Sepia", "Night"), "Original"),
                readerZoom = number("readerZoom", 1f, 1f, 2f),
                readerKeepAwake = j.optBoolean("readerKeepAwake"),
                readerRememberPage = j.optBoolean("readerRememberPage", true),
                readerDoubleTap = j.optBoolean("readerDoubleTap", true),
                readerFocus = j.optBoolean("readerFocus"),
                exportFormat =
                    choice("exportFormat", listOf("BibTeX", "RIS", "Markdown"), "BibTeX"),
                confirmRemoval = j.optBoolean("confirmRemoval", true),
            )
        }
    }
}

class PreferencesStore(context: Context) {
    private val disk = context.getSharedPreferences("overthink_preferences", Context.MODE_PRIVATE)
    private val state =
        MutableStateFlow(
            runCatching {
                    AppPreferences.from(JSONObject(disk.getString("customization", null) ?: "{}"))
                }
                .getOrDefault(AppPreferences())
                .let {
                    if (disk.contains("customization")) it
                    else
                        AppPreferences.from(
                            it.json().put("theme", disk.getString("appearance", "System"))
                        )
                }
        )
    val preferences = state.asStateFlow()

    @Synchronized
    fun update(change: (AppPreferences) -> AppPreferences) {
        val next = AppPreferences.from(change(state.value).json())
        disk.edit().putString("customization", next.json().toString()).apply()
        state.value = next
    }

    fun reset() = update { AppPreferences() }
}

fun List<ResearchItem>.sortedForLibrary(order: String): List<ResearchItem> =
    when (order) {
        "Title A–Z" -> sortedBy { it.title.lowercase(java.util.Locale.ROOT) }
        "Newest year" ->
            sortedWith(compareByDescending<ResearchItem> { it.year ?: 0 }.thenBy { it.title })
        "Most cited" ->
            sortedWith(compareByDescending<ResearchItem> { it.citations ?: -1 }.thenBy { it.title })
        else -> sortedByDescending { it.savedAt }
    }

fun List<ResearchItem>.sortedForSearch(order: String): List<ResearchItem> =
    when (order) {
        "Newest year",
        "Most cited" -> sortedForLibrary(order)
        "Oldest year" -> sortedBy { it.year ?: Int.MAX_VALUE }
        else -> this
    }
