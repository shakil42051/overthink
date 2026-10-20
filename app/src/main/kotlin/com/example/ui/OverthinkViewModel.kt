package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject

data class DiscoveryState(
    val query: String = "",
    val kind: ItemKind = ItemKind.PAPER,
    val filter: SearchFilter = SearchFilter(),
    val page: Int = 1,
    val items: List<ResearchItem> = emptyList(),
    val statuses: List<ProviderOutcome> = emptyList(),
    val loading: Boolean = false,
    val searched: Boolean = false,
    val more: Boolean = false,
)

data class Account(val id: String, val username: String)

class OverthinkViewModel(app: Application) : AndroidViewModel(app) {
    val store = WorkspaceStore(app)
    val workspace = store.workspace
    val documents = DocumentStore(app)
    val backend = BackendClient()
    private val vault = SessionVault(app)
    private val prefs = app.getSharedPreferences("overthink_preferences", 0)
    private val customization = PreferencesStore(app)
    val preferences = customization.preferences

    private fun initialDiscovery() =
        preferences.value.let {
            DiscoveryState(
                kind = ItemKind.valueOf(it.searchKind),
                filter = SearchFilter(it.searchProvider, it.searchYear, it.searchPdfOnly),
            )
        }

    val discovery = MutableStateFlow(initialDiscovery())
    val account = MutableStateFlow<Account?>(null)
    val events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val busy = MutableStateFlow(false)
    val chat =
        workspace
            .map { it.messages }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val assistantSource = MutableStateFlow<String?>(null)
    val comparison = MutableStateFlow<List<ResearchItem>>(emptyList())
    private var searchJob: Job? = null
    private var operation: Job? = null
    var searchGateway: SearchGateway = ResearchApi()
    private var page = 1

    init {
        backend.baseUrl = prefs.getString("server", "").orEmpty()
        vault.read()?.let { raw ->
            runCatching {
                val j = JSONObject(raw)
                backend.token = j.getString("token")
                account.value = Account(j.getString("id"), j.getString("username"))
                store.switch("${backend.baseUrl}|${j.getString("id")}")
            }
        }
    }

    fun notify(message: String) {
        events.tryEmit(message)
    }

    fun query(q: String) {
        discovery.update { it.copy(query = q) }
    }

    fun category(kind: ItemKind) {
        searchJob?.cancel()
        discovery.value = initialDiscovery().copy(query = discovery.value.query, kind = kind)
        if (discovery.value.query.isNotBlank()) search()
    }

    fun filters(filter: SearchFilter) {
        discovery.update { it.copy(filter = filter) }
        if (discovery.value.searched) search()
    }

    fun search(more: Boolean = false) {
        val current = discovery.value
        if (current.query.isBlank()) return
        searchJob?.cancel()
        val targetPage = if (more) page + 1 else 1
        searchJob =
            viewModelScope.launch {
                discovery.update {
                    it.copy(
                        loading = true,
                        searched = true,
                        items = if (more) it.items else emptyList(),
                        statuses = emptyList(),
                    )
                }
                try {
                    val result =
                        searchGateway.search(
                            current.query,
                            current.kind,
                            targetPage,
                            current.filter,
                        )
                    ensureActive()
                    page = targetPage
                    discovery.update {
                        it.copy(
                            loading = false,
                            page = page,
                            items =
                                (if (more) it.items + result.items else result.items).distinctBy { p
                                    ->
                                    p.id
                                },
                            statuses = result.statuses,
                            more = result.canLoadMore,
                        )
                    }
                    if (preferences.value.rememberHistory)
                        withContext(Dispatchers.IO) {
                            store.update {
                                if (!preferences.value.rememberHistory) it
                                else
                                    it.copy(
                                        history =
                                            (listOf(current.query.trim()) + it.history)
                                                .distinct()
                                                .take(30)
                                    )
                            }
                        }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    discovery.update { it.copy(loading = false) }
                    notify(e.message ?: "Search failed")
                }
            }
    }

    internal suspend fun awaitSearchIdle() {
        searchJob?.join()
    }

    fun save(item: ResearchItem, useDefaultCollection: Boolean = true) = work {
        val next =
            if (workspace.value.items.any { it.id == item.id }) item
            else
                item.copy(
                    collection =
                        if (useDefaultCollection && item.collection == "Reading list")
                            preferences.value.defaultCollection
                        else item.collection
                )
        store.save(next)
        notify("Saved to ${next.collection}")
    }

    fun customize(change: (AppPreferences) -> AppPreferences) {
        val old = preferences.value
        customization.update(change)
        val next = preferences.value
        if (!discovery.value.searched)
            discovery.update {
                it.copy(
                    kind = ItemKind.valueOf(next.searchKind),
                    filter = SearchFilter(next.searchProvider, next.searchYear, next.searchPdfOnly),
                )
            }
        if (old.rememberHistory && !next.rememberHistory) clearHistory()
    }

    fun resetCustomization() {
        searchJob?.cancel()
        page = 1
        customization.reset()
        discovery.value = initialDiscovery()
    }

    fun clearHistory() =
        viewModelScope.launch(Dispatchers.IO) { store.update { it.copy(history = emptyList()) } }

    fun organize(id: String, change: (ResearchItem) -> ResearchItem) = work {
        store.update { w -> w.copy(items = w.items.map { if (it.id == id) change(it) else it }) }
    }

    fun editNote(note: Note, text: String) = work {
        require(text.isNotBlank()) { "A note cannot be empty" }
        store.update { w ->
            w.copy(
                notes = w.notes.map { if (it.id == note.id) it.copy(text = text.trim()) else it }
            )
        }
    }

    fun remove(item: ResearchItem) = work {
        store.remove(item.id)
        notify("Removed from library")
    }

    fun project(project: Project) = work { store.project(project) }

    fun deleteProject(id: String) = work {
        store.update { it.copy(projects = it.projects.filterNot { p -> p.id == id }) }
    }

    fun annotate(item: ResearchItem, text: String, page: Int = 0) = work {
        if (text.isNotBlank()) {
            if (workspace.value.items.none { it.id == item.id })
                store.save(item.copy(collection = preferences.value.defaultCollection))
            store.note(Note(itemId = item.id, text = text.trim(), page = page))
        }
    }

    fun deleteNote(id: String) = work {
        store.update { it.copy(notes = it.notes.filterNot { n -> n.id == id }) }
    }

    fun compare(item: ResearchItem) {
        if (comparison.value.size >= 3 && comparison.value.none { it.id == item.id }) {
            notify("Compare up to three papers at a time. Remove one to choose another.")
            return
        }
        comparison.update {
            if (it.any { p -> p.id == item.id }) it.filterNot { p -> p.id == item.id }
            else (it + item).take(3)
        }
    }

    fun theme(value: String) {
        customize { it.copy(theme = value) }
    }

    fun setServer(url: String) {
        require(account.value == null) { "Sign out before changing servers" }
        backend.baseUrl = url.trim().trimEnd('/')
        prefs.edit().putString("server", backend.baseUrl).apply()
    }

    fun authenticate(username: String, password: String, register: Boolean) = work {
        val result =
            backend.request(
                if (register) "/auth/register" else "/auth/login",
                JSONObject().put("username", username).put("password", password),
            )
        val user = result.getJSONObject("user")
        val auth = Account(user.getString("id"), user.getString("username"))
        backend.token = result.getString("token")
        vault.write(
            JSONObject()
                .put("token", backend.token)
                .put("id", auth.id)
                .put("username", auth.username)
                .toString()
        )
        searchJob?.cancelAndJoin()
        discovery.value = initialDiscovery()
        store.switch("${backend.baseUrl}|${auth.id}")
        account.value = auth
        assistantSource.value = null
        comparison.value = emptyList()
        notify("Signed in as ${auth.username}")
    }

    fun logout() = work {
        try {
            backend.request("/auth/logout", JSONObject())
        } catch (_: Exception) {
            notify(
                "Signed out locally. The server session will expire automatically if unreachable."
            )
        }
        searchJob?.cancelAndJoin()
        discovery.value = initialDiscovery()
        backend.token = ""
        vault.clear()
        account.value = null
        store.switch("guest")
        assistantSource.value = null
        comparison.value = emptyList()
    }

    fun deleteAccount(password: String) = work {
        backend.request("/account", JSONObject().put("password", password), "DELETE")
        searchJob?.cancelAndJoin()
        discovery.value = initialDiscovery()
        store.update { Workspace() }
        backend.token = ""
        vault.clear()
        account.value = null
        store.switch("guest")
        comparison.value = emptyList()
        assistantSource.value = null
        notify("Account and server workspace deleted")
    }

    fun selectAssistantSource(item: ResearchItem) {
        assistantSource.value = item.id
        if (workspace.value.items.none { it.id == item.id }) save(item)
    }

    fun synchronize() = work {
        check(account.value != null) { "Sign in to synchronize your workspace" }
        val remote = backend.request("/workspace")
        val scope = "${backend.baseUrl}|${account.value!!.id}"
        val known = prefs.getLong("revision:$scope", 0)
        val revision = remote.getLong("revision")
        if (revision != known && revision != 0L) {
            val unchanged =
                digest(workspace.value.json().toString()) ==
                    prefs.getString("baseline:$scope", null)
            val empty = workspace.value == Workspace()
            check(unchanged || empty) {
                "Sync conflict: local and server workspaces both changed. Export a backup before resolving. No data was overwritten."
            }
            val incoming = Workspace.from(remote.getJSONObject("workspace"))
            store.update { incoming }
        }
        val result =
            backend.request(
                "/workspace",
                JSONObject().put("revision", revision).put("workspace", workspace.value.json()),
                "PUT",
            )
        prefs
            .edit()
            .putLong("revision:$scope", result.getLong("revision"))
            .putString("baseline:$scope", digest(workspace.value.json().toString()))
            .apply()
        notify("Workspace synchronized")
    }

    fun restoreServerWorkspace() = work {
        check(account.value != null) { "Sign in first" }
        val remote = backend.request("/workspace")
        val incoming = Workspace.from(remote.getJSONObject("workspace"))
        val scope = "${backend.baseUrl}|${account.value!!.id}"
        store.update { incoming }
        prefs
            .edit()
            .putLong("revision:$scope", remote.getLong("revision"))
            .putString("baseline:$scope", digest(incoming.json().toString()))
            .apply()
        notify("Server workspace restored on this device")
    }

    fun ask(question: String, items: List<ResearchItem>) = work {
        require(question.isNotBlank()) { "Enter a question" }
        check(account.value != null) { "Connect your server and sign in to use AI" }
        require(items.isNotEmpty()) { "Save or select a paper with an abstract first" }
        val sourceId = items.first().id
        val history =
            org.json.JSONArray().apply {
                workspace.value.messages
                    .filter { it.sourceId == sourceId }
                    .takeLast(8)
                    .forEach { put(it.json()) }
            }
        store.update {
            it.copy(messages = (it.messages + ChatMessage("You", question, sourceId)).takeLast(100))
        }
        val context =
            org.json.JSONArray().apply {
                items
                    .filter { it.summary.isNotBlank() }
                    .take(3)
                    .forEach {
                        put(
                            JSONObject()
                                .put("id", it.id)
                                .put("title", it.title)
                                .put("text", it.summary)
                                .put("url", it.url)
                        )
                    }
            }
        val response =
            backend.request(
                "/assistant",
                JSONObject()
                    .put("question", question)
                    .put("sources", context)
                    .put("history", history),
            )
        store.update {
            it.copy(
                messages =
                    (it.messages + ChatMessage("Overthink", response.getString("answer"), sourceId))
                        .takeLast(100)
            )
        }
    }

    fun importPdf(uri: Uri, existing: ResearchItem? = null, onReady: (ResearchItem, File) -> Unit) =
        work {
            val title =
                getApplication<Application>()
                    .contentResolver
                    .query(
                        uri,
                        arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                        null,
                        null,
                        null,
                    )
                    ?.use { if (it.moveToFirst()) it.getString(0) else null }
                    ?.removeSuffix(".pdf") ?: "Imported document"
            val item =
                existing
                    ?: ResearchItem(
                        "local:${java.util.UUID.randomUUID()}",
                        title,
                        source = "Your device",
                        collection = preferences.value.defaultCollection,
                    )
            val file = documents.import(uri, item.id)
            if (existing == null) store.save(item)
            withContext(Dispatchers.Main) { onReady(item, file) }
        }

    fun openPdf(item: ResearchItem, onReady: (File) -> Unit) = work {
        val file = documents.download(item)
        withContext(Dispatchers.Main) { onReady(file) }
    }

    fun exportSources(items: List<ResearchItem>) = items.export(preferences.value.exportFormat)

    fun importBackup(raw: String) = work {
        val imported = Workspace.from(JSONObject(raw))
        store.merge(imported)
        notify("Backup merged into this workspace")
    }

    fun legacy() = work {
        notify("Imported ${store.importLegacy()} papers. The original database was preserved.")
    }

    fun cancelOperation() {
        operation?.cancel()
        notify("Operation cancelled")
    }

    private fun work(action: suspend () -> Unit): Job {
        if (busy.value) {
            notify("Please wait for the current operation")
            return Job().apply { complete() }
        }
        return viewModelScope
            .launch {
                busy.value = true
                try {
                    withContext(Dispatchers.IO) { action() }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    notify(e.message ?: "Operation failed. Please retry.")
                } finally {
                    busy.value = false
                }
            }
            .also { operation = it }
    }

    override fun onCleared() {
        store.close()
        super.onCleared()
    }
}
