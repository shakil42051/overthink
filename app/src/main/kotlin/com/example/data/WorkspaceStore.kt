package com.example.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * A separate database for every account/server pair; the guest workspace is never merged
 * implicitly.
 */
class WorkspaceStore(private val context: Context) {
    private var db = open("guest")
    private val state = MutableStateFlow(load())
    val workspace = state.asStateFlow()

    private fun open(scope: String) =
        object : SQLiteOpenHelper(context, "overthink_v2_${digest(scope)}.db", null, 1) {
                override fun onCreate(db: SQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE workspace (id INTEGER PRIMARY KEY CHECK(id=1), body TEXT NOT NULL)"
                    )
                }

                override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    error("A data-preserving migration is required")
                }
            }
            .writableDatabase

    private fun load() =
        db.rawQuery("SELECT body FROM workspace WHERE id=1", null).use {
            if (it.moveToFirst()) Workspace.from(JSONObject(it.getString(0))) else Workspace()
        }

    @Synchronized
    fun switch(scope: String) {
        db.close()
        db = open(scope)
        state.value = load()
    }

    @Synchronized
    fun update(transform: (Workspace) -> Workspace) {
        val next = transform(state.value)
        db.beginTransaction()
        try {
            db.execSQL(
                "INSERT OR REPLACE INTO workspace(id, body) VALUES(1, ?)",
                arrayOf(next.json().toString()),
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        state.value = next
    }

    fun save(item: ResearchItem) = update {
        it.copy(items = listOf(item) + it.items.filterNot { p -> p.id == item.id })
    }

    fun remove(id: String) = update { w ->
        w.copy(
            items = w.items.filterNot { it.id == id },
            notes = w.notes.filterNot { it.itemId == id },
            projects = w.projects.map { it.copy(paperIds = it.paperIds - id) },
        )
    }

    fun project(project: Project) = update {
        it.copy(projects = listOf(project) + it.projects.filterNot { p -> p.id == project.id })
    }

    fun note(note: Note) = update { it.copy(notes = listOf(note) + it.notes) }

    fun merge(other: Workspace) = update { w ->
        Workspace(
            (w.items + other.items).distinctBy { it.id },
            (w.projects + other.projects).distinctBy { it.id },
            (w.notes + other.notes).distinctBy { it.id },
            (w.history + other.history).distinct().take(30),
            (w.messages + other.messages).distinct().takeLast(100),
        )
    }

    fun importLegacy(): Int {
        val file = context.getDatabasePath("research_verse_database")
        if (!file.exists()) return 0
        val imported = mutableListOf<ResearchItem>()
        val notes = mutableListOf<Note>()
        val projects = mutableListOf<Project>()
        val history = mutableListOf<String>()
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { old ->
            old.rawQuery("SELECT * FROM research_papers WHERE id NOT LIKE 'seed_%'", null).use { c
                ->
                fun field(name: String) =
                    c.getColumnIndex(name).let {
                        if (it < 0 || c.isNull(it)) "" else c.getString(it)
                    }
                while (c.moveToNext()) imported +=
                    ResearchItem(
                        field("id"),
                        field("title"),
                        field("authorsDisplay"),
                        field("year").toIntOrNull(),
                        field("fullAbstract").ifBlank { field("abstractPreview") },
                        field("doi").takeIf { it.isNotBlank() }?.let { "https://doi.org/$it" }
                            ?: field("pdfUrl"),
                        field("pdfUrl"),
                        field("doi"),
                        field("sourcePlatform"),
                        collection = field("collectionName").ifBlank { "Imported" },
                    )
            }
            val ids = imported.map { it.id }.toSet()
            fun tableExists(name: String) =
                old.rawQuery(
                        "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                        arrayOf(name),
                    )
                    .use { it.moveToFirst() }
            if (tableExists("research_notes"))
                old.rawQuery("SELECT id,targetId,title,content,timestamp FROM research_notes", null)
                    .use { c ->
                        while (c.moveToNext()) if (c.getString(1) in ids)
                            notes.add(
                                Note(
                                    c.getString(0),
                                    c.getString(1),
                                    listOf(c.getString(2), c.getString(3))
                                        .filter { it.isNotBlank() }
                                        .joinToString("\n\n"),
                                    createdAt = c.getLong(4),
                                )
                            )
                    }
            if (tableExists("academic_highlights"))
                old.rawQuery(
                        "SELECT id,paperId,selectedText,pageNumber,noteOrComment,createdAt FROM academic_highlights WHERE id NOT LIKE 'hl_seed_%'",
                        null,
                    )
                    .use { c ->
                        while (c.moveToNext()) if (c.getString(1) in ids)
                            notes.add(
                                Note(
                                    c.getString(0),
                                    c.getString(1),
                                    listOf("Imported quote: ${c.getString(2)}", c.getString(4))
                                        .filter { it.isNotBlank() }
                                        .joinToString("\n\n"),
                                    c.getInt(3).coerceAtLeast(0),
                                    c.getLong(5),
                                )
                            )
                    }
            if (tableExists("projects"))
                old.rawQuery(
                        "SELECT id,title,problemStatement,deadline,tasksJson FROM projects WHERE id != 'proj_default_1'",
                        null,
                    )
                    .use { c ->
                        while (c.moveToNext()) projects.add(
                            Project(
                                c.getString(0),
                                c.getString(1),
                                c.getString(2),
                                c.getString(3),
                                parseLegacyTasks(c.getString(4)),
                            )
                        )
                    }
            if (tableExists("search_history"))
                old.rawQuery(
                        "SELECT query FROM search_history ORDER BY timestamp DESC LIMIT 30",
                        null,
                    )
                    .use { c -> while (c.moveToNext()) history.add(c.getString(0)) }
        }
        merge(Workspace(items = imported, projects = projects, notes = notes, history = history))
        return imported.size
    }

    fun close() = db.close()
}

fun digest(value: String) =
    MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") {
        "%02x".format(it)
    }
