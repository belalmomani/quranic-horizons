package app.quranichorizons.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.tasks.await

/** تسجيل دخول عادي بالبريد وكلمة المرور (Firebase Auth). الأدمن يُعتمد فقط إذا كان بريده موثَّقًا. */
object AuthManager {
    private fun auth() = FirebaseAuth.getInstance()

    fun currentEmail(): String = try { auth().currentUser?.email ?: "" } catch (_: Exception) { "" }
    fun isVerified(): Boolean = try { auth().currentUser?.isEmailVerified == true } catch (_: Exception) { false }

    /** يحدّث حالة التوثيق من الخادم (بعد ضغط رابط التوثيق في البريد). */
    suspend fun refresh() { try { auth().currentUser?.reload()?.await() } catch (_: Exception) { } }

    suspend fun signIn(email: String, password: String) { auth().signInWithEmailAndPassword(email.trim(), password).await() }

    /** ينشئ حسابًا جديدًا ويرسل رسالة توثيق إلى البريد. */
    suspend fun register(email: String, password: String) {
        val u = auth().createUserWithEmailAndPassword(email.trim(), password).await().user
        u?.sendEmailVerification()?.await()
    }

    suspend fun sendVerification() { auth().currentUser?.sendEmailVerification()?.await() }
    suspend fun resetPassword(email: String) { auth().sendPasswordResetEmail(email.trim()).await() }
    fun signOut() { try { auth().signOut() } catch (_: Exception) { } }

    /** رسالة عربية مفهومة لأخطاء Firebase الشائعة. */
    fun explain(e: Exception): String {
        val code = (e as? FirebaseAuthException)?.errorCode ?: ""
        return when (code) {
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS" -> "البريد أو كلمة المرور غير صحيحة"
            "ERROR_USER_NOT_FOUND" -> "لا يوجد حساب بهذا البريد؛ أنشئ حسابًا جديدًا"
            "ERROR_EMAIL_ALREADY_IN_USE" -> "هذا البريد مسجَّل من قبل؛ سجّل الدخول"
            "ERROR_WEAK_PASSWORD" -> "كلمة المرور ضعيفة (6 أحرف على الأقل)"
            "ERROR_INVALID_EMAIL" -> "صيغة البريد غير صحيحة"
            "ERROR_TOO_MANY_REQUESTS" -> "محاولات كثيرة؛ انتظر قليلًا"
            "ERROR_NETWORK_REQUEST_FAILED" -> "لا يوجد اتصال بالإنترنت"
            else -> (e.message ?: e.javaClass.simpleName)
        }
    }
}
