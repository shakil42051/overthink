package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*

@Composable
fun HomeScreen(
    vm: OverthinkViewModel,
    onSearch: (String) -> Unit,
    onNavigate: (Destination) -> Unit,
    onOpen: (ResearchItem) -> Unit,
) {
    val workspace by vm.workspace.collectAsStateWithLifecycle()
    val columns = sourceColumns()
    val p = LocalAppPreferences.current
    var query by rememberSaveable { mutableStateOf("") }
    val totalTasks = workspace.projects.sumOf { it.tasks.size }
    val completedTasks =
        workspace.projects.sumOf { project -> project.tasks.count { it.status == TaskStatus.DONE } }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(p.gutter),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow("Your next idea starts here")
                Text(
                    "A clearer space.\nA bigger perspective.",
                    style = MaterialTheme.typography.displayMedium,
                )
                Text(
                    "Discover, connect, and create with your research.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { SearchBox(query, { query = it }, { onSearch(query) }) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WorkspaceMetric("Sources", workspace.items.size.toString(), Modifier.weight(1f)) {
                    onNavigate(Destination.LIBRARY)
                }
                WorkspaceMetric("Notes", workspace.notes.size.toString(), Modifier.weight(1f)) {
                    onNavigate(Destination.LIBRARY)
                }
                WorkspaceMetric(
                    "Projects",
                    workspace.projects.size.toString(),
                    Modifier.weight(1f),
                ) {
                    onNavigate(Destination.PROJECTS)
                }
            }
        }
        if (p.homeSpotlight)
            item {
                val colors = MaterialTheme.colorScheme
                Surface(shape = RoundedCornerShape(p.corner), color = colors.primary) {
                    Box(
                        Modifier.fillMaxWidth()
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(colors.primary, colors.primary.copy(alpha = .78f))
                                )
                            )
                    ) {
                        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                            val center =
                                androidx.compose.ui.geometry.Offset(
                                    size.width * .94f,
                                    size.height * .15f,
                                )
                            for (radius in listOf(80f, 140f, 200f)) drawCircle(
                                colors.onPrimary.copy(alpha = .10f),
                                radius * density,
                                center,
                                style =
                                    androidx.compose.ui.graphics.drawscope.Stroke(1.2f * density),
                            )
                        }
                        Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, null, tint = colors.onPrimary)
                            Text(
                                "Stay curious.\nFind your next connection.",
                                style = MaterialTheme.typography.headlineLarge,
                                color = colors.onPrimary,
                            )
                            Text(
                                "A paper, a dataset, a new possibility. Follow the evidence wherever it leads.",
                                color = colors.onPrimary.copy(alpha = .85f),
                            )
                            FilledTonalButton(onClick = { onNavigate(Destination.SEARCH) }) {
                                Text("Explore research")
                                Spacer(Modifier.width(8.dp))
                                ForwardIcon()
                            }
                        }
                    }
                }
            }
        if (p.homeProjects) {
            item {
                SectionTitle(
                    "Move an idea forward",
                    if (totalTasks > 0) "$completedTasks of $totalTasks tasks complete"
                    else "Turn a question into a practical plan.",
                    {
                        TextButton(onClick = { onNavigate(Destination.PROJECTS) }) {
                            Text("Projects")
                        }
                    },
                )
            }
            if (workspace.projects.isEmpty())
                item {
                    PaperSurface {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Icon(
                                Icons.Outlined.FolderOpen,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Give your ideas direction",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    "Link sources, plan tasks, and track progress.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        TextButton(onClick = { onNavigate(Destination.PROJECTS) }) {
                            Text("Start a project")
                            Spacer(Modifier.width(8.dp))
                            ForwardIcon()
                        }
                    }
                }
            items(workspace.projects.take(2), key = { "project:${it.id}" }) { project ->
                PaperSurface(Modifier.clickable { onNavigate(Destination.PROJECTS) }) {
                    Text(project.title, style = MaterialTheme.typography.titleMedium)
                    val progress by
                        androidx.compose.animation.core.animateFloatAsState(
                            project.progress,
                            androidx.compose.animation.core.tween(p.motionMillis),
                            label = "project progress",
                        )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "${project.tasks.count { it.status == TaskStatus.DONE }} / ${project.tasks.size} tasks · ${project.paperIds.size} linked sources",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (p.homeLibrary) {
            item {
                SectionTitle(
                    "On your desk",
                    if (workspace.items.isEmpty()) "Keep the sources that spark something."
                    else "Recently saved, ready when you are.",
                    {
                        TextButton(onClick = { onNavigate(Destination.LIBRARY) }) {
                            Text("View all")
                        }
                    },
                )
            }
            if (workspace.items.isEmpty())
                item {
                    EmptyState(
                        "Keep what matters",
                        "Save a source from Discover or import your own PDF. Your library and notes are available offline.",
                    ) {
                        OutlinedButton(onClick = { onNavigate(Destination.LIBRARY) }) {
                            Text("Open your library")
                        }
                    }
                }
            researchRows(
                workspace.items.sortedForLibrary("Recently saved").take(3),
                columns,
                vm,
                onOpen,
            )
        }
        if (p.homeHistory && p.rememberHistory && workspace.history.isNotEmpty())
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("Pick up a thread")
                    workspace.history.take(4).forEach { q ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onSearch(q) }.heightIn(min = 52.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.History,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(q, Modifier.weight(1f).padding(horizontal = 12.dp))
                            ForwardIcon()
                        }
                    }
                }
            }
        item {
            TextButton(onClick = { onNavigate(Destination.PERSONALIZE) }) {
                Icon(Icons.Outlined.Tune, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Make this space yours")
            }
        }
    }
}

@Composable
private fun WorkspaceMetric(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f),
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
fun SearchBox(query: String, onChange: (String) -> Unit, onSearch: () -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("A topic, a paper, a DOI…") },
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        trailingIcon = {
            IconButton(onClick = onSearch, enabled = query.isNotBlank()) {
                Icon(Icons.Outlined.ArrowCircleRight, "Search")
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResearchCard(item: ResearchItem, vm: OverthinkViewModel, onOpen: () -> Unit) {
    val p = LocalAppPreferences.current
    val haptics = LocalHapticFeedback.current
    val workspace by vm.workspace.collectAsStateWithLifecycle()
    val comparison by vm.comparison.collectAsStateWithLifecycle()
    val record = workspace.items.find { it.id == item.id }
    val saved = record != null
    var confirmRemove by remember { mutableStateOf(false) }
    PaperSurface {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (p.cardMetadata) Tag(item.source.ifBlank { item.kind.name.lowercase() })
            if (p.cardMetadata)
                item.year?.let {
                    Text(
                        "$it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            Spacer(Modifier.weight(1f))
            if (record != null)
                IconButton(
                    onClick = { vm.organize(item.id) { it.copy(favorite = !it.favorite) } }
                ) {
                    Icon(
                        if (record.favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        if (record.favorite) "Unfavorite source" else "Favorite source",
                        tint =
                            if (record.favorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            IconButton(
                onClick = {
                    if (p.haptics) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (saved) {
                        if (p.confirmRemoval) confirmRemove = true else vm.remove(item)
                    } else vm.save(item)
                }
            ) {
                Icon(
                    if (saved) Icons.Outlined.BookmarkAdded else Icons.Outlined.BookmarkAdd,
                    if (saved) "Remove from library" else "Save to library",
                )
            }
        }
        Text(
            item.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        )
        if (p.cardAuthors && item.authors.isNotBlank())
            Text(
                item.authors,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        if (p.cardAbstract && item.summary.isNotBlank())
            Text(
                item.summary,
                maxLines = p.cardLines,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        if (record != null && p.cardMetadata) {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item { Tag(record.readingStatus.label) }
                items(record.tags.take(4)) { Tag(it) }
            }
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onOpen) {
                Text(if (item.kind == ItemKind.PAPER) "View paper" else "View source")
                Spacer(Modifier.width(8.dp))
                ForwardIcon()
            }
            if (item.kind == ItemKind.PAPER)
                TextButton(onClick = { vm.compare(item) }) {
                    Icon(
                        if (comparison.any { it.id == item.id }) Icons.Outlined.Check
                        else Icons.AutoMirrored.Outlined.CompareArrows,
                        null,
                        Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Compare")
                }
        }
    }
    if (confirmRemove)
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove this source?") },
            text = {
                Text(
                    "This removes it from your library and projects, along with its notes. Export a workspace backup first if you need to keep a copy."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.remove(item)
                        confirmRemove = false
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text("Keep source") }
            },
        )
}

@Composable
fun DiscoverScreen(vm: OverthinkViewModel, onOpen: (ResearchItem) -> Unit, onCompare: () -> Unit) {
    val p = LocalAppPreferences.current
    val columns = sourceColumns()
    val state by vm.discovery.collectAsStateWithLifecycle()
    val compare by vm.comparison.collectAsStateWithLifecycle()
    var showFilters by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(p.gutter),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { SectionTitle("Follow your curiosity", "Research starts with a better question.") }
        item { SearchBox(state.query, vm::query, { vm.search() }) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ItemKind.entries) { kind ->
                    FilterChip(
                        selected = state.kind == kind,
                        onClick = { vm.category(kind) },
                        label = {
                            Text(
                                when (kind) {
                                    ItemKind.PAPER -> "Papers"
                                    ItemKind.DATASET -> "Datasets"
                                    ItemKind.CODE -> "Code"
                                }
                            )
                        },
                    )
                }
                if (state.kind == ItemKind.PAPER)
                    item {
                        OutlinedButton(onClick = { showFilters = true }) {
                            Icon(Icons.Outlined.Tune, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Refine")
                        }
                    }
            }
        }
        if (compare.isNotEmpty())
            item {
                FilledTonalButton(onClick = onCompare, modifier = Modifier.fillMaxWidth()) {
                    Text("Compare ${compare.size} selected papers")
                    Spacer(Modifier.width(10.dp))
                    ForwardIcon()
                }
            }
        if (
            state.kind == ItemKind.PAPER &&
                (state.filter.pdfOnly ||
                    state.filter.fromYear > 0 ||
                    state.filter.source != "All sources")
        )
            item {
                Text(
                    "${state.filter.source} · ${if (state.filter.fromYear == 0) "Any year" else "Since ${state.filter.fromYear}"}${if(state.filter.pdfOnly)" · Direct PDF" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        if (state.loading)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(
                        "Searching academic sources…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        if (state.statuses.isNotEmpty())
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.statuses.forEach { s ->
                        Text(
                            "${s.provider}: ${s.error ?: "${s.items.size} records received"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                if (s.error != null) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        if (!state.searched)
            item {
                EmptyState(
                    "An open door to new ideas",
                    "Search across OpenAlex, Crossref and arXiv. Explore dataset records from DataCite and research code on GitHub. Sources are credited on each record.",
                )
            }
        if (state.searched && !state.loading && state.items.isEmpty())
            item {
                EmptyState(
                    "No results to show",
                    if (state.statuses.any { it.error != null })
                        "Some sources could not be reached. Review the status above or try again."
                    else "Try fewer keywords or broaden your filters.",
                ) {
                    OutlinedButton(onClick = { vm.search() }) { Text("Try again") }
                }
            }
        if (state.items.isNotEmpty())
            item {
                ChoiceSetting("Order loaded results", p.searchSort, AppPreferences.searchSorts) {
                    value ->
                    vm.customize { it.copy(searchSort = value) }
                }
            }
        researchRows(state.items.sortedForSearch(p.searchSort), columns, vm, onOpen)
        if (state.more && !state.loading)
            item {
                OutlinedButton(onClick = { vm.search(true) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Load more sources")
                }
            }
    }
    if (showFilters) {
        var source by remember { mutableStateOf(state.filter.source) }
        var year by remember {
            mutableStateOf(if (state.filter.fromYear == 0) "" else state.filter.fromYear.toString())
        }
        var pdf by remember { mutableStateOf(state.filter.pdfOnly) }
        AlertDialog(
            onDismissRequest = { showFilters = false },
            title = { Text("Refine your search") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Source")
                    listOf("All sources", "OpenAlex", "Crossref", "arXiv").forEach { s ->
                        Row(
                            Modifier.fillMaxWidth().clickable { source = s },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = source == s, onClick = { source = s })
                            Text(s)
                        }
                    }
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it.filter(Char::isDigit).take(4) },
                        label = { Text("Published from year (optional)") },
                        supportingText = { Text("Leave blank to include all years.") },
                        singleLine = true,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = pdf, onCheckedChange = { pdf = it })
                        Text("Direct PDF link available")
                    }
                    Text(
                        "Metadata is reported by each provider. A PDF link does not establish peer review.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val y = if (year.isBlank()) 0 else year.toIntOrNull()
                        if (y != null && (y == 0 || y in 1500..2100)) {
                            vm.filters(SearchFilter(source, y, pdf))
                            showFilters = false
                        } else vm.notify("Enter a year between 1500 and 2100, or leave it blank")
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        vm.filters(SearchFilter())
                        showFilters = false
                    }
                ) {
                    Text("Reset")
                }
            },
        )
    }
}

@Composable
fun sourceColumns(): Int {
    val width = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    val available =
        minOf(1180, width - if (width >= 840) 80 else 0) -
            LocalAppPreferences.current.gutter.value.toInt() * 2
    return (available / 340).coerceIn(1, 3)
}

fun androidx.compose.foundation.lazy.LazyListScope.researchRows(
    sources: List<ResearchItem>,
    columns: Int,
    vm: OverthinkViewModel,
    onOpen: (ResearchItem) -> Unit,
) {
    items(sources.chunked(columns), key = { "sources:${it.first().id}" }) { row ->
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            row.forEach { source ->
                Box(Modifier.weight(1f)) { ResearchCard(source, vm) { onOpen(source) } }
            }
            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}
