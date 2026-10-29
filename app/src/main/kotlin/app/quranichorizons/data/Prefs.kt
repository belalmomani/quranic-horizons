package app.quranichorizons.data

import android.content.Context

/** تفضيلات المستخدم: آخر موضع، تقليل الحركة، حجم النص، وتيرة العرض. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("qh_prefs", Context.MODE_PRIVATE)

    var lastSurah: Int
        get() = sp.getInt("last_surah", 0)
        set(v) = sp.edit().putInt("last_surah", v).apply()

    var lastAyah: Int
        get() = sp.getInt("last_ayah", 1)
        set(v) = sp.edit().putInt("last_ayah", v).apply()

    var reduceMotion: Boolean
        get() = sp.getBoolean("reduce_motion", false)
        set(v) = sp.edit().putBoolean("reduce_motion", v).apply()

    /** مضاعف حجم نص الآية (1.0 = الافتراضي) */
    var textScale: Float
        get() = sp.getFloat("text_scale", 1.0f)
        set(v) = sp.edit().putFloat("text_scale", v).apply()

    /** ثواني بقاء كل آية في وضع القراءة (حين لا تتوفر تلاوة) */
    var paceSeconds: Int
        get() = sp.getInt("pace", 9)
        set(v) = sp.edit().putInt("pace", v).apply()

    /** عنوان خدمة التلاوة الوسيطة المُدخَل في نسخة الاختبار (فارغ = قيمة البناء) */
    var audioBase: String
        get() = sp.getString("audio_base", "") ?: ""
        set(v) = sp.edit().putString("audio_base", v).apply()

    /** رمز اللغة المختار يدويًا؛ فارغ = حسب لغة الجهاز */
    var langOverride: String
        get() = sp.getString("lang", "") ?: ""
        set(v) = sp.edit().putString("lang", v).apply()

    /** تشغيل السور تباعًا: عند انتهاء السورة تبدأ التالية المتاحة تلقائيًا */
    var autoNextSurah: Boolean
        get() = sp.getBoolean("auto_next", true)
        set(v) = sp.edit().putBoolean("auto_next", v).apply()

    /** قائمة تطبيقات «متجري» (JSON) */
    var storeJson: String
        get() = sp.getString("store_json", "") ?: ""
        set(v) = sp.edit().putString("store_json", v).apply()

    /** مستودع GitHub (owner/name) للتحديث المباشر للتطبيق */
    var updateRepo: String
        get() = sp.getString("update_repo", "") ?: ""
        set(v) = sp.edit().putString("update_repo", v).apply()

    /** بريد الحساب المسجَّل دخوله (يُملأ بعد ربط تسجيل الدخول)؛ لا يُعدّ ضمانًا أمنيًا */
    var accountEmail: String
        get() = sp.getString("account_email", "") ?: ""
        set(v) = sp.edit().putString("account_email", v).apply()
}
