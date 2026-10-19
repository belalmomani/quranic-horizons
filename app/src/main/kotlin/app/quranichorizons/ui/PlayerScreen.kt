package app.quranichorizons.ui

import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.quranichorizons.BuildConfig
import app.quranichorizons.data.Catalog
import app.quranichorizons.data.ContentRepository
import app.quranichorizons.data.Prefs
import app.quranichorizons.data.SceneSpec
import app.quranichorizons.data.SurahsFile
import app.quranichorizons.player.AudioPlayer
import app.quranichorizons.scene.SceneCanvas
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    repo: ContentRepository,
    prefs: Prefs,
    surahs: SurahsFile,
    catalog: Catalog,
    number: Int,
    startAyah: Int,
    onExit: () -> Unit,
    onToList: () -> Unit,
) {
    val (mod, error) = remember(number) { loadModuleSafely(repo, surahs, catalog, number) }
    if (mod == null) {
        Column(Modifier.fillMaxSize().background(QhColors.NavyDeep).padding(24.dp)) {
            Text(error ?: "تعذر التحميل", color = QhColors.Gold, fontSize = 18.sp)
            TextButton(onClick = onExit) { Text("رجوع", color = QhColors.GoldSoft) }
        }
        return
    }
    val ayahs = mod.module.ayahs
    var index by remember { mutableStateOf((startAyah - 1).coerceIn(0, ayahs.size - 1)) }
    var playing by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }
    var showTafsir by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var reduce by remember { mutableStateOf(prefs.reduceMotion) }
    var scale by remember { mutableStateOf(prefs.textScale) }
    var pace by remember { mutableStateOf(prefs.paceSeconds) }
    var audioFailed by remember { mutableStateOf<String?>(null) }
    var replayKey by remember { mutableStateOf(0) }

    val audioMode = BuildConfig.AUDIO_ENABLED && audioFailed == null
    val active = playing && !showTafsir && !showSettings && !finished
    val ayah = ayahs[index]
    val scene = mod.scenes.getValue(ayah.sceneId)

    // حفظ الموضع
    LaunchedEffect(index) { prefs.lastSurah = number; prefs.lastAyah = index + 1 }

    // الانتقال بين المشاهد: مزج تدريجي (أو قطع فوري عند تقليل الحركة)
    var fromSpec by remember { mutableStateOf<SceneSpec?>(null) }
    var curSpec by remember { mutableStateOf(scene.spec) }
    var curId by remember { mutableStateOf(scene.id) }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(scene.id, replayKey) {
        if (scene.id != curId) {
            fromSpec = curSpec
            curSpec = scene.spec
            curId = scene.id
            if (reduce) fade.snapTo(1f) else {
                fade.snapTo(0f)
                fade.animateTo(1f, androidx.compose.animation.core.tween(scene.transition.ms))
            }
            fromSpec = null
        }
    }

    // ساعة الحركة
    var timeMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(active, reduce) {
        if (active && !reduce) {
            var last = withFrameMillis { it }
            while (true) {
                val now = withFrameMillis { it }
                timeMs += now - last
                last = now
            }
        }
    }

    // وضع القراءة: مؤقت لكل آية حين لا تتوفر تلاوة
    LaunchedEffect(index, active, audioMode, pace, replayKey) {
        if (!audioMode && active) {
            delay(pace * 1000L)
            if (index < ayahs.size - 1) index += 1 else finished = true
        }
    }

    // التلاوة (معطّلة في هذا الإصدار؛ تُفعَّل بعد التحقق من ترخيص القارئ)
    val advanceState = rememberUpdatedState({ if (index < ayahs.size - 1) index += 1 else finished = true })
    val player = remember {
        AudioPlayer(
            onCompleted = { advanceState.value() },
            onFailed = { audioFailed = it },
        )
    }
    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(index, audioMode, replayKey) {
        if (audioMode) player.play(ayah.audio.url)
    }
    LaunchedEffect(active, audioMode) {
        if (audioMode) { if (active) player.resume() else player.pause() }
    }

    // إيقاف عند خروج التطبيق للخلفية
    val ctx = LocalContext.current
    DisposableEffect(ctx) {
        val lc = (ctx as? ComponentActivity)?.lifecycle
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) playing = false }
        lc?.addObserver(obs)
        onDispose { lc?.removeObserver(obs) }
    }

    Box(Modifier.fillMaxSize().background(QhColors.NavyDeep)) {
        SceneCanvas(
            from = fromSpec, to = curSpec, transition = fade.value,
            timeMs = timeMs, sceneProgress = (index + 1f) / ayahs.size,
            animate = active && !reduce, modifier = Modifier.fillMaxSize(),
        )
        // غلالة تحمي وضوح النص دون حجب المشهد
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0x66060D25), Color(0x22060D25), Color(0xCC060D25))),
            ),
        )
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onExit) { Text("‹", color = QhColors.GoldSoft, fontSize = 22.sp) }
                Text(
                    "سورة ${mod.info.nameAr} — ${index + 1}/${ayahs.size}",
                    color = QhColors.Ink, fontSize = 15.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                )
                TextButton(onClick = { playing = false; showSettings = true }) { Text("⚙", color = QhColors.GoldSoft, fontSize = 20.sp) }
            }
            if (mod.module.review.status != "approved") {
                Text(
                    "معاينة قيد المراجعة — المشاهد تجريبية غير معتمدة",
                    color = QhColors.GoldSoft, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (index == 0 && mod.module.basmala != null) {
                        Text(
                            mod.module.basmala, style = AyahStyle, color = QhColors.GoldSoft,
                            fontSize = (22 * scale).sp, lineHeight = (40 * scale).sp, textAlign = TextAlign.Center,
                        )
                    }
                    Text(
                        ayah.text, style = AyahStyle, fontSize = (30 * scale).sp, lineHeight = (54 * scale).sp,
                        textAlign = TextAlign.Center,
                    )
                    if (scene.title.isNotBlank()) {
                        Text(scene.title, color = QhColors.Muted, fontSize = 13.sp)
                    }
                }
            }
            Slider(
                value = index.toFloat(),
                onValueChange = { index = it.toInt().coerceIn(0, ayahs.size - 1); finished = false },
                valueRange = 0f..(ayahs.size - 1).toFloat().coerceAtLeast(1f),
                steps = (ayahs.size - 2).coerceAtLeast(0),
            )
            Row(
                Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { if (index > 0) { index -= 1; finished = false } }) { Text("السابقة", color = QhColors.Ink) }
                Button(
                    onClick = { if (finished) { index = 0; finished = false; replayKey++; playing = true } else playing = !playing },
                    colors = ButtonDefaults.buttonColors(containerColor = QhColors.Gold, contentColor = QhColors.NavyDeep),
                ) { Text(if (playing && !finished) "إيقاف مؤقت" else "تشغيل") }
                TextButton(onClick = { if (index < ayahs.size - 1) index += 1 }) { Text("التالية", color = QhColors.Ink) }
            }
            TextButton(
                onClick = { playing = false; showTafsir = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("التفسير ومصدر المشهد", color = QhColors.GoldSoft) }
        }

        if (finished) {
            Box(Modifier.fillMaxSize().background(Color(0xDD060D25)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("انتهت سورة ${mod.info.nameAr}", color = QhColors.Gold, fontSize = 24.sp)
                    Button(
                        onClick = { index = 0; finished = false; playing = true; replayKey++ },
                        colors = ButtonDefaults.buttonColors(containerColor = QhColors.Gold, contentColor = QhColors.NavyDeep),
                    ) { Text("إعادة الرحلة") }
                    TextButton(onClick = onToList) { Text("العودة إلى السور", color = QhColors.GoldSoft) }
                }
            }
        }
    }

    if (showTafsir) {
        ModalBottomSheet(onDismissRequest = { showTafsir = false }, containerColor = QhColors.NavyCard) {
            var tab by remember { mutableStateOf(0) }
            val recs = mod.tafsir
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("سبب اختيار هذا المشهد", color = QhColors.Gold, fontSize = 16.sp)
                if (scene.tafsirQuote.isNotBlank()) {
                    Text(scene.tafsirQuote, color = QhColors.Ink, fontSize = 15.sp, lineHeight = 26.sp)
                    Text("المقطع أعلاه نقل حرفي من تفسير السعدي، والنسبة قيد المراجعة.", color = QhColors.Muted, fontSize = 12.sp)
                }
                if (recs.isNotEmpty()) {
                    TabRow(selectedTabIndex = tab.coerceIn(0, recs.size - 1), containerColor = QhColors.NavyCard) {
                        recs.forEachIndexed { i, r ->
                            Tab(selected = tab == i, onClick = { tab = i }, text = { Text(r.bookAr, fontSize = 13.sp) })
                        }
                    }
                    val r = recs[tab.coerceIn(0, recs.size - 1)]
                    Text("${r.bookAr} — ${r.authorAr}", color = QhColors.GoldSoft, fontSize = 14.sp)
                    Text("نقل حرفي — النسبة قيد المراجعة (الترخيص: ${r.licenseStatus})", color = QhColors.Muted, fontSize = 12.sp)
                    r.sourceNotes.forEach { Text("• $it", color = QhColors.Muted, fontSize = 12.sp) }
                    Text(r.text, color = QhColors.Ink, fontSize = 15.sp, lineHeight = 26.sp)
                }
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            containerColor = QhColors.NavyCard,
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("تم", color = QhColors.Gold) } },
            title = { Text("إعدادات العرض", color = QhColors.Gold) },
            text = {
                PrefsControls(
                    reduce, { reduce = it; prefs.reduceMotion = it },
                    scale, { scale = it; prefs.textScale = it },
                    pace, { pace = it; prefs.paceSeconds = it },
                )
            },
        )
    }
}
