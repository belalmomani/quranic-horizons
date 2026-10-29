package app.quranichorizons.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import app.quranichorizons.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/** تسجيل الدخول بـGoogle عبر Firebase Auth. البريد يأتي من حساب Firebase الموثَّق لا من نص يكتبه المستخدم. */
object AuthManager {
    /** البريد الحالي (فارغ إن لم يكن هناك دخول أو لم يُهيَّأ Firebase). */
    fun currentEmail(): String = try { FirebaseAuth.getInstance().currentUser?.email ?: "" } catch (_: Exception) { "" }

    /** يُرجع البريد عند النجاح؛ يرمي استثناءً برسالة عند الفشل. */
    suspend fun signIn(activityContext: Context): String {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(activityContext.getString(R.string.default_web_client_id))
            .build()
        val result = CredentialManager.create(activityContext)
            .getCredential(activityContext, GetCredentialRequest.Builder().addCredentialOption(option).build())
        val google = GoogleIdTokenCredential.createFrom(result.credential.data)
        val user = FirebaseAuth.getInstance()
            .signInWithCredential(GoogleAuthProvider.getCredential(google.idToken, null)).await().user
        return user?.email ?: ""
    }

    fun signOut() { try { FirebaseAuth.getInstance().signOut() } catch (_: Exception) { } }
}
