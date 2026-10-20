package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class Destination(val label: String) {
    HOME("Home"),
    SEARCH("Discover"),
    LIBRARY("Library"),
    PROJECTS("Projects"),
    ASSISTANT("Assistant"),
    SETTINGS("Settings"),
    COMPARE("Compare"),
    PERSONALIZE("Personalize"),
}

@Composable
fun OverthinkApp(vm: OverthinkViewModel) {
    val preferences by vm.preferences.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val screenStates = rememberSaveableStateHolder()
    var destinationName by rememberSaveable { mutableStateOf(preferences.startScreen) }
    val destination = Destination.valueOf(destinationName)
    fun navigate(d: Destination) {
        if (preferences.haptics && destinationName != d.name)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        destinationName = d.name
    }
    var selectedJson by rememberSaveable { mutableStateOf<String?>(null) }
    val selected =
        remember(selectedJson) {
            selectedJson?.let { runCatching { ResearchItem.from(JSONObject(it)) }.getOrNull() }
        }
    var readerPath by rememberSaveable { mutableStateOf<String?>(null) }
    val workspace by vm.workspace.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val account by vm.account.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(vm) { vm.events.collect { snackbar.showSnackbar(it) } }
    val pendingExport = remember(context) { PendingExport(context) }
    val export =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream")
        ) { uri ->
            if (uri != null)
                scope.launch {
                    runCatching {
                            withContext(Dispatchers.IO) {
                                context.contentResolver.openOutputStream(uri)?.use {
                                    pendingExport.copyTo(it)
                                } ?: error("Could not write the export")
                                pendingExport.clear()
                            }
                        }
                        .onSuccess { vm.notify("Export saved") }
                        .onFailure { vm.notify(it.message ?: "Export failed") }
                }
            else scope.launch(Dispatchers.IO) { pendingExport.clear() }
        }
    fun launchExport(text: String, name: String) {
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { pendingExport.prepare(text) } }
                .onSuccess { export.launch(name) }
                .onFailure { vm.notify(it.message ?: "Could not prepare the export") }
        }
    }
    val restore =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null)
                scope.launch {
                    runCatching {
                            withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(uri)?.use { input ->
                                    val out = java.io.ByteArrayOutputStream()
                                    val buffer = ByteArray(16384)
                                    while (true) {
                                        val n = input.read(buffer)
                                        if (n < 0) break
                                        require(out.size() + n <= 2 * 1024 * 1024) {
                                            "Backup exceeds 2 MB"
                                        }
                                        out.write(buffer, 0, n)
                                    }
                                    out.toString("UTF-8")
                                } ?: error("Could not read the backup")
                            }
                        }
                        .onSuccess { vm.importBackup(it) }
                        .onFailure { vm.notify(it.message ?: "Invalid backup") }
                }
        }
    val importPdf =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null)
                vm.importPdf(uri, selected) { item, file ->
                    selectedJson = item.json().toString()
                    readerPath = file.path
                }
        }
    BackHandler(enabled = selected != null || destination != Destination.HOME) {
        if (readerPath != null) readerPath = null
        else if (selected != null) selectedJson = null
        else if (destination == Destination.PERSONALIZE) navigate(Destination.SETTINGS)
        else navigate(Destination.HOME)
    }
    val tabs =
        listOf(
            Destination.HOME to Icons.Outlined.Home,
            Destination.SEARCH to Icons.Outlined.Search,
            Destination.LIBRARY to Icons.Outlined.Bookmarks,
            Destination.PROJECTS to Icons.Outlined.FolderOpen,
        )
    fun open(item: ResearchItem) {
        selectedJson = item.json().toString()
    }
    fun external(url: String) {
        runCatching {
                require(Uri.parse(url).scheme in listOf("https", "http"))
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            .onFailure { vm.notify("Could not open this source") }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val wide = maxWidth >= 840.dp
        if (readerPath != null && selected != null) {
            Box(Modifier.fillMaxSize()) {
                ReaderScreen(selected, java.io.File(readerPath!!), vm) { readerPath = null }
                SnackbarHost(
                    snackbar,
                    Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
                )
            }
        } else
            Row(Modifier.fillMaxSize()) {
                if (wide && selected == null)
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxHeight().statusBarsPadding(),
                        header = {
                            Text(
                                "◎",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(16.dp),
                            )
                        },
                    ) {
                        tabs.forEach { (tab, icon) ->
                            NavigationRailItem(
                                selected = destination == tab,
                                onClick = { navigate(tab) },
                                icon = { Icon(icon, null) },
                                label = { Text(tab.label) },
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        NavigationRailItem(
                            selected = destination == Destination.SETTINGS,
                            onClick = { navigate(Destination.SETTINGS) },
                            icon = { Icon(Icons.Outlined.Settings, "Settings") },
                            label = { Text("Settings") },
                        )
                    }
                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(snackbar) },
                    topBar = {
                        if (selected == null)
                            Surface(color = MaterialTheme.colorScheme.background) {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .statusBarsPadding()
                                        .padding(horizontal = 24.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "◎",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        "overthink",
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.padding(start = 8.dp),
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick = { navigate(Destination.ASSISTANT) }) {
                                        Icon(Icons.Outlined.AutoAwesome, "Research assistant")
                                    }
                                    IconButton(onClick = { navigate(Destination.SETTINGS) }) {
                                        Icon(
                                            Icons.Outlined.AccountCircle,
                                            account?.username ?: "Account and settings",
                                        )
                                    }
                                }
                            }
                    },
                    bottomBar = {
                        if (!wide && selected == null && destination in tabs.map { it.first })
                            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                tabs.forEach { (tab, icon) ->
                                    NavigationBarItem(
                                        selected = destination == tab,
                                        onClick = { navigate(tab) },
                                        icon = { Icon(icon, null) },
                                        label = { Text(tab.label) },
                                    )
                                }
                            }
                    },
                ) { padding ->
                    Box(
                        Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        if (selected != null) {
                            screenStates.SaveableStateProvider("detail:${selected.id}") {
                                DetailScreen(
                                    selected,
                                    vm,
                                    onBack = { selectedJson = null },
                                    onSource = { external(selected.url) },
                                    onRead = {
                                        vm.documents.cached(selected.id)?.let {
                                            readerPath = it.path
                                        } ?: vm.openPdf(selected) { readerPath = it.path }
                                    },
                                    onImport = { importPdf.launch(arrayOf("application/pdf")) },
                                    onAssistant = {
                                        navigate(Destination.ASSISTANT)
                                        selectedJson = null
                                    },
                                )
                            }
                        } else
                            AnimatedContent(
                                targetState = destination,
                                transitionSpec = {
                                    (fadeIn(tween(preferences.motionMillis)) +
                                        slideInHorizontally(tween(preferences.motionMillis)) {
                                            if (preferences.reduceMotion) 0 else it / 30
                                        }) togetherWith fadeOut(tween(preferences.motionMillis / 2))
                                },
                                modifier = Modifier.widthIn(max = 1180.dp).fillMaxSize(),
                                label = "destination",
                            ) { screen ->
                                screenStates.SaveableStateProvider(screen.name) {
                                    when (screen) {
                                        Destination.HOME ->
                                            HomeScreen(
                                                vm,
                                                { q ->
                                                    vm.query(q)
                                                    navigate(Destination.SEARCH)
                                                    vm.search()
                                                },
                                                { navigate(it) },
                                                ::open,
                                            )
                                        Destination.SEARCH ->
                                            DiscoverScreen(
                                                vm,
                                                ::open,
                                                { navigate(Destination.COMPARE) },
                                            )
                                        Destination.LIBRARY ->
                                            LibraryScreen(
                                                vm,
                                                ::open,
                                                { importPdf.launch(arrayOf("application/pdf")) },
                                                { items ->
                                                    val ext =
                                                        when (preferences.exportFormat) {
                                                            "RIS" -> "ris"
                                                            "Markdown" -> "md"
                                                            else -> "bib"
                                                        }
                                                    launchExport(
                                                        vm.exportSources(items),
                                                        "overthink-library.$ext",
                                                    )
                                                },
                                            )
                                        Destination.PROJECTS -> ProjectsScreen(vm, ::open)
                                        Destination.ASSISTANT -> AssistantScreen(vm)
                                        Destination.SETTINGS ->
                                            SettingsScreen(
                                                vm,
                                                {
                                                    launchExport(
                                                        workspace.json().toString(2),
                                                        "overthink-backup.json",
                                                    )
                                                },
                                                {
                                                    restore.launch(
                                                        arrayOf(
                                                            "application/json",
                                                            "application/octet-stream",
                                                        )
                                                    )
                                                },
                                                { navigate(Destination.PERSONALIZE) },
                                            )
                                        Destination.COMPARE -> ComparisonScreen(vm, ::open)
                                        Destination.PERSONALIZE ->
                                            PersonalizationScreen(vm) {
                                                navigate(Destination.SETTINGS)
                                            }
                                    }
                                }
                            }
                        if (busy)
                            Surface(Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LinearProgressIndicator(Modifier.weight(1f))
                                    IconButton(onClick = { vm.cancelOperation() }) {
                                        Icon(Icons.Outlined.Close, "Cancel current operation")
                                    }
                                }
                            }
                    }
                }
            }
    }
}
