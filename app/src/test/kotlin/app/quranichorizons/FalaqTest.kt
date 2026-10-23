package app.quranichorizons

import app.quranichorizons.data.Catalog
import app.quranichorizons.data.ContentVerifier
import app.quranichorizons.data.ModuleFile
import app.quranichorizons.data.SceneFile
import app.quranichorizons.data.SurahsFile
import app.quranichorizons.data.TafsirFile
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** وحدة الفلق (113): بنية سليمة، اقتباسات حرفية، ولا اعتماد وفيها معوّقات (صور معاينة، تلاوة، نسبة التفاسير). */
class FalaqTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val root = File("../content/bootstrap")
    private fun read(p: String) = File(root, p).readText(Charsets.UTF_8)
    private val surahs = json.decodeFromString(SurahsFile.serializer(), read("surahs.json"))
    private val catalog = json.decodeFromString(Catalog.serializer(), read("catalog.json"))
    private val m = catalog.modules.first { it.surah == 113 }
    private val module = json.decodeFromString(ModuleFile.serializer(), read("${m.dir}/module.json"))
    private val scenes = json.decodeFromString(SceneFile.serializer(), read("${m.dir}/scenes.json")).scenes.associateBy { it.id }
    private val tafsir = json.decodeFromString(TafsirFile.serializer(), read("${m.dir}/tafsir.json")).records

    @Test fun structureIsSoundWithFiveAyahs() {
        val info = surahs.surahs.first { it.number == 113 }
        assertEquals(emptyList<String>(), ContentVerifier.structureProblems(info, module, scenes))
        assertEquals(5, module.ayahs.size)
    }

    @Test fun quranTextIsNotTouchedByBasmalaSplit() {
        assertTrue(module.ayahs[0].text.startsWith("قُلْ"))
        assertEquals("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", module.basmala)
    }

    @Test fun beatQuotesAreVerbatimFromMuyassarRecords() {
        val byId = tafsir.associateBy { it.id }
        for (sc in scenes.values) for (b in sc.beats) {
            val rec = byId.getValue(b.tafsirRecord)
            assertTrue("آية ${b.ayah}", b.ayah in rec.ayahs && rec.text.contains(b.tafsirQuote))
        }
    }

    @Test fun notApprovedWhileBlockersRemain() {
        assertFalse(ContentVerifier.isFullyApproved(module, scenes, tafsir))
        assertTrue(m.status != "approved")
        assertTrue(module.review.blockers.any { it.contains("دقة") })
    }
}
