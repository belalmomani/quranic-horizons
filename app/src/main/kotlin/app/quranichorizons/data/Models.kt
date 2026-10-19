package app.quranichorizons.data

import kotlinx.serialization.Serializable

@Serializable
data class TextSource(
    val name: String = "",
    val url: String = "",
    val license: String = "",
    val licenseUrl: String = "",
)

@Serializable
data class SurahInfo(
    val number: Int,
    val nameAr: String,
    val nameEn: String = "",
    val meaning: String = "",
    val revelation: String = "",
    val ayahCount: Int,
)

@Serializable
data class SurahsFile(
    val formatVersion: Int,
    val textSource: TextSource = TextSource(),
    val deliveryOrder: List<Int> = emptyList(),
    val surahs: List<SurahInfo>,
)

@Serializable
data class FileEntry(val path: String, val sha256: String, val bytes: Long)

@Serializable
data class CatalogModule(
    val surah: Int,
    val version: String,
    val status: String,
    val dir: String,
    val files: List<FileEntry>,
    val totalBytes: Long = 0,
)

@Serializable
data class Catalog(val formatVersion: Int, val appMinVersion: Int = 1, val modules: List<CatalogModule>)

@Serializable
data class AudioInfo(val url: String, val durationMs: Long? = null, val status: String = "license_unverified")

@Serializable
data class AyahEntry(val number: Int, val text: String, val sceneId: String, val audio: AudioInfo)

@Serializable
data class ReviewInfo(val status: String = "draft", val blockers: List<String> = emptyList())

@Serializable
data class ModuleText(val source: String = "", val sha256OfSourceLines: String = "", val note: String = "")

@Serializable
data class ModuleFile(
    val formatVersion: Int,
    val surah: Int,
    val moduleVersion: String,
    val nameAr: String,
    val ayahCount: Int,
    val review: ReviewInfo = ReviewInfo(),
    val basmala: String? = null,
    val text: ModuleText = ModuleText(),
    val ayahs: List<AyahEntry>,
)

@Serializable
data class TafsirRecord(
    val id: String,
    val bookAr: String,
    val authorAr: String,
    val scope: String = "surah",
    val ayahs: List<Int> = emptyList(),
    val text: String,
    val origin: String = "",
    val attribution: String = "",
    val sourceNotes: List<String> = emptyList(),
    val licenseStatus: String = "unverified",
    val attributionReview: String = "pending",
)

@Serializable
data class TafsirFile(val formatVersion: Int, val surah: Int, val records: List<TafsirRecord>)

@Serializable
data class Glow(val x: Float, val y: Float, val r: Float, val color: String, val alpha: Float)

@Serializable
data class Ridge(val h: Float, val color: String)

@Serializable
data class SceneSpec(
    val skyTop: String,
    val skyBottom: String,
    val stars: Int = 80,
    val twinkle: Float = 0.3f,
    val glow: Glow? = null,
    val ridges: List<Ridge> = emptyList(),
    val mist: Float = 0f,
    val shadowEdge: Float = 0f,
    val shadowRetreat: Boolean = false,
    val drift: Float = 0.2f,
)

@Serializable
data class SceneReview(val status: String = "draft", val note: String = "")

@Serializable
data class SceneTransition(val type: String = "crossfade", val ms: Int = 1600)

@Serializable
data class SceneDef(
    val id: String,
    val ayahs: List<Int>,
    val kind: String = "procedural",
    val title: String = "",
    val tafsirQuote: String = "",
    val spec: SceneSpec,
    val review: SceneReview = SceneReview(),
    val transition: SceneTransition = SceneTransition(),
)

@Serializable
data class SceneFile(val formatVersion: Int, val surah: Int, val scenes: List<SceneDef>)

/** وحدة سورة كاملة محمّلة في الذاكرة. */
data class SurahModule(
    val info: SurahInfo,
    val catalog: CatalogModule,
    val module: ModuleFile,
    val tafsir: List<TafsirRecord>,
    val scenes: Map<String, SceneDef>,
)

/** حالة سورة في القائمة. */
enum class SurahAvailability { PREVIEW, IN_PREPARATION, READY }
