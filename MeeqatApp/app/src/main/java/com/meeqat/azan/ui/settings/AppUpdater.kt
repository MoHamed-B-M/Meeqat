package com.meeqat.azan.ui.settings

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * In-app updater backed by the rolling `beta-latest` GitHub prerelease.
 * No new dependencies: HttpURLConnection + DownloadManager + FileProvider only.
 */
object AppUpdater {
    const val REPO = "MoHamed-B-M/Meeqat"
    const val TAG = "beta-latest"
    private const val API_URL = "https://api.github.com/repos/$REPO/releases/tags/$TAG"

    data class Latest(
        val releaseName: String,
        val publishedAt: String,
        val apkUrl: String,
        val apkName: String,
        val sizeBytes: Long,
        /** Parsed from release name `Meeqat 1.0.0-dev(#N)` → 100000 + N, else -1. */
        val versionCode: Long,
    )

    suspend fun fetchLatest(): Latest = withContext(Dispatchers.IO) {
        val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Meeqat-Updater")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("GitHub API returned HTTP $code")
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            parseRelease(JSONObject(body))
        } finally {
            conn.disconnect()
        }
    }

    private fun parseRelease(json: JSONObject): Latest {
        val name = json.optString("name", TAG)
        val published = json.optString("published_at", "")
        val assets = json.optJSONArray("assets")
        var apkUrl = ""
        var apkName = ""
        var size = 0L
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val a = assets.optJSONObject(i) ?: continue
                val assetName = a.optString("name", "")
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = a.optString("browser_download_url", "")
                    apkName = assetName
                    size = a.optLong("size", 0L)
                    break
                }
            }
        }
        if (apkUrl.isBlank()) throw IllegalStateException("No APK asset found in $TAG release")
        val runNumber = Regex("""#(\d+)""").find(name)?.groupValues?.getOrNull(1)?.toLongOrNull()
        val versionCode = if (runNumber != null) 100000L + runNumber else -1L
        return Latest(name, published, apkUrl, apkName, size, versionCode)
    }

    fun installedVersionCode(context: Context): Long {
        return try {
            val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkg.longVersionCode else @Suppress("DEPRECATION") pkg.versionCode.toLong()
        } catch (_: Exception) { -1L }
    }

    fun installedVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—"
        } catch (_: Exception) { "—" }
    }

    fun canInstallUnknownApps(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }

    fun openUnknownAppsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    /** Downloads the APK with progress callbacks (0f..1f, -1f = unknown length). */
    suspend fun download(context: Context, url: String, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
            val out = File(dir, "meeqat-update.apk")
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Meeqat-Updater")
                setRequestProperty("Accept", "application/vnd.android.package-archive")
                connectTimeout = 20_000
                readTimeout = 20_000
                connect()
            }
            try {
                val total = conn.contentLengthLong
                conn.inputStream.use { input ->
                    out.outputStream().use { output ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            output.write(buf, 0, n)
                            done += n
                            onProgress(if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else -1f)
                        }
                        output.flush()
                    }
                }
            } finally {
                conn.disconnect()
            }
            out
        }

    fun install(context: Context, apk: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    @Suppress("unused")
    fun enqueueWithSystemDownloader(context: Context, url: String, fileName: String): Long {
        val req = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("Meeqat update")
            setDescription(fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "meeqat-update.apk")
            setMimeType("application/vnd.android.package-archive")
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(req)
    }
}
