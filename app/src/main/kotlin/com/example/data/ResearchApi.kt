package com.example.data

import android.util.Xml
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser

data class SearchFilter(
    val source: String = "All sources",
    val fromYear: Int = 0,
    val pdfOnly: Boolean = false,
)

data class ProviderOutcome(
    val provider: String,
    val items: List<ResearchItem> = emptyList(),
    val error: String? = null,
)

data class SearchPage(
    val items: List<ResearchItem>,
    val statuses: List<ProviderOutcome>,
    val canLoadMore: Boolean,
)

interface SearchGateway {
    suspend fun search(query: String, kind: ItemKind, page: Int, filter: SearchFilter): SearchPage
}

class ResearchApi : SearchGateway {
    private val client =
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .callTimeout(35, TimeUnit.SECONDS)
            .build()

    override suspend fun search(
        query: String,
        kind: ItemKind,
        page: Int,
        filter: SearchFilter,
    ): SearchPage = coroutineScope {
        val q = query.trim()
        require(q.isNotBlank()) { "Enter a topic, title, or DOI" }
        require(q.length <= 4000) { "Use a shorter search query" }
        val providers =
            when (kind) {
                ItemKind.PAPER ->
                    if (arxivIdentifier(q) != null) listOf("arXiv")
                    else if (doi(q) != null) listOf("OpenAlex", "Crossref")
                    else listOf("OpenAlex", "Crossref", "arXiv")
                ItemKind.DATASET -> listOf("DataCite")
                ItemKind.CODE -> listOf("GitHub")
            }.filter {
                kind != ItemKind.PAPER || filter.source == "All sources" || filter.source == it
            }
        require(providers.isNotEmpty()) {
            "This identifier is not available from the selected provider. Choose All sources."
        }
        val jobs =
            providers.map { source ->
                async {
                    try {
                        ProviderOutcome(
                            source,
                            when (source) {
                                "OpenAlex" -> openAlex(q, page)
                                "Crossref" -> crossref(q, page)
                                "arXiv" -> arxiv(q, page)
                                "DataCite" -> datasets(q, page)
                                else -> github(q, page)
                            },
                        )
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        ProviderOutcome(source, error = e.message ?: "Request failed")
                    }
                }
            }
        val statuses = jobs.map { it.await() }
        val raw = statuses.flatMap { it.items }
        val result =
            raw.distinctBy {
                    if (it.doi.isNotBlank()) it.doi.lowercase()
                    else it.title.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")
                }
                .filter { filter.accepts(it) }
        SearchPage(
            result,
            statuses,
            raw.size >= 12 && !q.startsWith("10.") && !q.contains("doi.org/"),
        )
    }

    private suspend fun json(url: String): JSONObject = JSONObject(text(url))

    private suspend fun text(url: String): String = suspendCancellableCoroutine { continuation ->
        val call =
            client.newCall(
                Request.Builder()
                    .url(url)
                    .header("User-Agent", "Overthink/2.0 academic-discovery")
                    .header("Accept", "application/json, application/atom+xml")
                    .build()
            )
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive)
                        continuation.resumeWithException(
                            IOException("Connection failed. Check your network and retry.")
                        )
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!continuation.isActive) return
                        if (!it.isSuccessful)
                            continuation.resumeWithException(
                                IOException(
                                    when (it.code) {
                                        429 -> "Rate limit reached. Retry shortly."
                                        401,
                                        403 -> "Provider denied this request."
                                        else -> "Provider returned HTTP ${it.code}"
                                    }
                                )
                            )
                        else continuation.resume(it.body?.string().orEmpty())
                    }
                }
            }
        )
    }

    private fun encode(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun doi(q: String) =
        q.trim().removePrefix("https://doi.org/").removePrefix("http://doi.org/").takeIf {
            it.startsWith("10.") && it.contains('/')
        }

    private suspend fun openAlex(q: String, page: Int): List<ResearchItem> {
        val identifier = doi(q)
        val root =
            json(
                if (identifier != null)
                    "https://api.openalex.org/works/https://doi.org/${encode(identifier)}"
                else "https://api.openalex.org/works?search=${encode(q)}&per-page=12&page=$page"
            )
        return (if (identifier != null) listOf(root) else root.optJSONArray("results").objects())
            .map { j ->
                val loc = j.optJSONObject("best_oa_location") ?: j.optJSONObject("primary_location")
                val inv = j.optJSONObject("abstract_inverted_index")
                val words = sortedMapOf<Int, String>()
                inv?.keys()?.forEach { word ->
                    inv.optJSONArray(word)?.let { a ->
                        for (i in 0 until a.length()) words[a.getInt(i)] = word
                    }
                }
                val d =
                    j.optString("doi")
                        .removePrefix("https://doi.org/")
                        .takeUnless { it == "null" }
                        .orEmpty()
                ResearchItem(
                    "doi:${d.lowercase()}".takeIf { d.isNotBlank() } ?: j.getString("id"),
                    j.optString("display_name", "Untitled record"),
                    j.optJSONArray("authorships")
                        .objects()
                        .mapNotNull { it.optJSONObject("author")?.optString("display_name") }
                        .joinToString(", "),
                    j.optInt("publication_year").takeIf { it > 0 },
                    words.values.joinToString(" "),
                    loc?.optString("landing_page_url")
                        ?.takeUnless { it == "null" }
                        .orEmpty()
                        .ifBlank {
                            if (d.isNotBlank()) "https://doi.org/$d" else j.getString("id")
                        },
                    loc?.optString("pdf_url")?.takeUnless { it == "null" }.orEmpty(),
                    d,
                    "OpenAlex",
                    openAccess = j.optJSONObject("open_access")?.optBoolean("is_oa"),
                    citations = j.optInt("cited_by_count"),
                )
            }
    }

    private suspend fun crossref(q: String, page: Int): List<ResearchItem> {
        val d = doi(q)
        val message =
            json(
                    if (d != null) "https://api.crossref.org/works/${encode(d)}"
                    else
                        "https://api.crossref.org/works?query=${encode(q)}&rows=12&offset=${(page-1)*12}"
                )
                .getJSONObject("message")
        return (if (d != null) listOf(message) else message.optJSONArray("items").objects()).map { j
            ->
            val id = j.optString("DOI")
            ResearchItem(
                "doi:${id.lowercase()}",
                j.optJSONArray("title")?.optString(0).orEmpty().ifBlank { "Untitled record" },
                j.optJSONArray("author").objects().joinToString(", ") {
                    "${it.optString("given")} ${it.optString("family")}".trim()
                },
                j.optJSONObject("published")
                    ?.optJSONArray("date-parts")
                    ?.optJSONArray(0)
                    ?.optInt(0),
                j.optString("abstract").replace(Regex("<[^>]+>"), " "),
                j.optString("URL", "https://doi.org/$id"),
                doi = id,
                source = "Crossref",
                citations = j.optInt("is-referenced-by-count"),
            )
        }
    }

    private suspend fun arxiv(q: String, page: Int): List<ResearchItem> {
        if (doi(q) != null) return emptyList()
        val id = arxivIdentifier(q)
        val body =
            text(
                if (id != null) "https://export.arxiv.org/api/query?id_list=$id"
                else
                    "https://export.arxiv.org/api/query?search_query=all:${encode(q)}&start=${(page-1)*12}&max_results=12"
            )
        return withContext(Dispatchers.Default) {
            val parser = Xml.newPullParser()
            parser.setInput(body.reader())
            val records = mutableListOf<ResearchItem>()
            var title = ""
            var summary = ""
            var url = ""
            var published = ""
            var pdf = ""
            val authors = mutableListOf<String>()
            var inEntry = false
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG)
                    when (parser.name) {
                        "entry" -> {
                            inEntry = true
                            title = ""
                            summary = ""
                            url = ""
                            pdf = ""
                            published = ""
                            authors.clear()
                        }
                        "title" ->
                            if (inEntry) title = parser.nextText().replace(Regex("\\s+"), " ")
                        "summary" -> if (inEntry) summary = parser.nextText().trim()
                        "id" -> if (inEntry) url = parser.nextText().replace("http://", "https://")
                        "published" -> if (inEntry) published = parser.nextText()
                        "name" -> if (inEntry) authors.add(parser.nextText())
                        "link" ->
                            if (inEntry && parser.getAttributeValue(null, "title") == "pdf")
                                pdf =
                                    parser
                                        .getAttributeValue(null, "href")
                                        .replace("http://", "https://")
                    }
                if (parser.eventType == XmlPullParser.END_TAG && parser.name == "entry") {
                    records.add(
                        ResearchItem(
                            url,
                            title,
                            authors.joinToString(", "),
                            published.take(4).toIntOrNull(),
                            summary,
                            url,
                            pdf,
                            source = "arXiv",
                            openAccess = true,
                        )
                    )
                    inEntry = false
                }
                parser.next()
            }
            records
        }
    }

    private suspend fun datasets(q: String, page: Int) =
        json(
                "https://api.datacite.org/dois?query=${encode(q)}&resource-type-id=dataset&page[size]=12&page[number]=$page"
            )
            .optJSONArray("data")
            .objects()
            .map { j ->
                val a = j.getJSONObject("attributes")
                val d = a.optString("doi")
                ResearchItem(
                    "doi:$d",
                    a.optJSONArray("titles").objects().firstOrNull()?.optString("title").orEmpty(),
                    a.optJSONArray("creators").objects().joinToString(", ") {
                        it.optString("name")
                    },
                    a.optInt("publicationYear").takeIf { it > 0 },
                    a.optJSONArray("descriptions")
                        .objects()
                        .firstOrNull()
                        ?.optString("description")
                        .orEmpty(),
                    a.optString("url").ifBlank { "https://doi.org/$d" },
                    doi = d,
                    source = "DataCite",
                    kind = ItemKind.DATASET,
                )
            }

    private suspend fun github(q: String, page: Int) =
        json(
                "https://api.github.com/search/repositories?q=${encode(q)}&sort=stars&per_page=12&page=$page"
            )
            .optJSONArray("items")
            .objects()
            .map { j ->
                ResearchItem(
                    "github:${j.getLong("id")}",
                    j.getString("full_name"),
                    j.optJSONObject("owner")?.optString("login").orEmpty(),
                    j.optString("updated_at").take(4).toIntOrNull(),
                    j.optString("description").takeUnless { it == "null" }.orEmpty(),
                    j.getString("html_url"),
                    source = "GitHub",
                    kind = ItemKind.CODE,
                )
            }
}

/** Paper filters never exclude dataset/code records or invent dates for undated papers. */
fun SearchFilter.accepts(item: ResearchItem): Boolean =
    item.kind != ItemKind.PAPER ||
        ((fromYear == 0 || (item.year?.let { it >= fromYear } ?: false)) &&
            (!pdfOnly || item.pdfUrl.isNotBlank()))
