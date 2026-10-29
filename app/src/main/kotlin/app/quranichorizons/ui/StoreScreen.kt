package app.quranichorizons.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quranichorizons.BuildConfig
import app.quranichorizons.data.Prefs
import app.quranichorizons.i18n.Lang
import app.quranichorizons.i18n.LocalLang
import app.quranichorizons.i18n.Strings
import app.quranichorizons.store.GitHubApi
import app.quranichorizons.store.StoreApp
import app.quranichorizons.store.StoreRepo
import kotlinx.coroutines.launch

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = QhColors.Ink, unfocusedTextColor = QhColors.Ink,
    focusedBorderColor = QhColors.Gold, unfocusedBorderColor = QhColors.Muted, cursorColor = QhColors.Gold,
)

private fun mbText(b: Long) = if (b <= 0) "" else "%.1f MB".format(b / 1048576.0)

/** تنزيل APK وفتح المثبّت مع عرض النسبة. يُرجع رسالة الحالة عبر onStatus. */
private suspend fun downloadAndInstall(ctx: android.content.Context, lang: Lang, url: String, name: String, onStatus: (String) -> Unit) {
    fun s(k: String, vararg a: Any) = Strings.get(lang, k, *a)
    try {
        val f = GitHubApi.download(ctx, url, name) { p -> onStatus(if (p >= 0) s("updProgress", p) else "…") }
        onStatus(if (GitHubApi.install(ctx, f)) "" else s("updAllow"))
    } catch (e: Exception) {
        onStatus(s("updError", e.message ?: e.javaClass.simpleName))
    }
}

/** «متجري»: تطبيقات من GitHub بتصنيفات؛ للمستخدم المعتمدة فقط، وللمدير كل الحالات وأزرار الاعتماد. */
@Composable
fun StoreScreen(prefs: Prefs, admin: Boolean, onBack: () -> Unit) {
    val repo = remember { StoreRepo(prefs) }
    var apps by remember { mutableStateOf(repo.all()) }
    var filter by remember { mutableStateOf("") }          // "" = الكل
    var tab by remember { mutableStateOf("approved") }
    var input by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val lang = LocalLang.current
    fun s(k: String, vararg a: Any) = Strings.get(lang, k, *a)
    fun reload() { apps = repo.all() }

    val shown = apps.filter { (if (admin) it.status == tab else it.status == "approved") && (filter.isEmpty() || it.category == filter) }

    Column(Modifier.fillMaxSize().background(QhColors.NavyDeep).padding(16.dp)) {
        TextButton(onClick = onBack) { Text("‹ " + s("back"), color = QhColors.GoldSoft) }
        Text(s(if (admin) "adminTitle" else "storeTitle"), color = QhColors.Gold, fontSize = 24.sp)
        if (admin) Text(s("adminLocalNote"), color = QhColors.Muted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            listOf("" to "storeAll", "tools" to "storeTools", "news" to "storeNews").forEach { (k, label) ->
                OutlinedButton(onClick = { filter = k }, border = BorderStroke(1.dp, if (filter == k) QhColors.Gold else QhColors.Muted)) {
                    Text(s(label), color = if (filter == k) QhColors.Gold else QhColors.Ink, fontSize = 13.sp)
                }
            }
        }
        if (admin) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("pending" to "storePending", "approved" to "storeApproved", "rejected" to "storeRejected").forEach { (k, label) ->
                OutlinedButton(onClick = { tab = k }, border = BorderStroke(1.dp, if (tab == k) QhColors.Gold else QhColors.Muted)) {
                    Text(s(label), color = if (tab == k) QhColors.Gold else QhColors.Ink, fontSize = 13.sp)
                }
            }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp)) {
            // الإضافة من لوحة الإدارة فقط
            if (admin) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = QhColors.NavyCard)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("+  " + s("storeAddTitle"), color = QhColors.Ink, fontSize = 16.sp)
                        Text(s("storeAddNote"), color = QhColors.Muted, fontSize = 12.sp)
                        OutlinedTextField(value = input, onValueChange = { input = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("owner/name", color = QhColors.Muted) }, colors = fieldColors())
                        Button(enabled = !busy && input.isNotBlank(), onClick = {
                            val p = GitHubApi.parseRepo(input)
                            if (p == null) { msg = s("badRepo"); return@Button }
                            busy = true; msg = "…"
                            scope.launch {
                                try {
                                    val info = GitHubApi.repoInfo(p.first, p.second)
                                    val rel = try { GitHubApi.latestRelease(p.first, p.second) } catch (_: Exception) { null }
                                    val old = repo.all().firstOrNull { it.id == "${p.first}/${p.second}" }
                                    repo.upsert(StoreApp(p.first, p.second, p.second, info.description, old?.category ?: "tools",
                                        old?.status ?: "approved",
                                        rel?.tag ?: "", rel?.apkUrl ?: "", rel?.apkSize ?: 0L, info.iconUrl))
                                    msg = ""; input = ""; reload()
                                } catch (e: Exception) { msg = s("updError", e.message ?: e.javaClass.simpleName) }
                                busy = false
                            }
                        }) { Text(s("add")) }
                        if (msg.isNotEmpty()) Text(msg, color = QhColors.GoldSoft, fontSize = 13.sp)
                    }
                }
            }
            }
            if (shown.isEmpty()) item { Text(s("storeEmpty"), color = QhColors.Muted, fontSize = 14.sp) }
            items(shown, key = { it.id }) { a ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = QhColors.NavyCard)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(a.title, color = QhColors.Ink, fontSize = 18.sp)
                        Text("${a.id} · ${s(if (a.category == "news") "storeNews" else "storeTools")} · ${a.tag} · ${mbText(a.apkSize)}" +
                            (if (a.apkUrl.isEmpty()) " · " + s("noApkYet") else ""), color = QhColors.Muted, fontSize = 12.sp)
                        if (a.description.isNotBlank()) Text(a.description, color = QhColors.Ink, fontSize = 14.sp)
                        if (a.apkUrl.isNotEmpty() && a.status == "approved") {
                            Button(enabled = !busy, onClick = {
                                busy = true
                                scope.launch { downloadAndInstall(ctx, lang, a.apkUrl, a.name + ".apk") { msg = it }; busy = false }
                            }) { Text(s("download")) }
                        }
                        if (admin) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (a.status != "approved") TextButton(onClick = { repo.setStatus(a.id, "approved"); reload() }) { Text(s("approve"), color = QhColors.GoldSoft) }
                                if (a.status != "rejected") TextButton(onClick = { repo.setStatus(a.id, "rejected"); reload() }) { Text(s("reject"), color = QhColors.GoldSoft) }
                                TextButton(onClick = { repo.setCategory(a.id, if (a.category == "tools") "news" else "tools"); reload() }) { Text(s("changeCat"), color = QhColors.GoldSoft) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = {
                                    busy = true
                                    scope.launch {
                                        try {
                                            val rel = GitHubApi.latestRelease(a.owner, a.name)
                                            repo.upsert(a.copy(tag = rel.tag, apkUrl = rel.apkUrl ?: "", apkSize = rel.apkSize)); reload()
                                        } catch (e: Exception) { msg = s("updError", e.message ?: "") }
                                        busy = false
                                    }
                                }) { Text(s("refreshApp"), color = QhColors.GoldSoft) }
                                TextButton(onClick = { repo.remove(a.id); reload() }) { Text(s("delete"), color = QhColors.GoldSoft) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** التحديث المباشر للتطبيق نفسه من آخر Release في مستودع GitHub. */
@Composable
fun UpdateScreen(prefs: Prefs, onBack: () -> Unit) {
    var repoText by remember { mutableStateOf(prefs.updateRepo) }
    var info by remember { mutableStateOf("") }
    var apkUrl by remember { mutableStateOf("") }
    var apkName by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val lang = LocalLang.current
    fun s(k: String, vararg a: Any) = Strings.get(lang, k, *a)
    Column(Modifier.fillMaxSize().background(QhColors.NavyDeep).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("‹ " + s("back"), color = QhColors.GoldSoft) }
        Text(s("menuUpdate"), color = QhColors.Gold, fontSize = 24.sp)
        Text(s("updCurrent", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), color = QhColors.Ink, fontSize = 14.sp)
        Text(s("updRepo"), color = QhColors.Muted, fontSize = 13.sp)
        OutlinedTextField(value = repoText, onValueChange = { repoText = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("owner/name", color = QhColors.Muted) }, colors = fieldColors())
        Button(enabled = !busy && repoText.isNotBlank(), onClick = {
            val p = GitHubApi.parseRepo(repoText)
            if (p == null) { info = s("badRepo"); return@Button }
            prefs.updateRepo = repoText.trim(); busy = true; info = "…"; apkUrl = ""
            scope.launch {
                try {
                    val r = GitHubApi.latestRelease(p.first, p.second)
                    info = s("updLatest", r.tag, r.apkName ?: s("updNoApk")) + (if (r.apkSize > 0) "  (" + mbText(r.apkSize) + ")" else "")
                    apkUrl = r.apkUrl ?: ""; apkName = r.apkName ?: "update.apk"
                } catch (e: Exception) { info = s("updError", e.message ?: e.javaClass.simpleName) }
                busy = false
            }
        }) { Text(s("updCheck")) }
        if (info.isNotEmpty()) Text(info, color = QhColors.GoldSoft, fontSize = 14.sp)
        if (apkUrl.isNotEmpty()) Button(enabled = !busy, onClick = {
            busy = true
            scope.launch { downloadAndInstall(ctx, lang, apkUrl, apkName) { info = it }; busy = false }
        }) { Text(s("updDownload")) }
        Text(s("updOnlyDebug"), color = QhColors.Muted, fontSize = 12.sp)
    }
}
