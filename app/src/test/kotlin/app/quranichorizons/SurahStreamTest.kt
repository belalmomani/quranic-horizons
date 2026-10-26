package app.quranichorizons

import app.quranichorizons.data.SurahStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SurahStreamTest {
    @Test fun urlIsPaddedAndHostFixed() {
        assertEquals("https://cdn.mp3quran.net/audio/ahmad-ajmi/r1/114.mp3", SurahStream.url(114))
        assertEquals("https://cdn.mp3quran.net/audio/ahmad-ajmi/r1/001.mp3", SurahStream.url(1))
        assertEquals(null, SurahStream.url(0))
        assertEquals(null, SurahStream.url(115))
    }

    @Test fun endsAreCumulativeAndLastIsOne() {
        val e = SurahStream.ends("بِسْمِ ٱللَّهِ", listOf("قُلْ", "مَلِكِ ٱلنَّاسِ", "إِلَـٰهِ ٱلنَّاسِ"))
        assertEquals(3, e.size)
        assertEquals(1.0, e.last(), 1e-9)
        assertTrue(e[0] < e[1] && e[1] < e[2])
        assertEquals(0, SurahStream.indexAt(e, 0.0))
        assertEquals(2, SurahStream.indexAt(e, 0.9999))
        assertEquals(2, SurahStream.indexAt(e, 1.0))
        assertEquals(0.0, SurahStream.startOf(e, 0), 0.0)
        assertEquals(e[0], SurahStream.startOf(e, 1), 0.0)
    }
}
