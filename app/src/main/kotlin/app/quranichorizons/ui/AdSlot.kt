package app.quranichorizons.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.viewinterop.AndroidView
import app.quranichorizons.BuildConfig
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** ارتفاع الشريط المحجوز للإعلان أسفل الشاشات الجانبية (بانر قياسي 50dp + هامش). */
val AD_SLOT_HEIGHT = 56.dp

/**
 * شريط مخصص للإعلان فقط، في أسفل الشاشة تحت كل الأزرار والمحتوى، لا يغطي شيئًا.
 * يبقى محجوزًا بنفس الارتفاع سواء حُمّل إعلان أم لا، فلا يقفز التخطيط.
 * لا يُستخدم في شاشة التشغيل (التلاوة) ولا فوق نص الآيات أو التفسير.
 * عند دمج Start.io يوضع بانره داخل هذا الصندوق فقط.
 */
@Composable
fun AdSlot(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(AD_SLOT_HEIGHT).background(QhColors.NavyDeep), contentAlignment = Alignment.Center) {
        if (BuildConfig.ADS_ENABLED) {
            AndroidView(factory = { ctx ->
                try { com.startapp.sdk.ads.banner.Banner(ctx) } catch (e: Throwable) { android.view.View(ctx) }
            })
        }
    }
}
