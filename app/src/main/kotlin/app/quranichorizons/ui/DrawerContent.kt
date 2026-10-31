package app.quranichorizons.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quranichorizons.data.Prefs
import app.quranichorizons.i18n.Lang
import app.quranichorizons.i18n.tr

/** بريد المدير الذي تظهر له «لوحة الإدارة». للعرض فقط: ليس حماية أمنية قبل ربط تسجيل دخول حقيقي. */
const val ADMIN_EMAIL = "bilalalmomani90@gmail.com"

fun isAdmin(email: String): Boolean = email.trim().equals(ADMIN_EMAIL, ignoreCase = true)

@Composable
private fun DrawerItem(text: String, onClick: () -> Unit) {
    Text(text, color = QhColors.Ink, fontSize = 17.sp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp))
}

/** محتوى القائمة الجانبية: الرئيسية، الإعدادات، التحديث المباشر، اللغة، تقليل الحركة، متجري، لوحة الإدارة (للمدير). */
@Composable
fun DrawerContent(
    prefs: Prefs,
    langOverride: String, onLang: (String) -> Unit,
    onHome: () -> Unit, onSettings: () -> Unit, onUpdate: () -> Unit, onStore: () -> Unit, onAdmin: () -> Unit,
    email: String, verified: Boolean, authMsg: String, onSignIn: () -> Unit, onSignOut: () -> Unit, onVerify: () -> Unit,
) {
    var reduce by remember { mutableStateOf(prefs.reduceMotion) }
    var langOpen by remember { mutableStateOf(false) }
    val admin = verified && isAdmin(email)
    Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("الآفاق القرآنية", color = QhColors.Gold, fontSize = 22.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
        DrawerItem(tr("menuHome"), onHome)
        DrawerItem(tr("settingsTitle"), onSettings)
        DrawerItem(tr("menuUpdate"), onUpdate)
        DrawerItem(tr("language") + (if (langOpen) "  ▴" else "  ▾"), { langOpen = !langOpen })
        if (langOpen) {
            DrawerItem((if (langOverride.isEmpty()) "● " else "○ ") + tr("langAuto"), { onLang("") })
            Lang.values().forEach { l ->
                DrawerItem((if (langOverride == l.code) "● " else "○ ") + l.flag + "  " + l.native, { onLang(l.code) })
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("reduceMotion"), color = QhColors.Ink, fontSize = 17.sp, modifier = Modifier.weight(1f))
            Switch(checked = reduce, onCheckedChange = { reduce = it; prefs.reduceMotion = it })
        }
        DrawerItem(tr("menuStore"), onStore)
        if (admin) DrawerItem(tr("menuAdmin"), onAdmin)
        if (email.isEmpty()) DrawerItem(tr("menuSignIn"), onSignIn)
        else {
            Text(email, color = QhColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            if (!verified) {
                Text("البريد غير موثَّق: افتح الرابط المرسل إلى بريدك ثم اضغط «تحقق».", color = QhColors.GoldSoft, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                DrawerItem("تحقق من التوثيق / إعادة إرسال الرابط", onVerify)
            }
            DrawerItem(tr("menuSignOut"), onSignOut)
        }
        if (authMsg.isNotEmpty()) Text(authMsg, color = QhColors.GoldSoft, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
    }
}
