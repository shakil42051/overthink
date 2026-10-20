package com.example.data

import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

enum class ItemKind {
    PAPER,
    DATASET,
    CODE,
}

enum class ReadingStatus(val label: String) {
    TO_READ("To read"),
    READING("Reading"),
    READ("Read"),
}

fun arxivIdentifier(query: String): String? =
    query
        .trim()
        .replace(
            Regex(
                "^(?:https?://(?:www\\.)?arxiv\\.org/(?:abs|pdf)/|arxiv:)",
                RegexOption.IGNORE_CASE,
            ),
            "",
        )
        .removeSuffix(".pdf")
        .takeIf { it.matches(Regex("\\d{4}\\.\\d{4,5}(v\\d+)?")) }

data class ResearchItem(
    val id: String,
    val title: String,
    val authors: String = "",
    val year: Int? = null,
    val summary: String = "",
    val url: String = "",
    val pdfUrl: String = "",
    val doi: String = "",
    val source: String = "",
    val kind: ItemKind = ItemKind.PAPER,
    val openAccess: Boolean? = null,
    val citations: Int? = null,
    val collection: String = "Reading list",
    val savedAt: Long = System.currentTimeMillis(),
    val favorite: Boolean = false,
    val readingStatus: ReadingStatus = ReadingStatus.TO_READ,
    val tags: List<String> = emptyList(),
) {
    fun json() =
        JSONObject()
            .put("id", id)
            .put("title", title)
            .put("authors", authors)
            .put("year", year ?: JSONObject.NULL)
            .put("summary", summary)
            .put("url", url)
            .put("pdfUrl", pdfUrl)
            .put("doi", doi)
            .put("source", source)
            .put("kind", kind.name)
            .put("openAccess", openAccess ?: JSONObject.NULL)
            .put("citations", citations ?: JSONObject.NULL)
            .put("collection", collection)
            .put("savedAt", savedAt)
            .put("favorite", favorite)
            .put("readingStatus", readingStatus.name)
            .put("tags", JSONArray(tags))

    companion object {
        fun from(j: JSONObject) =
            ResearchItem(
                j.getString("id"),
                j.getString("title"),
                j.optString("authors"),
                if (j.isNull("year")) null else j.optInt("year").takeIf { it > 0 },
                j.optString("summary"),
                j.optString("url"),
                j.optString("pdfUrl"),
                j.optString("doi"),
                j.optString("source"),
                runCatching { ItemKind.valueOf(j.optString("kind", "PAPER")) }
                    .getOrDefault(ItemKind.PAPER),
                if (j.isNull("openAccess")) null else j.optBoolean("openAccess"),
                if (j.isNull("citations")) null else j.optInt("citations"),
                j.optString("collection", "Reading list"),
                j.optLong("savedAt"),
                j.optBoolean("favorite"),
                runCatching { ReadingStatus.valueOf(j.optString("readingStatus", "TO_READ")) }
                    .getOrDefault(ReadingStatus.TO_READ),
                j.optJSONArray("tags")
                    ?.strings()
                    ?.map { it.trim().take(40) }
                    ?.filter { it.isNotBlank() }
                    ?.distinct()
                    ?.take(20) ?: emptyList(),
            )
    }
}

enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
}

data class ProjectTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val status: TaskStatus = TaskStatus.TODO,
) {
    fun json() = JSONObject().put("id", id).put("title", title).put("status", status.name)
}

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val question: String = "",
    val deadline: String = "",
    val tasks: List<ProjectTask> = emptyList(),
    val paperIds: List<String> = emptyList(),
) {
    val progress: Float
        get() =
            if (tasks.isEmpty()) 0f
            else tasks.count { it.status == TaskStatus.DONE }.toFloat() / tasks.size

    fun json() =
        JSONObject()
            .put("id", id)
            .put("title", title)
            .put("question", question)
            .put("deadline", deadline)
            .put("tasks", JSONArray().apply { tasks.forEach { put(it.json()) } })
            .put("paperIds", JSONArray(paperIds))

    companion object {
        fun from(j: JSONObject) =
            Project(
                j.getString("id"),
                j.getString("title"),
                j.optString("question"),
                j.optString("deadline"),
                j.optJSONArray("tasks").objects().map {
                    ProjectTask(
                        it.getString("id"),
                        it.getString("title"),
                        TaskStatus.valueOf(it.getString("status")),
                    )
                },
                j.optJSONArray("paperIds").strings(),
            )
    }
}

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val text: String,
    val page: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun json() =
        JSONObject()
            .put("id", id)
            .put("itemId", itemId)
            .put("text", text)
            .put("page", page)
            .put("createdAt", createdAt)

    companion object {
        fun from(j: JSONObject) =
            Note(
                j.getString("id"),
                j.getString("itemId"),
                j.getString("text"),
                j.optInt("page"),
                j.optLong("createdAt"),
            )
    }
}

data class ChatMessage(val role: String, val text: String, val sourceId: String = "") {
    fun json() = JSONObject().put("role", role).put("text", text).put("sourceId", sourceId)
}

data class Workspace(
    val items: List<ResearchItem> = emptyList(),
    val projects: List<Project> = emptyList(),
    val notes: List<Note> = emptyList(),
    val history: List<String> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
) {
    fun json() =
        JSONObject()
            .put("schema", 1)
            .put("items", JSONArray().apply { items.forEach { put(it.json()) } })
            .put("projects", JSONArray().apply { projects.forEach { put(it.json()) } })
            .put("notes", JSONArray().apply { notes.forEach { put(it.json()) } })
            .put("history", JSONArray(history))
            .put("messages", JSONArray().apply { messages.forEach { put(it.json()) } })

    companion object {
        fun from(j: JSONObject): Workspace {
            require(j.optInt("schema") == 1) { "Unsupported backup version" }
            val workspace =
                Workspace(
                    j.optJSONArray("items").objects().map(ResearchItem::from),
                    j.optJSONArray("projects").objects().map(Project::from),
                    j.optJSONArray("notes").objects().map(Note::from),
                    j.optJSONArray("history").strings(),
                    j.optJSONArray("messages").objects().map {
                        ChatMessage(
                            it.getString("role"),
                            it.getString("text"),
                            it.optString("sourceId"),
                        )
                    },
                )
            require(
                workspace.items.size <= 10000 &&
                    workspace.projects.size <= 10000 &&
                    workspace.notes.size <= 10000
            ) {
                "Backup contains too many records"
            }
            require(
                workspace.items.map { it.id }.distinct().size == workspace.items.size &&
                    workspace.projects.map { it.id }.distinct().size == workspace.projects.size &&
                    workspace.notes.map { it.id }.distinct().size == workspace.notes.size
            ) {
                "Backup contains duplicate record IDs"
            }
            val itemIds = workspace.items.map { it.id }.toSet()
            require(
                workspace.items.all { it.id.isNotBlank() && it.title.isNotBlank() } &&
                    workspace.notes.all { it.itemId in itemIds && it.page >= 0 } &&
                    workspace.projects.all { p ->
                        p.paperIds.all { it in itemIds } &&
                            p.tasks.map { it.id }.distinct().size == p.tasks.size
                    }
            ) {
                "Backup contains invalid references"
            }
            return workspace
        }
    }
}

fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { getString(it) }

fun parseLegacyTasks(raw: String): List<ProjectTask> {
    if (raw.isBlank()) return emptyList()
    val json = runCatching { JSONArray(raw) }.getOrNull()
    return if (json != null)
        (0 until json.length()).mapNotNull { i ->
            val value = json.opt(i)
            when (value) {
                is JSONObject ->
                    value
                        .optString("title")
                        .takeIf { it.isNotBlank() }
                        ?.let {
                            ProjectTask(
                                title = it,
                                status =
                                    runCatching {
                                            TaskStatus.valueOf(value.optString("status", "TODO"))
                                        }
                                        .getOrDefault(TaskStatus.TODO),
                            )
                        }
                is String -> {
                    val completed = value.substringAfterLast('|', "") == "true"
                    val title =
                        if (value.substringAfterLast('|', "") in listOf("true", "false"))
                            value.substringBeforeLast('|')
                        else value
                    ProjectTask(
                        title = title,
                        status = if (completed) TaskStatus.DONE else TaskStatus.TODO,
                    )
                }
                else -> null
            }
        }
    else
        raw.split("||")
            .filter { it.isNotBlank() }
            .map {
                val title = it.substringBeforeLast("::", it)
                val status =
                    runCatching { TaskStatus.valueOf(it.substringAfterLast("::", "TODO")) }
                        .getOrDefault(TaskStatus.TODO)
                ProjectTask(title = title, status = status)
            }
}

fun ResearchItem.bibtex(): String {
    fun escape(s: String) =
        s.replace("\\", "\\textbackslash{}")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("%", "\\%")
            .replace("&", "\\&")
            .replace("_", "\\_")
    val key = "overthink" + id.hashCode().toUInt().toString(16)
    return "@misc{$key,\n  title={${escape(title)}},\n  author={${escape(authors)}},\n" +
        (year?.let { "  year={$it},\n" } ?: "") +
        (doi.takeIf { it.isNotBlank() }?.let { "  doi={${escape(it)}},\n" } ?: "") +
        "  url={${escape(url)}}\n}"
}

fun List<ResearchItem>.export(format: String): String {
    fun line(s: String) = s.replace('\r', ' ').replace('\n', ' ')
    fun markdown(s: String) =
        line(s)
            .replace("\\", "\\\\")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("*", "\\*")
            .replace("_", "\\_")
    return when (format) {
        "RIS" ->
            joinToString("\n\n") { p ->
                buildString {
                    append("TY  - GEN\nTI  - ${line(p.title)}\n")
                    if (p.authors.isNotBlank()) append("AU  - ${line(p.authors)}\n")
                    p.year?.let { append("PY  - $it\n") }
                    if (p.doi.isNotBlank()) append("DO  - ${line(p.doi)}\n")
                    if (p.url.isNotBlank()) append("UR  - ${line(p.url)}\n")
                    if (p.summary.isNotBlank()) append("AB  - ${line(p.summary)}\n")
                    append("ER  -")
                }
            }
        "Markdown" ->
            "# Research library\n\n" +
                joinToString("\n\n---\n\n") { p ->
                    buildString {
                        append("## ${markdown(p.title)}\n\n")
                        append(
                            listOf(p.authors, p.year?.toString().orEmpty(), p.source)
                                .filter { it.isNotBlank() }
                                .joinToString(" · ") { markdown(it) }
                        )
                        if (p.url.startsWith("https://") || p.url.startsWith("http://"))
                            append("\n\n<${p.url.replace(">", "%3E").replace("\n", "") }>")
                        append(
                            "\n\nCollection: ${markdown(p.collection)} · ${p.readingStatus.label}"
                        )
                        if (p.tags.isNotEmpty())
                            append("\nTags: ${p.tags.joinToString { markdown(it) }}")
                        if (p.summary.isNotBlank()) append("\n\n${markdown(p.summary)}")
                    }
                }
        else -> joinToString("\n\n") { it.bibtex() }
    }
}
