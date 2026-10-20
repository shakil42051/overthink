package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import kotlin.math.roundToInt

@Composable
fun PersonalizationScreen(vm: OverthinkViewModel, onBack: () -> Unit) {
    val p by vm.preferences.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableStateOf("Style") }
    var query by rememberSaveable { mutableStateOf("") }
    var reset by remember { mutableStateOf(false) }
    fun visible(group: String, keywords: String) =
        if (query.isBlank()) category == group else "$group $keywords".contains(query.trim(), true)
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(p.gutter),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back to settings")
                }
                Column(Modifier.weight(1f)) {
                    Text("Make it yours", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "Your workspace. Your way.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Eyebrow("Live preview")
                    Text(
                        "A fresh perspective",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "Changes apply instantly and stay on this device.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tag(p.accent)
                        Tag(p.typography)
                        Tag(p.density)
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                query,
                { query = it },
                Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Find a setting") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty())
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Outlined.Close, "Clear settings search")
                        }
                },
            )
        }
        if (query.isBlank())
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Style", "Home", "Discovery", "Library", "Reader", "Privacy")) {
                        group ->
                        FilterChip(
                            selected = category == group,
                            onClick = { category = group },
                            label = { Text(group) },
                        )
                    }
                }
            }
        if (
            visible(
                "Style",
                "theme light dark system accent color typography font text size spacing density corners animation motion haptics",
            )
        )
            item {
                PaperSurface {
                    Eyebrow("Look & feel")
                    ChoiceSetting("Theme", p.theme, listOf("System", "Light", "Dark")) { value ->
                        vm.customize { it.copy(theme = value) }
                    }
                    Text("Accent color", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("Forest", "Ocean", "Indigo", "Rose", "Amber").forEach { accent ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier =
                                    Modifier.clickable { vm.customize { it.copy(accent = accent) } }
                                        .padding(vertical = 8.dp),
                            ) {
                                Box(
                                    Modifier.size(40.dp)
                                        .clip(CircleShape)
                                        .background(accentColors(accent).first),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (accent == p.accent)
                                        Icon(
                                            Icons.Outlined.Check,
                                            "Selected $accent",
                                            tint = androidx.compose.ui.graphics.Color.White,
                                        )
                                }
                                Text(
                                    accent,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                    }
                    ChoiceSetting("Typography", p.typography, listOf("Modern", "Editorial")) { value
                        ->
                        vm.customize { it.copy(typography = value) }
                    }
                    SliderSetting(
                        "Text size",
                        "${(p.fontScale * 100).roundToInt()}% · adds to your system text size",
                        p.fontScale,
                        .85f..1.3f,
                        8,
                    ) { value ->
                        vm.customize { it.copy(fontScale = value) }
                    }
                    ChoiceSetting("Content spacing", p.density, listOf("Comfortable", "Compact")) {
                        value ->
                        vm.customize { it.copy(density = value) }
                    }
                    ChoiceSetting("Card corners", p.corners, listOf("Rounded", "Soft", "Crisp")) {
                        value ->
                        vm.customize { it.copy(corners = value) }
                    }
                    ToggleSetting("Reduce motion", "Use instant screen changes.", p.reduceMotion) {
                        value ->
                        vm.customize { it.copy(reduceMotion = value) }
                    }
                    if (!p.reduceMotion)
                        ChoiceSetting(
                            "Animation speed",
                            p.motionSpeed,
                            listOf("Quick", "Balanced", "Relaxed"),
                        ) { value ->
                            vm.customize { it.copy(motionSpeed = value) }
                        }
                    ToggleSetting(
                        "Touch feedback",
                        "Subtle haptics for navigation and bookmarks.",
                        p.haptics,
                    ) { value ->
                        vm.customize { it.copy(haptics = value) }
                    }
                }
            }
        if (
            visible(
                "Home",
                "start screen launch dashboard spotlight library projects history sections",
            )
        )
            item {
                PaperSurface {
                    Eyebrow("Your starting point")
                    ChoiceSetting(
                        "Open the app on",
                        Destination.valueOf(p.startScreen).label,
                        listOf("Home", "Discover", "Library", "Projects"),
                    ) { label ->
                        vm.customize {
                            it.copy(
                                startScreen =
                                    Destination.entries.first { d -> d.label == label }.name
                            )
                        }
                    }
                    Text(
                        "Start screen changes apply on the next launch.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ToggleSetting(
                        "Home spotlight",
                        "Show the research invitation card.",
                        p.homeSpotlight,
                    ) { value ->
                        vm.customize { it.copy(homeSpotlight = value) }
                    }
                    ToggleSetting(
                        "Recent library",
                        "Keep recently saved sources on Home.",
                        p.homeLibrary,
                    ) { value ->
                        vm.customize { it.copy(homeLibrary = value) }
                    }
                    ToggleSetting(
                        "Project overview",
                        "Show active projects and task progress.",
                        p.homeProjects,
                    ) { value ->
                        vm.customize { it.copy(homeProjects = value) }
                    }
                    ToggleSetting(
                        "Recent searches",
                        "Continue a search from Home.",
                        p.homeHistory,
                    ) { value ->
                        vm.customize { it.copy(homeHistory = value) }
                    }
                }
            }
        if (
            visible(
                "Discovery",
                "search default papers datasets code provider year PDF sorting relevance",
            )
        )
            item {
                PaperSurface {
                    Eyebrow("Discovery defaults")
                    Text(
                        "Defaults apply when starting a fresh session. Refine an active search inside Discover.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ChoiceSetting(
                        "Search for",
                        p.searchKind.lowercase().replaceFirstChar { it.uppercase() },
                        listOf("Paper", "Dataset", "Code"),
                    ) { value ->
                        vm.customize { it.copy(searchKind = value.uppercase()) }
                    }
                    ChoiceSetting(
                        "Paper provider",
                        p.searchProvider,
                        listOf("All sources", "OpenAlex", "Crossref", "arXiv"),
                    ) { value ->
                        vm.customize { it.copy(searchProvider = value) }
                    }
                    var year by
                        remember(p.searchYear) {
                            mutableStateOf(if (p.searchYear == 0) "" else p.searchYear.toString())
                        }
                    OutlinedTextField(
                        year,
                        { year = it.filter(Char::isDigit).take(4) },
                        Modifier.fillMaxWidth(),
                        label = { Text("Papers published from year (optional)") },
                        supportingText = { Text("Leave blank to include all years.") },
                        singleLine = true,
                    )
                    TextButton(
                        enabled =
                            (year.isBlank() || year.toIntOrNull() in 1500..2100) &&
                                (year.toIntOrNull() ?: 0) != p.searchYear,
                        onClick = { vm.customize { it.copy(searchYear = year.toIntOrNull() ?: 0) } },
                    ) {
                        Text("Save year")
                    }
                    ToggleSetting(
                        "Direct PDF only",
                        "Default paper searches to records with PDF links.",
                        p.searchPdfOnly,
                    ) { value ->
                        vm.customize { it.copy(searchPdfOnly = value) }
                    }
                    ChoiceSetting(
                        "Order loaded results",
                        p.searchSort,
                        AppPreferences.searchSorts,
                    ) { value ->
                        vm.customize { it.copy(searchSort = value) }
                    }
                }
            }
        if (
            visible(
                "Library",
                "cards authors abstract preview lines metadata default collection sorting export BibTeX RIS Markdown confirmation",
            )
        )
            item {
                PaperSurface {
                    Eyebrow("Library & source cards")
                    ChoiceSetting("Library order", p.librarySort, AppPreferences.librarySorts) {
                        value ->
                        vm.customize { it.copy(librarySort = value) }
                    }
                    var collection by
                        remember(p.defaultCollection) { mutableStateOf(p.defaultCollection) }
                    OutlinedTextField(
                        collection,
                        { collection = it.take(80) },
                        Modifier.fillMaxWidth(),
                        label = { Text("Default collection for new saves") },
                        singleLine = true,
                    )
                    TextButton(
                        enabled =
                            collection.isNotBlank() && collection.trim() != p.defaultCollection,
                        onClick = { vm.customize { it.copy(defaultCollection = collection) } },
                    ) {
                        Text("Save collection")
                    }
                    ToggleSetting(
                        "Show authors",
                        "Display authors on source cards.",
                        p.cardAuthors,
                    ) { value ->
                        vm.customize { it.copy(cardAuthors = value) }
                    }
                    ToggleSetting(
                        "Abstract previews",
                        "Read a short description in the list.",
                        p.cardAbstract,
                    ) { value ->
                        vm.customize { it.copy(cardAbstract = value) }
                    }
                    if (p.cardAbstract)
                        SliderSetting(
                            "Preview length",
                            "${p.cardLines} lines",
                            p.cardLines.toFloat(),
                            1f..5f,
                            3,
                        ) { value ->
                            vm.customize { it.copy(cardLines = value.roundToInt()) }
                        }
                    ToggleSetting(
                        "Source metadata",
                        "Show provider, year, and reading status.",
                        p.cardMetadata,
                    ) { value ->
                        vm.customize { it.copy(cardMetadata = value) }
                    }
                    ChoiceSetting(
                        "Export format",
                        p.exportFormat,
                        listOf("BibTeX", "RIS", "Markdown"),
                    ) { value ->
                        vm.customize { it.copy(exportFormat = value) }
                    }
                    ToggleSetting(
                        "Confirm source removal",
                        "Ask before removing a source and its notes.",
                        p.confirmRemoval,
                    ) { value ->
                        vm.customize { it.copy(confirmRemoval = value) }
                    }
                }
            }
        if (
            visible(
                "Reader",
                "PDF paper sepia night zoom awake screen progress remember page double tap focus",
            )
        )
            item {
                PaperSurface {
                    Eyebrow("Reading comfort")
                    ChoiceSetting(
                        "PDF appearance",
                        p.readerTheme,
                        listOf("Original", "Sepia", "Night"),
                    ) { value ->
                        vm.customize { it.copy(readerTheme = value) }
                    }
                    SliderSetting(
                        "Opening zoom",
                        "${(p.readerZoom * 100).roundToInt()}%",
                        p.readerZoom,
                        1f..2f,
                        3,
                    ) { value ->
                        vm.customize { it.copy(readerZoom = value) }
                    }
                    ToggleSetting(
                        "Keep screen awake",
                        "Keep the display on while the reader is open.",
                        p.readerKeepAwake,
                    ) { value ->
                        vm.customize { it.copy(readerKeepAwake = value) }
                    }
                    ToggleSetting(
                        "Remember reading position",
                        "Resume each PDF on its last page.",
                        p.readerRememberPage,
                    ) { value ->
                        vm.customize { it.copy(readerRememberPage = value) }
                    }
                    ToggleSetting(
                        "Double tap to zoom",
                        "Toggle between fitted and enlarged pages.",
                        p.readerDoubleTap,
                    ) { value ->
                        vm.customize { it.copy(readerDoubleTap = value) }
                    }
                    ToggleSetting(
                        "Open in focus mode",
                        "Hide reader tools; tap the page to reveal them.",
                        p.readerFocus,
                    ) { value ->
                        vm.customize { it.copy(readerFocus = value) }
                    }
                }
            }
        if (visible("Privacy", "history recent searches clear local data"))
            item {
                PaperSurface {
                    Eyebrow("Search privacy")
                    ToggleSetting(
                        "Remember search history",
                        "Turning this off also clears this workspace's recent searches.",
                        p.rememberHistory,
                    ) { value ->
                        vm.customize { it.copy(rememberHistory = value) }
                    }
                    OutlinedButton(onClick = { vm.clearHistory() }) {
                        Text("Clear recent searches")
                    }
                    Text(
                        "Searches are sent to the providers you select. Appearance and reading preferences stay on this device; workspace backups do not contain account credentials.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        item { TextButton(onClick = { reset = true }) { Text("Reset all customization") } }
    }
    if (reset)
        AlertDialog(
            onDismissRequest = { reset = false },
            title = { Text("Restore default preferences?") },
            text = {
                Text(
                    "This resets appearance, layout, discovery, library, and reading preferences. Your sources, projects, notes, and account are kept."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.resetCustomization()
                        reset = false
                    }
                ) {
                    Text("Reset preferences")
                }
            },
            dismissButton = { TextButton(onClick = { reset = false }) { Text("Cancel") } },
        )
}

@Composable
fun ChoiceSetting(title: String, value: String, choices: List<String>, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable { open = true }.heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                value,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Icon(Icons.Outlined.ExpandMore, "Change $title")
    }
    if (open)
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    choices.forEach { choice ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    onChange(choice)
                                    open = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = choice == value,
                                onClick = {
                                    onChange(choice)
                                    open = false
                                },
                            )
                            Text(choice, Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Close") } },
        )
}

@Composable
fun ToggleSetting(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.semanticsLabel(title),
        )
    }
}

@Composable
private fun SliderSetting(
    title: String,
    subtitle: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.semanticsLabel(title),
        )
    }
}
