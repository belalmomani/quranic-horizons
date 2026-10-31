package app.quranichorizons

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.quranichorizons.data.Catalog
import app.quranichorizons.data.ContentRepository
import app.quranichorizons.data.Prefs
import app.quranichorizons.data.SurahsFile
import app.quranichorizons.i18n.Lang
import app.quranichorizons.i18n.LocalLang
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import app.quranichorizons.ui.AdSlot
import app.quranichorizons.ui.QhColors
import app.quranichorizons.ui.HomeScreen
import app.quranichorizons.ui.DrawerContent
import app.quranichorizons.ui.StoreScreen
import app.quranichorizons.ui.UpdateScreen
import app.quranichorizons.ui.isAdmin
import kotlinx.coroutines.launch
import app.quranichorizons.ui.PlayerScreen
import app.quranichorizons.ui.QhTheme
import app.quranichorizons.ui.SettingsScreen
import app.quranichorizons.ui.SurahScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = ContentRepository(applicationContext)
        val prefs = Prefs(applicationContext)
        app.quranichorizons.data.OnlineAudio.baseOverride = prefs.audioBase
        if (BuildConfig.ADS_ENABLED) {
            try {
                com.startapp.sdk.adsbase.StartAppSDK.init(this, BuildConfig.START_IO_APP_ID, false)
                com.startapp.sdk.adsbase.StartAppAd.disableSplash()
                if (BuildConfig.ADS_TEST_MODE) com.startapp.sdk.adsbase.StartAppSDK.setTestAdsEnabled(true)
            } catch (e: Throwable) { /* الإعلان لا يعطّل التطبيق أبدًا */ }
        }
        setContent { QhTheme { App(repo, prefs) } }
    }
}

private const val HOME = 0
private const val SURAH = 1
private const val PLAYER = 2
private const val SETTINGS = 3
private const val STORE = 4
private const val ADMIN = 5
private const val UPDATE = 6

@Composable
private fun App(repo: ContentRepository, prefs: Prefs) {
    val surahs: SurahsFile = remember { repo.loadSurahs() }
    val catalog: Catalog = remember { repo.loadCatalog() }
    var langOverride by remember { mutableStateOf(prefs.langOverride) }
    val lang = Lang.resolve(langOverride)
    var screen by rememberSaveable { mutableStateOf(HOME) }
    var selected by rememberSaveable { mutableStateOf(0) }
    var startAyah by rememberSaveable { mutableStateOf(1) }
    // حالة قائمة السور والبحث ترتفع إلى هنا كي لا تعود القائمة إلى أولها عند الرجوع من سورة
    val homeListState = androidx.compose.foundation.lazy.rememberLazyListState()
    var homeQuery by rememberSaveable { mutableStateOf("") }

    val drawerState = androidx.compose.material3.rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val drawerScope = androidx.compose.runtime.rememberCoroutineScope()
    var email by remember { mutableStateOf(app.quranichorizons.auth.AuthManager.currentEmail()) }
    var verified by remember { mutableStateOf(app.quranichorizons.auth.AuthManager.isVerified()) }
    var authMsg by remember { mutableStateOf("") }
    var showAuth by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { app.quranichorizons.auth.AuthManager.refresh(); email = app.quranichorizons.auth.AuthManager.currentEmail(); verified = app.quranichorizons.auth.AuthManager.isVerified() }
    if (showAuth) app.quranichorizons.ui.AuthDialog(
        onDismiss = { showAuth = false },
        onDone = { showAuth = false; email = app.quranichorizons.auth.AuthManager.currentEmail(); verified = app.quranichorizons.auth.AuthManager.isVerified(); prefs.accountEmail = email },
    )
    fun go(s: Int) { screen = s; drawerScope.launch { drawerState.close() } }

    // مغادرة شاشة التشغيل عمدًا (رجوع/قائمة السور) توقف التلاوة؛ أما إغلاق/إعادة إنشاء النشاط فلا
    val actCtx = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(screen) { if (screen != PLAYER) app.quranichorizons.player.PlaybackEngine.stop(actCtx) }

    BackHandler(enabled = drawerState.isOpen) { drawerScope.launch { drawerState.close() } }
    BackHandler(enabled = screen != HOME) {
        screen = if (screen == PLAYER) SURAH else HOME
    }

    CompositionLocalProvider(
        LocalLang provides lang,
        LocalLayoutDirection provides (if (lang == Lang.AR) LayoutDirection.Rtl else LayoutDirection.Ltr),
    ) {
    androidx.compose.material3.ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = screen != PLAYER,
        drawerContent = {
            androidx.compose.material3.ModalDrawerSheet(drawerContainerColor = QhColors.NavyDeep) {
                DrawerContent(
                    prefs = prefs, langOverride = langOverride, onLang = { langOverride = it; prefs.langOverride = it },
                    onHome = { go(HOME) }, onSettings = { go(SETTINGS) }, onUpdate = { go(UPDATE) }, onStore = { go(STORE) }, onAdmin = { go(ADMIN) },
                    email = email, verified = verified, authMsg = authMsg,
                    onSignIn = { showAuth = true; drawerScope.launch { drawerState.close() } },
                    onSignOut = { app.quranichorizons.auth.AuthManager.signOut(); email = ""; verified = false; prefs.accountEmail = ""; authMsg = "" },
                    onVerify = { authMsg = "…"; drawerScope.launch { authMsg = try { app.quranichorizons.auth.AuthManager.sendVerification(); app.quranichorizons.auth.AuthManager.refresh(); verified = app.quranichorizons.auth.AuthManager.isVerified(); if (verified) "تم التوثيق" else "أُعيد إرسال الرابط؛ افتحه ثم اضغط مرة أخرى" } catch (e: Exception) { app.quranichorizons.auth.AuthManager.explain(e) } } },
                )
            }
        },
    ) {
    // الشاشات الجانبية: المحتوى كله فوق، وشريط الإعلان المحجوز في أسفلها لا يغطي شيئًا؛ شاشة التشغيل بلا إعلان
    Column(Modifier.fillMaxSize().background(QhColors.Navy).then(if (screen == PLAYER) Modifier else Modifier.systemBarsPadding())) {
    Box(Modifier.weight(1f).fillMaxWidth()) {
    when (screen) {
        HOME -> HomeScreen(
            repo = repo, prefs = prefs, surahs = surahs, catalog = catalog,
            onOpen = { selected = it; screen = SURAH },
            onContinue = { s, a -> selected = s; startAyah = a; screen = PLAYER },
            onSettings = { screen = SETTINGS },
            onMenu = { drawerScope.launch { drawerState.open() } },
            listState = homeListState, query = homeQuery, onQuery = { homeQuery = it },
        )
        SURAH -> SurahScreen(
            repo = repo, surahs = surahs, catalog = catalog, number = selected,
            onBack = { screen = HOME },
            onStart = { startAyah = 1; screen = PLAYER },
        )
        PLAYER -> androidx.compose.runtime.key(selected) {
            PlayerScreen(
                repo = repo, prefs = prefs, surahs = surahs, catalog = catalog, number = selected, startAyah = startAyah,
                onExit = { screen = SURAH },
                onToList = { screen = HOME },
                onSurah = { n -> selected = n; startAyah = 1 },
            )
        }
        STORE -> StoreScreen(prefs = prefs, admin = false, onBack = { screen = HOME })
        ADMIN -> StoreScreen(prefs = prefs, admin = verified && isAdmin(email), onBack = { screen = HOME })
        UPDATE -> UpdateScreen(prefs = prefs, onBack = { screen = HOME })
        SETTINGS -> SettingsScreen(
            repo = repo, prefs = prefs, surahs = surahs, catalog = catalog, onBack = { screen = HOME },
            langOverride = langOverride, onLang = { langOverride = it; prefs.langOverride = it },
        )
    }
    }
    if (screen != PLAYER) AdSlot()
    }
    }
    }
}
