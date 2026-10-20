package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    vm: OverthinkViewModel,
    onOpen: (ResearchItem) -> Unit,
    onImport: () -> Unit,
    onExport: (List<ResearchItem>) -> Unit,
) {
    val columns = sourceColumns()
    val w by vm.workspace.collectAsStateWithLifecycle()
    val p = LocalAppPreferences.current
    var query by rememberSaveable { mutableStateOf("") }
    var collection by rememberSaveable { mutableStateOf("All collections") }
    var kind by rememberSaveable { mutableStateOf("All types") }
    var status by rememberSaveable { mutableStateOf("Any reading status") }
    var favorites by rememberSaveable { mutableStateOf(false) }
    var filters by rememberSaveable { mutableStateOf(false) }
    val collections = w.items.map { it.collection }.distinct().sorted()
    LaunchedEffect(collections) {
        if (collection != "All collections" && collection !in collections)
            collection = "All collections"
    }
    val items =
        w.items
            .filter {
                (collection == "All collections" || it.collection == collection) &&
                    (kind == "All types" || it.kind.name == kind) &&
                    (status == "Any reading status" || it.readingStatus.label == status) &&
                    (!favorites || it.favorite) &&
                    "${it.title} ${it.authors} ${it.summary} ${it.tags.joinToString(" ")}"
                        .contains(query, true)
            }
            .sortedForLibrary(p.librarySort)
    val activeFilters = kind != "All types" || status != "Any reading status" || favorites
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(p.gutter),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            SectionTitle(
                "Your growing library",
                "${w.items.size} sources · ${w.notes.size} notes · ${w.items.count { it.favorite }} favorites",
            )
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onImport) {
                    Icon(Icons.Outlined.UploadFile, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Import PDF")
                }
                OutlinedButton(onClick = { onExport(items) }, enabled = items.isNotEmpty()) {
                    Icon(Icons.Outlined.FileDownload, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export ${p.exportFormat}")
                }
            }
        }
        item {
            OutlinedTextField(
                query,
                { query = it },
                Modifier.fillMaxWidth(),
                label = { Text("Search title, author, or tag") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { filters = true }) {
                        Icon(
                            Icons.Outlined.Tune,
                            "Filter your library",
                            tint =
                                if (activeFilters) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        }
        if (collections.isNotEmpty())
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("All collections") + collections) { name ->
                        FilterChip(
                            selected = collection == name,
                            onClick = { collection = name },
                            label = { Text(name) },
                        )
                    }
                }
            }
        if (activeFilters)
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        listOfNotNull(
                                kind.takeIf { it != "All types" }?.lowercase(),
                                status.takeIf { it != "Any reading status" },
                                "Favorites".takeIf { favorites },
                            )
                            .joinToString(" · "),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(
                        onClick = {
                            kind = "All types"
                            status = "Any reading status"
                            favorites = false
                        }
                    ) {
                        Text("Clear filters")
                    }
                }
            }
        item {
            ChoiceSetting(
                "${items.size} sources · library order",
                p.librarySort,
                AppPreferences.librarySorts,
            ) { value ->
                vm.customize { it.copy(librarySort = value) }
            }
        }
        if (items.isEmpty())
            item {
                EmptyState(
                    if (w.items.isEmpty()) "Ideas deserve a place to stay"
                    else "No matching sources",
                    if (w.items.isEmpty())
                        "Save sources from Discover or import a PDF to read and annotate offline."
                    else "Try another collection, tag, or filter.",
                )
            }
        researchRows(items, columns, vm, onOpen)
    }
    if (filters)
        AlertDialog(
            onDismissRequest = { filters = false },
            title = { Text("Shape your library") },
            text = {
                Column {
                    ChoiceSetting(
                        "Source type",
                        ItemKind.entries
                            .find { it.name == kind }
                            ?.let {
                                when (it) {
                                    ItemKind.PAPER -> "Papers"
                                    ItemKind.DATASET -> "Datasets"
                                    ItemKind.CODE -> "Code"
                                }
                            } ?: "All types",
                        listOf("All types", "Papers", "Datasets", "Code"),
                    ) {
                        kind =
                            when (it) {
                                "Papers" -> "PAPER"
                                "Datasets" -> "DATASET"
                                "Code" -> "CODE"
                                else -> "All types"
                            }
                    }
                    ChoiceSetting(
                        "Reading status",
                        status,
                        listOf("Any reading status") + ReadingStatus.entries.map { it.label },
                    ) {
                        status = it
                    }
                    ToggleSetting("Favorites only", "Show your starred sources.", favorites) {
                        favorites = it
                    }
                    Text(
                        "Export uses the sources currently shown in your library.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { filters = false }) { Text("Done") } },
        )
}

@Composable
fun ProjectsScreen(vm: OverthinkViewModel, onOpen: (ResearchItem) -> Unit) {
    val w by vm.workspace.collectAsStateWithLifecycle()
    var activeId by rememberSaveable { mutableStateOf<String?>(null) }
    var create by rememberSaveable { mutableStateOf(false) }
    var delete by remember { mutableStateOf<Project?>(null) }
    var links by remember { mutableStateOf(false) }
    val active = w.projects.find { it.id == activeId }
    BackHandler(enabled = active != null) { activeId = null }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(LocalAppPreferences.current.gutter),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (active == null) {
            item {
                SectionTitle(
                    "Good questions.\nClear next steps.",
                    "A workspace for the work behind your research.",
                )
            }
            item {
                Button(onClick = { create = true }) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("New project")
                }
            }
            if (w.projects.isEmpty())
                item {
                    EmptyState(
                        "Give your idea a direction",
                        "Define your research question, connect relevant sources, and break the work into manageable milestones.",
                    )
                }
            items(w.projects, key = { it.id }) { p ->
                PaperSurface(Modifier.clickable { activeId = p.id }) {
                    Eyebrow(if (p.deadline.isBlank()) "Research workspace" else "Due ${p.deadline}")
                    Text(p.title, style = MaterialTheme.typography.headlineMedium)
                    if (p.question.isNotBlank()) Text(p.question)
                    LinearProgressIndicator(
                        progress = { p.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "${p.tasks.count{it.status==TaskStatus.DONE}} of ${p.tasks.size} tasks complete · ${p.paperIds.size} linked sources",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            item {
                TextButton(onClick = { activeId = null }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                    Spacer(Modifier.width(8.dp))
                    Text("All projects")
                }
            }
            item {
                SectionTitle(active.title, active.question)
                if (active.deadline.isNotBlank()) Tag("Due ${active.deadline}")
            }
            item {
                LinearProgressIndicator(
                    progress = { active.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "${(active.progress*100).toInt()}% complete",
                    Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { SectionTitle("Next steps", "Tap a status to move a task forward.") }
            items(active.tasks, key = { it.id }) { task ->
                PaperSurface {
                    Text(task.title, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                val next =
                                    TaskStatus.entries[
                                            (task.status.ordinal + 1) % TaskStatus.entries.size]
                                vm.project(
                                    active.copy(
                                        tasks =
                                            active.tasks.map {
                                                if (it.id == task.id) it.copy(status = next) else it
                                            }
                                    )
                                )
                            }
                        ) {
                            Text(task.status.name.replace('_', ' '))
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                vm.project(
                                    active.copy(tasks = active.tasks.filterNot { it.id == task.id })
                                )
                            }
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, "Delete task")
                        }
                    }
                }
            }
            item {
                var title by rememberSaveable(active.id) { mutableStateOf("") }
                OutlinedTextField(
                    title,
                    { title = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("Add a next step") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (title.isNotBlank()) {
                                    vm.project(
                                        active.copy(
                                            tasks = active.tasks + ProjectTask(title = title.trim())
                                        )
                                    )
                                    title = ""
                                }
                            },
                            enabled = title.isNotBlank(),
                        ) {
                            Icon(Icons.Outlined.Add, "Add task")
                        }
                    },
                )
            }
            item {
                SectionTitle(
                    "Your evidence",
                    action = { TextButton(onClick = { links = true }) { Text("Link sources") } },
                )
            }
            items(w.items.filter { it.id in active.paperIds }, key = { "source:${it.id}" }) { item
                ->
                ResearchCard(item, vm, { onOpen(item) })
            }
            item {
                OutlinedButton(onClick = { delete = active }) {
                    Text("Delete project", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (create) {
        var title by rememberSaveable { mutableStateOf("") }
        var question by rememberSaveable { mutableStateOf("") }
        var date by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { create = false },
            title = { Text("Start a research project") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("Project title") })
                    OutlinedTextField(
                        question,
                        { question = it },
                        label = { Text("Your research question") },
                        minLines = 2,
                    )
                    OutlinedTextField(
                        date,
                        { date = it },
                        label = { Text("Deadline (YYYY-MM-DD, optional)") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = title.isNotBlank(),
                    onClick = {
                        val valid =
                            date.isBlank() ||
                                runCatching {
                                        require(date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
                                        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                                            .apply { isLenient = false }
                                            .parse(date)
                                    }
                                    .isSuccess
                        if (valid) {
                            vm.project(
                                Project(
                                    title = title.trim(),
                                    question = question.trim(),
                                    deadline = date,
                                )
                            )
                            create = false
                        } else vm.notify("Enter a real date in YYYY-MM-DD format")
                    },
                ) {
                    Text("Create project")
                }
            },
            dismissButton = { TextButton(onClick = { create = false }) { Text("Cancel") } },
        )
    }
    if (links && active != null)
        AlertDialog(
            onDismissRequest = { links = false },
            title = { Text("Link saved sources") },
            text = {
                LazyColumn {
                    if (w.items.isEmpty()) item { Text("Save sources to your library first.") }
                    items(w.items) { item ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = item.id in active.paperIds,
                                onCheckedChange = { checked ->
                                    vm.project(
                                        active.copy(
                                            paperIds =
                                                if (checked) (active.paperIds + item.id).distinct()
                                                else active.paperIds - item.id
                                        )
                                    )
                                },
                            )
                            Text(item.title)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { links = false }) { Text("Done") } },
        )
    delete?.let { p ->
        AlertDialog(
            onDismissRequest = { delete = null },
            title = { Text("Delete this project?") },
            text = {
                Text(
                    "Tasks and project links will be removed. Your saved sources and notes will remain in the library."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteProject(p.id)
                        delete = null
                        activeId = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { delete = null }) { Text("Keep project") } },
        )
    }
}

@Composable
fun SettingsScreen(
    vm: OverthinkViewModel,
    onExport: () -> Unit,
    onRestore: () -> Unit,
    onPersonalize: () -> Unit,
) {
    val account by vm.account.collectAsStateWithLifecycle()
    val preferences by vm.preferences.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var server by rememberSaveable { mutableStateOf(vm.backend.baseUrl) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showDelete by remember { mutableStateOf(false) }
    var restoreServer by remember { mutableStateOf(false) }
    var licenses by remember { mutableStateOf(false) }
    val context = LocalContext.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(LocalAppPreferences.current.gutter),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { SectionTitle("Make it your space", "Account, appearance, and your research data.") }
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Outlined.Palette,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "Designed around you",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "Colors, typography, home sections, discovery defaults, source cards, and reading comfort.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Button(onClick = onPersonalize) {
                        Text("Customize your workspace")
                        Spacer(Modifier.width(8.dp))
                        ForwardIcon()
                    }
                }
            }
        }
        item {
            PaperSurface {
                Eyebrow("Workspace")
                Text(
                    account?.let { "Signed in as ${it.username}" } ?: "On this device",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    if (account == null)
                        "Your local workspace works without an account. Connected accounts each have a separate workspace."
                    else "Your account workspace is separate from your device's guest library.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (account == null) {
                    OutlinedTextField(
                        server,
                        { server = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Server URL") },
                        placeholder = { Text("https://research.your-domain.com") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        username,
                        { username = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Username") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        password,
                        { password = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Password (12+ characters)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled =
                                !busy &&
                                    server.isNotBlank() &&
                                    username.isNotBlank() &&
                                    password.isNotBlank(),
                            onClick = {
                                vm.setServer(server)
                                vm.authenticate(username, password, false)
                                password = ""
                            },
                        ) {
                            Text("Sign in")
                        }
                        OutlinedButton(
                            enabled =
                                !busy &&
                                    server.isNotBlank() &&
                                    username.length >= 3 &&
                                    password.length >= 12,
                            onClick = {
                                vm.setServer(server)
                                vm.authenticate(username, password, true)
                                password = ""
                            },
                        ) {
                            Text("Create account")
                        }
                    }
                    Text(
                        "Use the backend included with this project. No email verification or recovery is claimed; account recovery requires your server operator.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.synchronize() }, enabled = !busy) {
                            Text("Sync workspace")
                        }
                        OutlinedButton(onClick = { vm.logout() }, enabled = !busy) {
                            Text("Sign out")
                        }
                    }
                }
            }
        }
        item {
            PaperSurface {
                Eyebrow("Appearance")
                Text("A comfortable place to read", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("System", "Light", "Dark").forEach { value ->
                        FilterChip(
                            selected = preferences.theme == value,
                            onClick = { vm.theme(value) },
                            label = { Text(value) },
                        )
                    }
                }
            }
        }
        item {
            PaperSurface {
                Eyebrow("Your data")
                Text("Keep a copy of your work", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Backups contain sources, projects, and notes. PDF files remain on this device and can be shared from the reader.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onExport) { Text("Export workspace backup") }
                OutlinedButton(onClick = onRestore) { Text("Merge a backup") }
                TextButton(onClick = { vm.legacy() }) {
                    Text("Import papers from the previous app")
                }
                Text(
                    "The original database is preserved. Known seeded sample papers are excluded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            PaperSurface {
                Eyebrow("Research with care")
                Text("Follow the original evidence", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Provider metadata may be incomplete. Open the original source to confirm authorship, availability, and publication status. AI works only with the supplied source text and can still make mistakes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (account != null)
            item {
                Column {
                    TextButton(onClick = { restoreServer = true }) {
                        Text("Restore the server workspace on this device")
                    }
                    TextButton(onClick = { showDelete = true }) {
                        Text("Delete account", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        item {
            Text(
                "Overthink 2.1 · Research, with room to think.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { licenses = true }) { Text("Open source notices") }
        }
    }
    if (licenses)
        AlertDialog(
            onDismissRequest = { licenses = false },
            title = { Text("Open source notices") },
            text = {
                val license = remember {
                    context.assets.open("licenses/androidx.txt").bufferedReader().use {
                        it.readText()
                    }
                }
                Text(
                    license,
                    Modifier.verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = { TextButton(onClick = { licenses = false }) { Text("Close") } },
        )
    if (showDelete) {
        var confirmation by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Permanently delete your account?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Your server workspace and this account's local records will be deleted. Export a backup first if you want to keep them. PDF files remain on this device."
                    )
                    OutlinedTextField(
                        confirmation,
                        { confirmation = it },
                        label = { Text("Confirm your password") },
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = confirmation.isNotBlank() && !busy,
                    onClick = {
                        vm.deleteAccount(confirmation)
                        confirmation = ""
                        showDelete = false
                    },
                ) {
                    Text("Delete permanently", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }
    if (restoreServer)
        AlertDialog(
            onDismissRequest = { restoreServer = false },
            title = { Text("Replace this local workspace?") },
            text = {
                Text(
                    "The current server version will replace this account's local records. Export a backup first to preserve unsynchronized changes. This does not affect the guest workspace."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.restoreServerWorkspace()
                        restoreServer = false
                    },
                    enabled = !busy,
                ) {
                    Text("Restore server version")
                }
            },
            dismissButton = { TextButton(onClick = { restoreServer = false }) { Text("Cancel") } },
        )
}
