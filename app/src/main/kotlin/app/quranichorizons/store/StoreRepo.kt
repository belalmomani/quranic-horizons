package app.quranichorizons.store

import app.quranichorizons.data.Prefs
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** تطبيق في «متجري». الحالة: pending / approved / rejected. التصنيف: tools / news. */
@Serializable
data class StoreApp(
    val owner: String,
    val name: String,
    val title: String,
    val description: String = "",
    val category: String = "tools",
    val status: String = "pending",
    val tag: String = "",
    val apkUrl: String = "",
    val apkSize: Long = 0L,
    val iconUrl: String = "",
) { val id: String get() = "$owner/$name" }

/** قائمة التطبيقات محفوظة على الجهاز فقط (لا خادم). الاعتماد/الرفض من لوحة الإدارة. */
class StoreRepo(private val prefs: Prefs) {
    private val json = Json { ignoreUnknownKeys = true }

    fun all(): List<StoreApp> = try { json.decodeFromString<List<StoreApp>>(prefs.storeJson.ifBlank { "[]" }) } catch (_: Exception) { emptyList() }

    private fun save(l: List<StoreApp>) { prefs.storeJson = json.encodeToString(l) }

    fun upsert(a: StoreApp) { save(all().filterNot { it.id == a.id } + a) }
    fun remove(id: String) { save(all().filterNot { it.id == id }) }
    fun setStatus(id: String, status: String) { save(all().map { if (it.id == id) it.copy(status = status) else it }) }
    fun setCategory(id: String, category: String) { save(all().map { if (it.id == id) it.copy(category = category) else it }) }
}
