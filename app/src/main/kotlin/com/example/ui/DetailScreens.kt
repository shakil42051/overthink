package com.example.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.io.File

@Composable
fun DetailScreen(
    item: ResearchItem,
    vm: OverthinkViewModel,
    onBack: () -> Unit,
    onSource: () -> Unit,
    onRead: () -> Unit,
    onImport: () -> Unit,
    onAssistant: () -> Unit,
) {
    val w by vm.workspace.collectAsStateWithLifecycle()
    val saved = w.items.find { it.id == item.id }
    val preferences = LocalAppPreferences.current
    val clipboard = LocalClipboardManager.current
    var citation by remember { mutableStateOf(false) }
    var collection by
        rememberSaveable(item.id) {
            mutableStateOf(saved?.collection ?: preferences.defaultCollection)
        }
    var tags by
        rememberSaveable(item.id) { mutableStateOf(saved?.tags?.joinToString(", ").orEmpty()) }
    var note by rememberSaveable(item.id) { mutableStateOf("") }
    val busy by vm.busy.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.widthIn(max = 900.dp).fillMaxSize(),
        contentPadding = PaddingValues(preferences.gutter),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
                Spacer(Modifier.weight(1f))
                Tag(item.source)
            }
        }
        item {
            Text(item.title, style = MaterialTheme.typography.headlineLarge)
            if (item.authors.isNotBlank())
                Text(
                    item.authors,
                    Modifier.padding(top = 14.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            item.year?.let {
                Text(
                    "Published $it",
                    Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.kind == ItemKind.PAPER) {
                    Button(
                        onClick = onRead,
                        enabled =
                            !busy &&
                                (item.pdfUrl.isNotBlank() || vm.documents.cached(item.id) != null),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.MenuBook, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (vm.documents.cached(item.id) != null) "Read offline PDF"
                            else "Download & read PDF"
                        )
                    }
                    if (item.pdfUrl.isBlank() && vm.documents.cached(item.id) == null)
                        Text(
                            "No direct PDF is provided by this source. Open the publisher page or attach a PDF you have access to.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.url.isNotBlank())
                        OutlinedButton(onClick = onSource) { Text("Original source") }
                    if (item.kind == ItemKind.PAPER)
                        OutlinedButton(onClick = { citation = true }) { Text("Cite") }
                }
            }
        }
        item {
            PaperSurface {
                Eyebrow(if (item.kind == ItemKind.PAPER) "Abstract" else "About this source")
                Text(
                    item.summary.ifBlank {
                        "No abstract or description was supplied by this provider."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "Source: ${item.source}. This is provider metadata, not a reconstruction of the full paper.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (item.doi.isNotBlank() || item.citations != null)
            item {
                PaperSurface {
                    Eyebrow("Bibliographic record")
                    if (item.doi.isNotBlank()) Text("DOI  ${item.doi}")
                    item.citations?.let { Text("$it citations reported by ${item.source}") }
                    Text(
                        "Peer review status: not verified by Overthink",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        item {
            PaperSurface {
                Eyebrow("Keep it connected")
                OutlinedTextField(
                    collection,
                    { collection = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("Collection") },
                    singleLine = true,
                )
                Button(
                    enabled = !busy,
                    onClick = {
                        vm.save(
                            (saved ?: item).copy(
                                collection =
                                    collection.trim().take(80).ifBlank {
                                        preferences.defaultCollection
                                    }
                            ),
                            useDefaultCollection = false,
                        )
                    },
                ) {
                    Icon(
                        if (saved != null) Icons.Outlined.BookmarkAdded
                        else Icons.Outlined.BookmarkAdd,
                        null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (saved != null) "Update collection" else "Save to library")
                }
                if (item.kind == ItemKind.PAPER)
                    TextButton(onClick = onImport, enabled = !busy) {
                        Text("Attach a PDF from your device")
                    }
                if (saved != null) {
                    ToggleSetting(
                        "Favorite source",
                        "Keep this source in your favorites.",
                        saved.favorite,
                    ) { value ->
                        vm.organize(item.id) { it.copy(favorite = value) }
                    }
                    ChoiceSetting(
                        "Reading status",
                        saved.readingStatus.label,
                        ReadingStatus.entries.map { it.label },
                    ) { value ->
                        vm.organize(item.id) {
                            it.copy(
                                readingStatus =
                                    ReadingStatus.entries.first { status -> status.label == value }
                            )
                        }
                    }
                    OutlinedTextField(
                        tags,
                        { tags = it.take(1000) },
                        Modifier.fillMaxWidth(),
                        label = { Text("Tags, separated by commas") },
                        supportingText = { Text("Up to 20 tags · 40 characters each") },
                    )
                    TextButton(
                        enabled = tags != saved.tags.joinToString(", "),
                        onClick = {
                            vm.organize(item.id) {
                                it.copy(
                                    tags =
                                        tags
                                            .split(',')
                                            .map { tag -> tag.trim().take(40) }
                                            .filter { tag -> tag.isNotBlank() }
                                            .distinct()
                                            .take(20)
                                )
                            }
                        },
                    ) {
                        Text("Save tags")
                    }
                }
            }
        }
        item { SectionTitle("Your notes", "Capture a question, quote, or connection.") }
        item {
            OutlinedTextField(
                note,
                { note = it },
                Modifier.fillMaxWidth(),
                label = { Text("What stands out?") },
                minLines = 3,
            )
            Button(
                onClick = {
                    vm.annotate(item, note)
                    note = ""
                },
                enabled = note.isNotBlank() && !busy,
                modifier = Modifier.padding(top = 10.dp),
            ) {
                Text("Save note")
            }
        }
        items(w.notes.filter { it.itemId == item.id }, key = { it.id }) { n ->
            NoteCard(n, { vm.deleteNote(n.id) }, { text -> vm.editNote(n, text) })
        }
        if (item.summary.isNotBlank())
            item {
                OutlinedButton(
                    onClick = {
                        vm.selectAssistantSource(item)
                        onAssistant()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.AutoAwesome, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Explore this abstract with AI")
                }
            }
    }
    if (citation)
        AlertDialog(
            onDismissRequest = { citation = false },
            title = { Text("BibTeX citation") },
            text = {
                Column {
                    Text(
                        "A metadata-based @misc entry. Check the publisher's record before submission.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        item.bibtex(),
                        Modifier.padding(top = 12.dp).verticalScroll(rememberScrollState()),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(item.bibtex()))
                        vm.notify("BibTeX copied")
                    }
                ) {
                    Text("Copy BibTeX")
                }
            },
            dismissButton = { TextButton(onClick = { citation = false }) { Text("Close") } },
        )
}

@Composable
fun NoteCard(note: Note, onDelete: () -> Unit, onEdit: (String) -> Unit) {
    var edit by remember { mutableStateOf(false) }
    PaperSurface {
        if (note.page > 0) Tag("Page ${note.page}")
        Text(note.text)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = { edit = true }) { Icon(Icons.Outlined.Edit, "Edit note") }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, "Delete note") }
        }
    }
    if (edit) {
        var text by remember(note.id) { mutableStateOf(note.text) }
        AlertDialog(
            onDismissRequest = { edit = false },
            title = { Text("Edit your note") },
            text = {
                OutlinedTextField(
                    text,
                    { text = it },
                    Modifier.fillMaxWidth(),
                    minLines = 4,
                    label = { Text("Your note") },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = text.isNotBlank(),
                    onClick = {
                        onEdit(text)
                        edit = false
                    },
                ) {
                    Text("Save changes")
                }
            },
            dismissButton = { TextButton(onClick = { edit = false }) { Text("Cancel") } },
        )
    }
}

private data class RenderedPage(
    val bitmap: Bitmap? = null,
    val total: Int = 0,
    val error: String? = null,
)

@Composable
fun ReaderScreen(item: ResearchItem, file: File, vm: OverthinkViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val p = LocalAppPreferences.current
    val account by vm.account.collectAsStateWithLifecycle()
    val reading = remember { context.getSharedPreferences("overthink_reading", 0) }
    val progressKey = "${account?.id ?: "guest"}:${item.id}"
    var page by
        rememberSaveable(item.id) {
            mutableIntStateOf(
                if (p.readerRememberPage) reading.getInt(progressKey, 0).coerceAtLeast(0) else 0
            )
        }
    var addNote by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    var jump by remember { mutableStateOf(false) }
    var focus by rememberSaveable(item.id) { mutableStateOf(p.readerFocus) }
    var retry by remember { mutableIntStateOf(0) }
    val rendered by
        produceState(RenderedPage(), file.path, page, retry) {
            value = RenderedPage()
            value =
                try {
                    val result = vm.documents.page(file, page)
                    RenderedPage(result.first, result.second)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    RenderedPage(error = e.message ?: "Could not render this PDF")
                }
        }
    var scale by remember(page, p.readerZoom) { mutableFloatStateOf(p.readerZoom) }
    var offset by remember(page) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 4f)
        val xLimit = viewport.width * (scale - 1f) / 2
        val yLimit = viewport.height * (scale - 1f) / 2
        offset =
            Offset(
                (offset.x + pan.x).coerceIn(-xLimit, xLimit),
                (offset.y + pan.y).coerceIn(-yLimit, yLimit),
            )
    }
    val w by vm.workspace.collectAsStateWithLifecycle()
    LaunchedEffect(page, p.readerRememberPage) {
        if (p.readerRememberPage) reading.edit().putInt(progressKey, page).apply()
    }
    LaunchedEffect(rendered.total) {
        if (rendered.total > 0 && page >= rendered.total) page = rendered.total - 1
    }
    val pageColorFilter =
        remember(p.readerTheme) {
            when (p.readerTheme) {
                "Sepia" ->
                    androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                        androidx.compose.ui.graphics.ColorMatrix(
                            floatArrayOf(
                                .9f,
                                0f,
                                0f,
                                0f,
                                22f,
                                0f,
                                .85f,
                                0f,
                                0f,
                                18f,
                                0f,
                                0f,
                                .70f,
                                0f,
                                24f,
                                0f,
                                0f,
                                0f,
                                1f,
                                0f,
                            )
                        )
                    )
                "Night" ->
                    androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                        androidx.compose.ui.graphics.ColorMatrix(
                            floatArrayOf(
                                -.86f,
                                0f,
                                0f,
                                0f,
                                230f,
                                0f,
                                -.86f,
                                0f,
                                0f,
                                233f,
                                0f,
                                0f,
                                -.86f,
                                0f,
                                230f,
                                0f,
                                0f,
                                0f,
                                1f,
                                0f,
                            )
                        )
                    )
                else -> null
            }
        }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BackHandler { if (settings) settings = false else onClose() }
        val readerView = androidx.compose.ui.platform.LocalView.current
        DisposableEffect(readerView, p.readerKeepAwake) {
            val previous = readerView.keepScreenOn
            readerView.keepScreenOn = p.readerKeepAwake
            onDispose { readerView.keepScreenOn = previous }
        }
        Scaffold(
            Modifier.fillMaxSize().safeDrawingPadding(),
            topBar = {
                if (!focus)
                    Surface {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onClose) {
                                Icon(Icons.Outlined.Close, "Close reader")
                            }
                            Text(
                                item.title,
                                Modifier.weight(1f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            IconButton(onClick = { showNotes = !showNotes }) {
                                Icon(Icons.AutoMirrored.Outlined.Notes, "Page notes")
                            }
                            IconButton(onClick = { settings = true }) {
                                Icon(Icons.Outlined.Tune, "Reader settings")
                            }
                            IconButton(
                                onClick = {
                                    runCatching {
                                            val uri =
                                                FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.files",
                                                    file,
                                                )
                                            context.startActivity(
                                                Intent.createChooser(
                                                    Intent(Intent.ACTION_SEND)
                                                        .setType("application/pdf")
                                                        .putExtra(Intent.EXTRA_STREAM, uri)
                                                        .addFlags(
                                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                        ),
                                                    "Share PDF",
                                                )
                                            )
                                        }
                                        .onFailure { vm.notify("Could not share the PDF") }
                                }
                            ) {
                                Icon(Icons.Outlined.Share, "Share PDF")
                            }
                        }
                    }
            },
            bottomBar = {
                if (!focus)
                    Surface(tonalElevation = 3.dp) {
                        Column {
                            if (showNotes)
                                LazyColumn(
                                    Modifier.heightIn(max = 180.dp).fillMaxWidth(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(
                                        w.notes.filter {
                                            it.itemId == item.id && it.page == page + 1
                                        }
                                    ) {
                                        Text(it.text)
                                    }
                                    item {
                                        TextButton(onClick = { addNote = true }) {
                                            Text("Add a note to page ${page + 1}")
                                        }
                                    }
                                }
                            if (rendered.total > 0)
                                LinearProgressIndicator(
                                    progress = { (page + 1f) / rendered.total },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                IconButton(onClick = { page-- }, enabled = page > 0) {
                                    Icon(Icons.Outlined.ChevronLeft, "Previous page")
                                }
                                TextButton(
                                    onClick = { jump = true },
                                    enabled = rendered.total > 0,
                                ) {
                                    Text(
                                        if (rendered.total > 0) "${page+1} / ${rendered.total}"
                                        else "Loading…"
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        scale = 1f
                                        offset = Offset.Zero
                                    }
                                ) {
                                    Text("Fit")
                                }
                                IconButton(onClick = { addNote = true }) {
                                    Icon(Icons.AutoMirrored.Outlined.NoteAdd, "Add page note")
                                }
                                IconButton(
                                    onClick = { page++ },
                                    enabled = page + 1 < rendered.total,
                                ) {
                                    Icon(Icons.Outlined.ChevronRight, "Next page")
                                }
                            }
                        }
                    }
            },
        ) { padding ->
            Box(
                Modifier.fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clipToBounds()
                    .onSizeChanged { viewport = it }
                    .transformable(transform)
                    .pointerInput(p.readerDoubleTap) {
                        detectTapGestures(
                            onTap = { focus = !focus },
                            onDoubleTap =
                                if (p.readerDoubleTap)
                                    ({
                                        scale = if (scale > 1.1f) 1f else 2f
                                        offset = Offset.Zero
                                    })
                                else null,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                rendered.bitmap?.let {
                    Image(
                        it.asImageBitmap(),
                        "Original PDF, page ${page+1}",
                        modifier =
                            Modifier.fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y,
                                ),
                        colorFilter = pageColorFilter,
                    )
                }
                if (rendered.error != null)
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(rendered.error!!)
                        TextButton(onClick = { retry++ }) { Text("Retry") }
                        TextButton(onClick = onClose) { Text("Close") }
                    }
                else if (rendered.bitmap == null) CircularProgressIndicator()
                if (focus) {
                    FilledTonalIconButton(
                        onClick = { focus = false },
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    ) {
                        Icon(Icons.Outlined.Visibility, "Show reader controls")
                    }
                    if (rendered.total > 0)
                        Surface(
                            Modifier.align(Alignment.BottomCenter).padding(12.dp),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            TextButton(onClick = { jump = true }) {
                                Text("${page+1} / ${rendered.total}")
                            }
                        }
                }
            }
        }
        if (settings)
            AlertDialog(
                onDismissRequest = { settings = false },
                title = { Text("Reading comfort") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        ChoiceSetting(
                            "PDF appearance",
                            p.readerTheme,
                            listOf("Original", "Sepia", "Night"),
                        ) { value ->
                            vm.customize { it.copy(readerTheme = value) }
                        }
                        ToggleSetting(
                            "Keep screen awake",
                            "While reading this document.",
                            p.readerKeepAwake,
                        ) { value ->
                            vm.customize { it.copy(readerKeepAwake = value) }
                        }
                        ToggleSetting(
                            "Focus mode",
                            "Tap the page to show or hide controls.",
                            focus,
                        ) {
                            focus = it
                        }
                        Text(
                            "Pinch to zoom · tap the page count to jump.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                confirmButton = { TextButton(onClick = { settings = false }) { Text("Done") } },
            )
        if (jump) {
            var target by remember { mutableStateOf((page + 1).toString()) }
            AlertDialog(
                onDismissRequest = { jump = false },
                title = { Text("Go to page") },
                text = {
                    OutlinedTextField(
                        target,
                        { target = it.filter(Char::isDigit).take(6) },
                        label = { Text("Page 1–${rendered.total}") },
                        singleLine = true,
                        keyboardOptions =
                            androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = target.toIntOrNull() in 1..rendered.total,
                        onClick = {
                            page = target.toInt() - 1
                            jump = false
                        },
                    ) {
                        Text("Go")
                    }
                },
                dismissButton = { TextButton(onClick = { jump = false }) { Text("Cancel") } },
            )
        }
        if (addNote) {
            var text by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { addNote = false },
                title = { Text("Note · page ${page+1}") },
                text = {
                    OutlinedTextField(
                        text,
                        { text = it },
                        label = { Text("Your observation") },
                        minLines = 3,
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = text.isNotBlank(),
                        onClick = {
                            vm.annotate(item, text, page + 1)
                            addNote = false
                            showNotes = true
                        },
                    ) {
                        Text("Save note")
                    }
                },
                dismissButton = { TextButton(onClick = { addNote = false }) { Text("Cancel") } },
            )
        }
    }
}

@Composable
fun AssistantScreen(vm: OverthinkViewModel) {
    val w by vm.workspace.collectAsStateWithLifecycle()
    val chat by vm.chat.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var question by rememberSaveable { mutableStateOf("") }
    var sourceId by rememberSaveable { mutableStateOf(vm.assistantSource.value) }
    val sources = w.items.filter { it.summary.isNotBlank() && it.kind == ItemKind.PAPER }
    val selected = sources.find { it.id == sourceId }
    LazyColumn(
        Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(LocalAppPreferences.current.gutter),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Eyebrow("A thinking partner")
            Text(
                "Ask better questions.\nStay close to the source.",
                style = MaterialTheme.typography.headlineLarge,
            )
        }
        item {
            Text(
                "Answers use only the selected abstract. Full-paper analysis and literature-wide conclusions require additional evidence.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            PaperSurface {
                Text("Choose your source", style = MaterialTheme.typography.titleMedium)
                sources.forEach { p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = sourceId == p.id, onClick = { sourceId = p.id })
                        Text(p.title, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (sources.isEmpty()) Text("Save a paper with an abstract to your library first.")
            }
        }
        items(chat.filter { it.sourceId == sourceId }) { message ->
            PaperSurface {
                Eyebrow(message.role)
                Text(message.text)
            }
        }
        item {
            OutlinedTextField(
                question,
                { question = it },
                Modifier.fillMaxWidth(),
                label = { Text("What would you like to understand?") },
                minLines = 3,
            )
            Button(
                onClick = {
                    selected?.let {
                        vm.ask(question, listOf(it))
                        question = ""
                    }
                },
                enabled = !busy && question.isNotBlank() && selected != null,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Icon(Icons.Outlined.AutoAwesome, null)
                Spacer(Modifier.width(8.dp))
                Text(if (busy) "Working…" else "Ask about this source")
            }
        }
    }
}

@Composable
fun ComparisonScreen(vm: OverthinkViewModel, onOpen: (ResearchItem) -> Unit) {
    val papers by vm.comparison.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(LocalAppPreferences.current.gutter),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            SectionTitle(
                "Put the evidence\nside by side",
                "Compare reported metadata. Missing information stays unknown.",
            )
        }
        if (papers.isEmpty())
            item {
                EmptyState(
                    "Choose papers to compare",
                    "Use Compare on a search result or saved paper. You can select up to three sources.",
                )
            }
        items(papers, key = { it.id }) { p ->
            PaperSurface {
                Eyebrow(p.source)
                Text(p.title, style = MaterialTheme.typography.titleLarge)
                Text(p.authors)
                HorizontalDivider()
                Text("Year: ${p.year?:"Not reported"}")
                Text("Citations: ${p.citations?:"Not reported"}")
                Text("Direct PDF: ${if(p.pdfUrl.isNotBlank())"Available"else "Not reported"}")
                Text("Peer review: Not verified")
                Text(
                    p.summary.ifBlank { "No abstract supplied" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row {
                    TextButton(onClick = { onOpen(p) }) { Text("View source") }
                    TextButton(onClick = { vm.compare(p) }) { Text("Remove") }
                }
            }
        }
    }
}
