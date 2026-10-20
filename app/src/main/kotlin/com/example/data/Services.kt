package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class SessionVault(context: Context) {
    private val prefs = context.getSharedPreferences("overthink_session", Context.MODE_PRIVATE)

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (ks.getKey("overthink_session_v2", null) as? SecretKey)
            ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                .apply {
                    init(
                        KeyGenParameterSpec.Builder(
                                "overthink_session_v2",
                                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                            )
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .build()
                    )
                }
                .generateKey()
    }

    fun write(value: String) {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key())
        prefs
            .edit()
            .putString("iv", Base64.encodeToString(c.iv, Base64.NO_WRAP))
            .putString(
                "body",
                Base64.encodeToString(c.doFinal(value.toByteArray()), Base64.NO_WRAP),
            )
            .apply()
    }

    fun read(): String? =
        runCatching {
                val c = Cipher.getInstance("AES/GCM/NoPadding")
                c.init(
                    Cipher.DECRYPT_MODE,
                    key(),
                    GCMParameterSpec(
                        128,
                        Base64.decode(prefs.getString("iv", null), Base64.NO_WRAP),
                    ),
                )
                String(c.doFinal(Base64.decode(prefs.getString("body", null), Base64.NO_WRAP)))
            }
            .getOrNull()

    fun clear() = prefs.edit().clear().apply()
}

class BackendClient {
    private val client =
        OkHttpClient.Builder().callTimeout(60, TimeUnit.SECONDS).followRedirects(false).build()
    var baseUrl = ""
    var token = ""

    suspend fun request(
        path: String,
        body: JSONObject? = null,
        method: String = if (body == null) "GET" else "POST",
    ): JSONObject =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(baseUrl)
            require(
                uri.scheme == "https" ||
                    (uri.scheme == "http" &&
                        uri.host in listOf("localhost", "127.0.0.1", "10.0.2.2"))
            ) {
                "Use an HTTPS server URL"
            }
            val b =
                Request.Builder()
                    .url(baseUrl.trimEnd('/') + path)
                    .header("Accept", "application/json")
            if (token.isNotEmpty()) b.header("Authorization", "Bearer $token")
            if (method != "GET")
                b.method(
                    method,
                    (body ?: JSONObject())
                        .toString()
                        .toRequestBody("application/json".toMediaType()),
                )
            suspendCancellableCoroutine { continuation ->
                val call = client.newCall(b.build())
                continuation.invokeOnCancellation { call.cancel() }
                call.enqueue(
                    object : okhttp3.Callback {
                        override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                            if (continuation.isActive)
                                continuation.resumeWithException(
                                    java.io.IOException(
                                        "Could not reach the server. Check the URL and your connection."
                                    )
                                )
                        }

                        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                            response.use {
                                if (!continuation.isActive) return
                                try {
                                    val raw = it.body?.string().orEmpty()
                                    if (!it.isSuccessful)
                                        error(
                                            runCatching { JSONObject(raw).optString("error") }
                                                .getOrDefault("")
                                                .ifBlank { "Server returned HTTP ${it.code}" }
                                        )
                                    val result = JSONObject(raw.ifBlank { "{}" })
                                    continuation.resume(result)
                                } catch (e: Exception) {
                                    continuation.resumeWithException(e)
                                }
                            }
                        }
                    }
                )
            }
        }
}

/** Survives activity/process recreation while Android's document picker is open. */
class PendingExport(context: Context) {
    private val file = android.util.AtomicFile(File(context.cacheDir, "overthink-export.pending"))

    @Synchronized
    fun prepare(text: String) {
        val out = file.startWrite()
        try {
            out.write(text.toByteArray(Charsets.UTF_8))
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            throw e
        }
    }

    @Synchronized
    fun copyTo(out: java.io.OutputStream) {
        file.openRead().use { it.copyTo(out) }
    }

    @Synchronized fun clear() = file.delete()
}

class DocumentStore(private val context: Context) {
    private val client =
        OkHttpClient.Builder().callTimeout(90, TimeUnit.SECONDS).followSslRedirects(false).build()
    private val folder = File(context.filesDir, "documents").apply { mkdirs() }

    fun cached(id: String): File? = File(folder, "${digest(id)}.pdf").takeIf { it.exists() }

    suspend fun download(item: ResearchItem): File =
        withContext(Dispatchers.IO) {
            cached(item.id)?.let {
                return@withContext it
            }
            require(Uri.parse(item.pdfUrl).scheme == "https") {
                "No direct HTTPS PDF is available. Open the source or import a PDF."
            }
            val target = File(folder, "${digest(item.id)}.pdf")
            val temp = File(folder, "${digest(item.id)}.part")
            try {
                client.newCall(Request.Builder().url(item.pdfUrl).build()).execute().use { r ->
                    require(r.isSuccessful) { "PDF request failed (HTTP ${r.code})" }
                    require(r.request.url.isHttps) { "The PDF redirected to an insecure URL" }
                    val body = r.body ?: error("Empty PDF response")
                    body.byteStream().use { input ->
                        temp.outputStream().use { output ->
                            val bytes = ByteArray(16384)
                            var total = 0L
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val n = input.read(bytes)
                                if (n < 0) break
                                total += n
                                require(total <= 40L * 1024 * 1024) {
                                    "PDF exceeds the 40 MB download limit"
                                }
                                output.write(bytes, 0, n)
                            }
                        }
                    }
                }
                validate(temp)
                check(temp.renameTo(target)) { "Could not save the document" }
                target
            } finally {
                temp.delete()
            }
        }

    suspend fun import(uri: Uri, id: String): File =
        withContext(Dispatchers.IO) {
            val temp = File(folder, "${digest(id)}.part")
            val target = File(folder, "${digest(id)}.pdf")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    temp.outputStream().use { out ->
                        val b = ByteArray(16384)
                        var total = 0
                        while (true) {
                            val n = input.read(b)
                            if (n < 0) break
                            total += n
                            require(total < 40 * 1024 * 1024) { "PDF exceeds 40 MB" }
                            out.write(b, 0, n)
                        }
                    }
                } ?: error("Could not open this file")
                validate(temp)
                val backup = File(folder, "${digest(id)}.previous")
                if (target.exists())
                    check(target.renameTo(backup)) { "Could not replace the previous PDF" }
                if (!temp.renameTo(target)) {
                    if (backup.exists()) backup.renameTo(target)
                    error("Could not save the imported PDF")
                }
                backup.delete()
                target
            } finally {
                temp.delete()
            }
        }

    private fun validate(file: File) {
        file.inputStream().use {
            val header = ByteArray(5)
            require(it.read(header) == 5 && String(header) == "%PDF-") {
                "The source returned a webpage, not a PDF"
            }
        }
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { require(it.pageCount > 0) { "The PDF has no pages" } }
        }
    }

    suspend fun page(file: File, index: Int): Pair<Bitmap, Int> =
        withContext(Dispatchers.IO) {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { pdf ->
                    pdf.openPage(index.coerceIn(0, pdf.pageCount - 1)).use { p ->
                        val scale = minOf(1400f / p.width, 6000f / p.height)
                        val bitmap =
                            Bitmap.createBitmap(
                                (p.width * scale).toInt().coerceAtLeast(1),
                                (p.height * scale).toInt().coerceAtLeast(1),
                                Bitmap.Config.ARGB_8888,
                            )
                        bitmap.eraseColor(Color.WHITE)
                        p.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap to pdf.pageCount
                    }
                }
            }
        }
}
