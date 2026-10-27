package app.quranichorizons.player

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import app.quranichorizons.data.SurahStream

/**
 * مشغّل التلاوة على مستوى العملية (لا على مستوى الشاشة): يستمر الصوت حتى لو أُغلقت/أُعيد إنشاء النشاط،
 * وتحمله الخدمة الأمامية PlaybackService. الشاشة تقرأ الموضع منه فتتبعه الصورة، وتعيد الارتباط بدل إعادة التشغيل.
 * عند انتهاء السورة وتفعيل «تشغيل متتابع» يبدأ التالية بنفسه حتى والتطبيق في الخلفية.
 */
object PlaybackEngine {
    @Volatile var surah = 0; private set
    @Volatile var finished = false; private set
    @Volatile var error: String? = null; private set
    /** تضبطه الشاشة: يرجع true إن عالج انتهاء الملف بنفسه (مثل تكرار الآية). */
    @Volatile var hook: (() -> Boolean)? = null
    @Volatile var autoNext = true
    @Volatile var nextProvider: (Int) -> Int? = { null }

    private var appCtx: Context? = null
    private var player: AudioPlayer? = null

    fun player(ctx: Context): AudioPlayer {
        appCtx = ctx.applicationContext
        return player ?: AudioPlayer(onCompleted = { completed() }, onFailed = { error = it }, context = appCtx).also { player = it }
    }

    fun start(ctx: Context, s: Int, url: String) {
        val p = player(ctx)
        surah = s; finished = false; error = null
        p.play(url)
    }

    private fun completed() {
        if (hook?.invoke() == true) return
        finished = true
        if (autoNext) {
            val n = nextProvider(surah)
            val u = n?.let { SurahStream.url(it) }
            val c = appCtx
            if (n != null && u != null && c != null) start(c, n, u)
        }
    }

    fun startService(ctx: Context) {
        try { ContextCompat.startForegroundService(ctx, Intent(ctx, PlaybackService::class.java)) } catch (_: Exception) { }
    }

    fun stopService(ctx: Context) {
        try { ctx.stopService(Intent(ctx, PlaybackService::class.java)) } catch (_: Exception) { }
    }

    /** إيقاف كامل (خروج المستخدم من شاشة التشغيل). */
    fun stop(ctx: Context) {
        player?.release()
        surah = 0; finished = false; error = null
        stopService(ctx)
    }
}
