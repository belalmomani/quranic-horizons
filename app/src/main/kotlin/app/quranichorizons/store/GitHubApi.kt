package app.quranichorizons.store

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** آخر إصدار منشور (Release) في مستودع GitHub مع ملف APK إن وُجد. */
data class ReleaseInfo(val tag: String, val title: String, val apkUrl: String?, val apkName: String?, val apkSize: Long)

/** معلومات المستودع العامة. */
data class RepoInfo(val owner: String, val name: String, val description: String, val iconUrl: String)

/** وصول للقراءة فقط إلى واجهة GitHub العامة (بلا مفاتيح). المستودعات الخاصة غير مدعومة. */
object GitHubApi {
    private val json = Json { ignoreUnknownKeys = true }

    /** يقبل owner/name أو رابط https://github.com/owner/name؛ وإلا null. */
    fun parseRepo(input: String): Pair<String, String>? {
        var s = input.trim().removeSuffix("/").removeSuffix(".git")
        s = s.removePrefix("https://").removePrefix("http://").removePrefix("www.").removePrefix("github.com/")
        val p = s.split("/")
        if (p.size < 2) return null
        val ok = Regex("^[A-Za-z0-9._-]+$")
        return if (ok.matches(p[0]) && ok.matches(p[1])) p[0] to p[1] else null
    }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000; c.readTimeout = 20000
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("User-Agent", "QuranicHorizons")
        try {
            if (c.responseCode != 200) throw IllegalStateException("GitHub ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }

    suspend fun latestRelease(owner: String, name: String): ReleaseInfo = withContext(Dispatchers.IO) {
        val o = json.parseToJsonElement(get("https://api.github.com/repos/$owner/$name/releases/latest")).jsonObject
        val assets: JsonArray = (o["assets"] as? JsonArray) ?: JsonArray(emptyList())
        val apk: JsonObject? = assets.map { it.jsonObject }.firstOrNull { (it["name"]?.jsonPrimitive?.contentOrNull ?: "").endsWith(".apk", true) }
        ReleaseInfo(
            tag = o["tag_name"]?.jsonPrimitive?.contentOrNull ?: "",
            title = o["name"]?.jsonPrimitive?.contentOrNull ?: "",
            apkUrl = apk?.get("browser_download_url")?.jsonPrimitive?.contentOrNull,
            apkName = apk?.get("name")?.jsonPrimitive?.contentOrNull,
            apkSize = apk?.get("size")?.jsonPrimitive?.longOrNull ?: 0L,
        )
    }

    suspend fun repoInfo(owner: String, name: String): RepoInfo = withContext(Dispatchers.IO) {
        val o = json.parseToJsonElement(get("https://api.github.com/repos/$owner/$name")).jsonObject
        RepoInfo(owner, name, o["description"]?.jsonPrimitive?.contentOrNull ?: "",
            "https://raw.githubusercontent.com/$owner/$name/HEAD/store/icon.png")
    }

    /** تنزيل ملف إلى مجلد التخزين المؤقت؛ onProgress تُستدعى بالنسبة 0..100 (أو -1 إن كان الحجم مجهولًا). */
    suspend fun download(ctx: Context, url: String, fileName: String, onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(ctx.cacheDir, "apk").apply { mkdirs() }
        val out = File(dir, fileName.replace(Regex("[^A-Za-z0-9._-]"), "_"))
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000; c.readTimeout = 30000
        c.setRequestProperty("User-Agent", "QuranicHorizons")
        c.instanceFollowRedirects = true
        try {
            if (c.responseCode != 200) throw IllegalStateException("HTTP ${c.responseCode}")
            val total = c.contentLengthLong
            var done = 0L
            c.inputStream.use { ins ->
                out.outputStream().use { os ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = ins.read(buf); if (n < 0) break
                        os.write(buf, 0, n); done += n
                        onProgress(if (total > 0) (done * 100 / total).toInt() else -1)
                    }
                }
            }
        } finally { c.disconnect() }
        out
    }

    /** يفتح مثبّت النظام. إن لم يُسمح للتطبيق بالتثبيت يفتح شاشة السماح ويُرجع false. */
    fun install(ctx: Context, apk: File): Boolean {
        if (Build.VERSION.SDK_INT >= 26 && !ctx.packageManager.canRequestPackageInstalls()) {
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + ctx.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return false
        }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", apk)
        ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }
}
