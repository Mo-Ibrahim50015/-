package com.example.util

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Finds the hosting FragmentActivity from any Compose LocalContext wrapper chain.
 */
tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

/**
 * Checks whether the device supports Fingerprint / Face / Device Pattern / Device PIN
 * authentication (compatible from Android 8 API 26 through Android 16 API 36).
 */
fun canUseDeviceBiometricOrCredential(context: Context): Boolean {
    return runCatching {
        val manager = BiometricManager.from(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            val bioResult = manager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            bioResult == BiometricManager.BIOMETRIC_SUCCESS
        }
    }.getOrDefault(false)
}

/**
 * Launches the real Android system BiometricPrompt supporting Fingerprint, Face,
 * and Device Pattern/PIN/Password seamlessly from Android 8 (API 26) onwards.
 */
fun launchDeviceBiometricOrCredentialPrompt(
    context: Context,
    title: String,
    subtitle: String,
    isArabic: Boolean,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    val activity = context.findFragmentActivity()
    if (activity == null || activity.isFinishing || activity.isDestroyed) {
        onFailure(tr(isArabic, "تعذر تشغيل نافذة التحقق على هذا الجهاز", "Unable to start verification"))
        return
    }
    runCatching {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_CANCELED
                ) {
                    onFailure(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // System prompt remains open for retry
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }

        prompt.authenticate(builder.build())
    }.onFailure {
        onFailure(
            tr(
                isArabic,
                "تأكد من تفعيل البصمة أو قفل الشاشة في إعدادات الهاتف أولاً",
                "Please enable fingerprint or screen lock in device settings first"
            )
        )
    }
}
