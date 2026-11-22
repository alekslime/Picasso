package app.picasso.tracer.monetization

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Signed-in Google profile stored on-device (login is local-only, no backend). */
data class GoogleAccount(val email: String, val name: String)

sealed interface SignInResult {
    data class SignedIn(val account: GoogleAccount) : SignInResult
    data object Cancelled : SignInResult
    data object Failed : SignInResult
    data object NotConfigured : SignInResult
}

/**
 * Google sign-in through Credential Manager. No Firebase, no backend: the
 * ID token is verified implicitly by Google's UI flow and only the profile
 * (email + name) is kept in Settings. Everything account-related lives in
 * DataStore, so deleting the account is deleting two keys.
 */
@Singleton
class GoogleAuth @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(activity: Activity): SignInResult {
        val serverClientId = context.getString(app.picasso.tracer.R.string.google_server_client_id)
        if (serverClientId.isBlank() || serverClientId.startsWith("REPLACE_")) {
            return SignInResult.NotConfigured
        }
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(true)
                .build()
            val response = credentialManager.getCredential(
                activity,
                GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build(),
            )
            val credential = GoogleIdTokenCredential.createFrom(response.credential.data)
            SignInResult.SignedIn(
                GoogleAccount(
                    email = credential.id,
                    name = credential.displayName?.takeIf { it.isNotBlank() } ?: credential.id,
                ),
            )
        } catch (e: GetCredentialCancellationException) {
            SignInResult.Cancelled
        } catch (_: Exception) {
            SignInResult.Failed
        }
    }

    /** Best-effort credential-state clear; local profile wipe happens regardless. */
    suspend fun signOut() {
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }
}
