package app.quranichorizons.data

import app.quranichorizons.BuildConfig

/**
 * بثّ مباشر لتلاوة السورة كاملة (ملف واحد) من mp3quran.net — تجربة في نسخة الاختبار فقط.
 * لا يُخزَّن أي ملف صوت. شروط استخدام mp3quran.net لم تُوثَّق بعد؛ لا يُفعَّل في الإصدار الرسمي قبل إذن/ترخيص مكتوب.
 * حدود الآيات داخل الملف تقديرية: تُوزَّع المدة الفعلية على الآيات بنسبة عدد حروفها (والبسملة في أول الملف).
 */
object SurahStream {
    const val RECITER_AR = "أحمد العجمي"
    const val SOURCE = "mp3quran.net"
    private const val BASE = "https://cdn.mp3quran.net/audio/ahmad-ajmi/r1/"

    val enabled: Boolean get() = BuildConfig.SURAH_STREAM_ENABLED

    /** رابط ملف السورة: رقم السورة بثلاث خانات (مثال: 114.mp3، 001.mp3). https ونطاق mp3quran فقط. */
    fun url(surah: Int): String? =
        if (surah in 1..114) "$BASE${surah.toString().padStart(3, '0')}.mp3" else null

    private fun letters(s: String): Int = s.count { it in 'ء'..'ي' || it == 'ٱ' }.coerceAtLeast(1)

    /**
     * نهايات الآيات كنسب تراكمية من مدة الملف (طولها = عدد الآيات، آخرها 1.0).
     * البسملة (إن كانت في أول الملف) تُحسب ضمن الآية الأولى.
     */
    fun ends(basmala: String?, ayahTexts: List<String>): DoubleArray {
        val w = ayahTexts.map { letters(it).toDouble() }.toMutableList()
        if (basmala != null && w.isNotEmpty()) w[0] = w[0] + letters(basmala)
        val total = w.sum()
        var acc = 0.0
        return DoubleArray(w.size) { acc += w[it]; acc / total }
    }

    fun indexAt(ends: DoubleArray, fraction: Double): Int {
        for (i in ends.indices) if (fraction < ends[i]) return i
        return ends.size - 1
    }

    fun startOf(ends: DoubleArray, i: Int): Double = if (i <= 0) 0.0 else ends[i - 1]
}
