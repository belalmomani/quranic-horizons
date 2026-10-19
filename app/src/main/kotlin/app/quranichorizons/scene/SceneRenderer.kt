package app.quranichorizons.scene

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import app.quranichorizons.data.SceneSpec
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

private fun hex(c: String, alpha: Float = 1f): Color =
    Color(android.graphics.Color.parseColor(c)).copy(alpha = alpha.coerceIn(0f, 1f))

/**
 * يرسم مشهدًا رمزيًا مجرّدًا. لا تجسيد لأي كائن ديني: سماء وضوء وأفق وضباب وظلال فقط.
 * timeMs يحرّك الوميض والانجراف؛ sceneProgress (0..1) لتراجع الظل في المشاهد التي تحتاجه.
 */
fun DrawScope.drawScene(spec: SceneSpec, alpha: Float, timeMs: Long, sceneProgress: Float, animate: Boolean) {
    val w = size.width
    val h = size.height
    val t = if (animate) timeMs / 1000f else 0f

    drawRect(
        brush = Brush.verticalGradient(listOf(hex(spec.skyTop, alpha), hex(spec.skyBottom, alpha))),
    )

    // نجوم ثابتة المواضع (بذرة ثابتة) ووميضها خفيف
    val rnd = Random(42)
    for (i in 0 until spec.stars) {
        val x = rnd.nextFloat() * w
        val y = rnd.nextFloat() * h * 0.72f
        val r = 0.6f + rnd.nextFloat() * 1.5f
        val phase = rnd.nextFloat() * (2f * PI.toFloat())
        val speed = 0.4f + rnd.nextFloat() * 0.8f
        val base = 0.35f + rnd.nextFloat() * 0.6f
        val tw = 1f - spec.twinkle * 0.5f * (1f + sin(t * speed + phase))
        drawCircle(Color.White.copy(alpha = (base * tw * alpha).coerceIn(0f, 1f)), r, Offset(x, y))
    }

    spec.glow?.let { g ->
        val pulse = 1f + 0.04f * sin(t * 0.6f)
        val center = Offset(g.x * w, g.y * h)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(hex(g.color, g.alpha * alpha), Color.Transparent),
                center = center,
                radius = g.r * w * pulse,
            ),
        )
    }

    if (spec.mist > 0f) {
        for (band in 0 until 3) {
            val y0 = h * (0.50f + band * 0.12f)
            val dx = (sin(t * spec.drift * 0.3f + band) * 0.04f) * w
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = spec.mist * 0.10f * alpha), Color.Transparent),
                    startY = y0,
                    endY = y0 + h * 0.16f,
                ),
                topLeft = Offset(dx, y0),
                size = androidx.compose.ui.geometry.Size(w, h * 0.16f),
            )
        }
    }

    spec.ridges.forEachIndexed { idx, ridge ->
        val base = h * (1f - ridge.h)
        val p = Path()
        p.moveTo(0f, h)
        val steps = 24
        for (s in 0..steps) {
            val x = w * s / steps
            val wobble = sin(s * 0.9f + idx * 2f) * h * 0.012f + sin(s * 0.37f + idx) * h * 0.018f
            p.lineTo(x, base + wobble)
        }
        p.lineTo(w, h)
        p.close()
        drawPath(p, hex(ridge.color, alpha))
    }

    if (spec.shadowEdge > 0f) {
        val k = if (spec.shadowRetreat) 1f - 0.65f * sceneProgress.coerceIn(0f, 1f) else 1f
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = (spec.shadowEdge * k * alpha).coerceIn(0f, 1f))),
                center = Offset(w / 2f, h / 2f),
                radius = maxOf(w, h) * 0.75f,
            ),
        )
    }
}

@Composable
fun SceneCanvas(
    from: SceneSpec?,
    to: SceneSpec,
    transition: Float,
    timeMs: Long,
    sceneProgress: Float,
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val p = transition.coerceIn(0f, 1f)
        if (from != null && p < 1f) {
            drawScene(from, 1f, timeMs, sceneProgress, animate)
            drawScene(to, p, timeMs, sceneProgress, animate)
        } else {
            drawScene(to, 1f, timeMs, sceneProgress, animate)
        }
    }
}
