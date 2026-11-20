package com.example.util

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Observes Firebase Auth state safely (returns null if Firebase isn't initialized in unit tests).
 */
fun observeFirebaseUserFlow(): Flow<FirebaseUser?> = callbackFlow {
    val auth = runCatching { Firebase.auth }.getOrNull()
    if (auth == null) {
        trySend(null)
        close()
        return@callbackFlow
    }
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    auth.addAuthStateListener(listener)
    trySend(auth.currentUser)
    awaitClose { auth.removeAuthStateListener(listener) }
}

/**
 * Opens the interactive Google account/email chooser via Jetpack Credential Manager
 * (`GetSignInWithGoogleOption`) and authenticates with Firebase Auth.
 */
fun signInWithGoogleAccountChooser(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    isArabic: Boolean,
    onSuccess: (FirebaseUser) -> Unit,
    onCancelled: () -> Unit,
    onError: (String) -> Unit
) {
    scope.launch {
        try {
            val webClientId = runCatching {
                context.getString(R.string.default_web_client_id)
            }.getOrDefault("")
            if (webClientId.isBlank()) {
                onError(
                    tr(
                        isArabic,
                        "تعذر قراءة إعدادات تسجيل الدخول بحساب جوجل",
                        "Missing Google Sign-In configuration"
                    )
                )
                return@launch
            }

            // Use GetSignInWithGoogleOption ONLY (1 option per request) for explicit email picker
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val result = credentialManager.getCredential(
                context = context,
                request = request
            )
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = Firebase.auth.signInWithCredential(firebaseCred).await()
                val signedInUser = authResult.user
                if (signedInUser != null) {
                    onSuccess(signedInUser)
                } else {
                    onError(
                        tr(
                            isArabic,
                            "لم يتم استلام بيانات الحساب بعد تسجيل الدخول",
                            "Failed to obtain user profile after sign-in"
                        )
                    )
                }
            } else {
                onError(
                    tr(
                        isArabic,
                        "نوع بيانات الاعتماد غير مدعوم لتسجيل الدخول",
                        "Unsupported credential type received"
                    )
                )
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Sign-in cancelled by user", e)
            onCancelled()
        } catch (e: NoCredentialException) {
            Log.w("Auth", "No Google account available on device", e)
            onError(
                tr(
                    isArabic,
                    "لا يوجد حساب جوجل مسجل على الهاتف. أضف حساب جوجل من إعدادات الجهاز أولاً.",
                    "No Google account found on this device. Please add one in device settings."
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Auth", "Google sign-in failed", e)
            onError(
                tr(
                    isArabic,
                    "تعذر إتمام تسجيل الدخول بحساب جوجل. تأكد من الاتصال بالإنترنت وحاول مرة أخرى.",
                    "Google Sign-In failed. Check your internet connection and try again."
                )
            )
        }
    }
}

/**
 * Signs out cleanly inside NonCancellable so credential state and Firebase Auth tear down together.
 */
fun signOutGoogleAccount(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onComplete: () -> Unit
) {
    scope.launch {
        withContext(NonCancellable) {
            runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
            runCatching { Firebase.auth.signOut() }
        }
        onComplete()
    }
}
