package app.quranichorizons.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quranichorizons.auth.AuthManager
import kotlinx.coroutines.launch

/** نافذة تسجيل الدخول/إنشاء حساب بالبريد وكلمة المرور. onDone تُستدعى بعد نجاح الدخول أو الإنشاء. */
@Composable
fun AuthDialog(onDismiss: () -> Unit, onDone: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = QhColors.Ink, unfocusedTextColor = QhColors.Ink,
        focusedBorderColor = QhColors.Gold, unfocusedBorderColor = QhColors.Muted, cursorColor = QhColors.Gold,
    )
    fun run(block: suspend () -> Unit) {
        if (email.isBlank()) { msg = "اكتب البريد"; return }
        busy = true; msg = "…"
        scope.launch {
            try { block(); msg = ""; busy = false; onDone() }
            catch (e: Exception) { msg = AuthManager.explain(e); busy = false }
        }
    }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = QhColors.NavyCard,
        title = { Text("تسجيل الدخول", color = QhColors.Gold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = email, onValueChange = { email = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    label = { Text("البريد الإلكتروني", color = QhColors.Muted) }, colors = colors)
                OutlinedTextField(value = pass, onValueChange = { pass = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    label = { Text("كلمة المرور", color = QhColors.Muted) }, colors = colors)
                if (msg.isNotEmpty()) Text(msg, color = QhColors.GoldSoft, fontSize = 13.sp)
                TextButton(enabled = !busy, onClick = {
                    if (email.isBlank()) { msg = "اكتب البريد أولًا" } else {
                        busy = true
                        scope.launch {
                            msg = try { AuthManager.resetPassword(email); "أُرسل رابط إعادة تعيين كلمة المرور إلى بريدك" } catch (e: Exception) { AuthManager.explain(e) }
                            busy = false
                        }
                    }
                }) { Text("نسيت كلمة المرور؟", color = QhColors.GoldSoft, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = { run { AuthManager.signIn(email, pass) } }) { Text("دخول", color = QhColors.Gold) }
        },
        dismissButton = {
            Column {
                TextButton(enabled = !busy, onClick = { run { AuthManager.register(email, pass) } }) { Text("إنشاء حساب جديد", color = QhColors.GoldSoft) }
                TextButton(enabled = !busy, onClick = onDismiss) { Text("إلغاء", color = QhColors.Muted) }
            }
        },
    )
}
