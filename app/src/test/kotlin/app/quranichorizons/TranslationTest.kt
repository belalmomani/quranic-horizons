package app.quranichorizons

import app.quranichorizons.data.TranslationFile
import kotlinx.serialization.json.Json
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationTest {
    private val json = Json { ignoreUnknownKeys = true }
    private fun root(): File {
        var d: File? = File("").absoluteFile
        while (d != null && !File(d, "content/bootstrap/catalog.json").exists()) d = d.parentFile
        return File(d!!, "content/bootstrap")
    }

    @Test fun translationFilesCoverEveryTafsirRecordAndNeverTranslateQuran() {
        val langs = setOf("en", "zh", "hi", "es", "fr", "bn")
        for (surah in File(root(), "modules").list()!!.filter { File(root(), "modules/$it/translations").isDirectory }.sorted()) {
            val dir = File(root(), "modules/$surah")
            val ids = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").findAll(File(dir, "tafsir.json").readText()).map { it.groupValues[1] }.toSet()
            val tdir = File(dir, "translations")
            assertEquals(langs, tdir.listFiles()!!.map { it.nameWithoutExtension }.toSet())
            for (f in tdir.listFiles()!!) {
                val t = json.decodeFromString(TranslationFile.serializer(), f.readText())
                assertEquals(f.nameWithoutExtension, t.lang)
                assertTrue("لا اعتماد بلا مراجع بشري", t.status != "approved")
                assertTrue(t.tafsir.isNotEmpty() && t.tafsir.keys.all { it in ids } && t.tafsir.values.all { it.isNotBlank() })
            }
        }
    }
}
