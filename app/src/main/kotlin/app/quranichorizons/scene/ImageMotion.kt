package app.quranichorizons.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.util.Random
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** حركة مبرمجة للصور الثابتة (بلا صوت): تقريب/إزاحة بطيئة + مؤثرات بوسوم fx: water crowd clouds rays dust rain flash fire mist sparkle. كلها دوال في t؛ عند t=0 ساكنة. */
internal fun DrawScope.drawLivingImage(bmp: ImageBitmap, fx: String, alpha: Float, z: Float, t: Float, seed: Int) {
    val a = alpha.coerceIn(0f, 1f)
    val w = size.width
    val h0 = w * bmp.height / bmp.width
    val top = size.height * 0.07f
    val tags = fx.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet().ifEmpty { setOf("dust", "clouds") }
    val sd = (seed % 997).toFloat()
    val k = 1.05f + 0.025f * sin(t * 0.12f + sd) + 0.04f * z.coerceIn(0f, 1f)
    val dw = w * k
    val dh = h0 * k
    val offX = (w - dw) / 2f + sin(t * 0.07f + sd) * 0.02f * w
    val offY = top - (dh - h0) / 2f + cos(t * 0.05f + sd) * 0.012f * h0
    clipRect(left = 0f, top = top, right = w, bottom = top + h0) {
        when {
            "water" in tags -> drawWaterStrips(bmp, a, t, offX, offY, dw, dh)
            "crowd" in tags -> drawCrowdStrips(bmp, a, t, offX, offY, dw, dh)
            else -> drawImage(bmp, dstOffset = IntOffset(offX.toInt(), offY.toInt()), dstSize = IntSize(dw.toInt(), dh.toInt()), alpha = a, filterQuality = FilterQuality.High)
        }
        if ("clouds" in tags) drawGlow(a, t, w, top, h0, sd, 0.012f, 0.12f, 0.1f, 0.30f, 0.10f, Color.White)
        if ("mist" in tags) drawGlow(a, t, w, top, h0, sd, 0.02f, 0.72f, 0.07f, 0.38f, 0.11f, Color.White)
        if ("rays" in tags) drawRaysFx(a, t, w, top, h0, sd)
        if ("fire" in tags) drawFireFx(a, t, w, top, h0)
        if ("rain" in tags) drawRainFx(a, t, w, top, h0, sd)
        if ("dust" in tags) drawDustFx(a, t, w, top, h0, sd)
        if ("sparkle" in tags) drawSparkleFx(a, t, w, top, h0, sd)
        if ("flash" in tags) drawFlashFx(a, t, w, top, h0, sd)
    }
}

private fun DrawScope.drawWaterStrips(bmp: ImageBitmap, a: Float, t: Float, offX: Float, offY: Float, dw: Float, dh: Float) {
    val n = 40; val sh = max(1, bmp.height / n); val stripH = dh / n
    for (i in 0 until n) {
        val ramp = ((i / n.toFloat() - 0.3f) / 0.7f).coerceIn(0f, 1f)
        val shift = (sin(t * 1.3f + i * 0.55f) * 0.006f + sin(t * 0.7f + i * 0.21f) * 0.003f) * dw * ramp
        val sy = i * sh
        val srcH = if (i == n - 1) bmp.height - sy else sh
        drawImage(bmp, srcOffset = IntOffset(0, sy), srcSize = IntSize(bmp.width, srcH),
            dstOffset = IntOffset((offX + shift).toInt(), (offY + i * stripH).toInt()),
            dstSize = IntSize(dw.toInt(), ceil(stripH).toInt() + 1), alpha = a, filterQuality = FilterQuality.Low)
    }
}

private fun DrawScope.drawCrowdStrips(bmp: ImageBitmap, a: Float, t: Float, offX: Float, offY: Float, dw: Float, dh: Float) {
    val m = 48; val sw = max(1, bmp.width / m); val colW = dw / m
    for (j in 0 until m) {
        val sx = j * sw
        val srcW = if (j == m - 1) bmp.width - sx else sw
        val bob = (sin(t * 2.0f + j * 0.9f) * 0.0035f + sin(t * 0.9f + j * 0.31f) * 0.0025f) * dh
        drawImage(bmp, srcOffset = IntOffset(sx, 0), srcSize = IntSize(srcW, bmp.height),
            dstOffset = IntOffset((offX + j * colW).toInt(), (offY + bob).toInt()),
            dstSize = IntSize(ceil(colW).toInt() + 1, dh.toInt()), alpha = a, filterQuality = FilterQuality.Low)
    }
}

private fun DrawScope.drawGlow(a: Float, t: Float, w: Float, top: Float, h0: Float, sd: Float, speed: Float, y0: Float, dy: Float, rad: Float, op: Float, col: Color) {
    for (i in 0 until 3) {
        val u = ((t * speed * (1f + i * 0.4f) + i * 0.4f + sd * 0.01f) % 1.4f) - 0.2f
        val c = Offset(u * w, top + h0 * (y0 + dy * i))
        val r = w * rad
        drawCircle(Brush.radialGradient(listOf(col.copy(alpha = op * a), Color.Transparent), center = c, radius = r), radius = r, center = c)
    }
}

private fun DrawScope.drawRaysFx(a: Float, t: Float, w: Float, top: Float, h0: Float, sd: Float) {
    val ox = w * 0.5f + sin(t * 0.1f + sd) * 0.12f * w
    val oy = top - h0 * 0.05f
    for (i in 0 until 5) {
        val ang = (-0.55f + i * 0.275f) + sin(t * 0.15f + i) * 0.03f
        val len = h0 * 1.3f; val sp = 0.07f
        val p = Path().apply {
            moveTo(ox, oy)
            lineTo(ox + sin(ang - sp) * len, oy + cos(ang - sp) * len)
            lineTo(ox + sin(ang + sp) * len, oy + cos(ang + sp) * len)
            close()
        }
        drawPath(p, Color(0xFFFFE2A0).copy(alpha = ((0.05f + 0.03f * sin(t * 0.5f + i * 1.3f)) * a).coerceIn(0f, 1f)))
    }
}

private fun DrawScope.drawFireFx(a: Float, t: Float, w: Float, top: Float, h0: Float) {
    val f = 0.12f + 0.06f * sin(t * 7f) + 0.04f * sin(t * 13.3f + 1f) + 0.03f * sin(t * 3.1f)
    val c = Offset(w * 0.68f, top + h0 * 0.62f); val r = w * 0.55f
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFF7A1A).copy(alpha = (f * a).coerceIn(0f, 1f)), Color.Transparent), center = c, radius = r), radius = r, center = c)
}

private fun DrawScope.drawRainFx(a: Float, t: Float, w: Float, top: Float, h0: Float, sd: Float) {
    val rnd = Random(sd.toLong() + 7)
    for (i in 0 until 70) {
        val x0 = rnd.nextFloat(); val y0 = rnd.nextFloat(); val sp = 0.8f + rnd.nextFloat() * 0.6f
        val y = (y0 + t * 1.4f * sp) % 1f
        val p = Offset((x0 + y * 0.05f) * w, top + y * h0)
        drawLine(Color(0xFFD5E6FF).copy(alpha = 0.28f * a), p, Offset(p.x - w * 0.004f, p.y + h0 * 0.045f), strokeWidth = w * 0.0022f)
    }
}

private fun DrawScope.drawDustFx(a: Float, t: Float, w: Float, top: Float, h0: Float, sd: Float) {
    val rnd = Random(sd.toLong() + 3)
    for (i in 0 until 36) {
        val x0 = rnd.nextFloat(); val y0 = rnd.nextFloat(); val sp = 0.5f + rnd.nextFloat(); val s = 0.5f + rnd.nextFloat() * 1.5f
        val x = ((x0 + t * 0.010f * sp) % 1f) * w
        val y = top + (((y0 - t * 0.006f * sp) % 1f + 1f) % 1f) * h0
        drawCircle(Color(0xFFFFE8B8).copy(alpha = (0.28f * (0.5f + 0.5f * sin(t * 0.9f + i)) * a).coerceIn(0f, 1f)), w * 0.0022f * s, Offset(x, y))
    }
}

private fun DrawScope.drawSparkleFx(a: Float, t: Float, w: Float, top: Float, h0: Float, sd: Float) {
    val rnd = Random(sd.toLong() + 11)
    for (i in 0 until 22) {
        val x = rnd.nextFloat() * w; val y = top + rnd.nextFloat() * h0
        val s = sin(t * 1.5f + rnd.nextFloat() * 2f * PI.toFloat())
        val al = (s * s * 0.85f * a).coerceIn(0f, 1f); val r = w * 0.004f
        drawCircle(Color(0xFFFFE9A8).copy(alpha = al), r, Offset(x, y))
        drawLine(Color(0xFFFFF4CC).copy(alpha = al * 0.7f), Offset(x - r * 3, y), Offset(x + r * 3, y), strokeWidth = r * 0.5f)
        drawLine(Color(0xFFFFF4CC).copy(alpha = al * 0.7f), Offset(x, y - r * 3), Offset(x, y + r * 3), strokeWidth = r * 0.5f)
    }
}

private fun DrawScope.drawFlashFx(a: Float, t: Float, w: Float, top: Float, h0: Float, sd: Float) {
    fun f(ph: Float): Float = if (ph < 0.06f) 1f - ph / 0.06f else 0f
    val ph = (t * 0.33f + sd * 0.013f) % 1f
    val v = max(f(ph), f((ph + 0.43f) % 1f)) * 0.30f * a
    if (v > 0.01f) drawRect(Color.White.copy(alpha = v.coerceIn(0f, 1f)), topLeft = Offset(0f, top), size = Size(w, h0))
}
