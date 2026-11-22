package com.example.util

import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.AppSettingsEntity

/**
 * Sentinel value persisted in `AppSettingsEntity.patternCode` when the user chooses
 * to rely on the phone's built-in Device Default Pattern / Screen Lock instead of
 * drawing a separate in-app custom 3x3 pattern.
 */
const val PATTERN_MODE_DEVICE_DEFAULT = "__DEVICE_DEFAULT__"

fun AppSettingsEntity.isDeviceDefaultPatternMode(): Boolean =
    patternEnabled && patternCode == PATTERN_MODE_DEVICE_DEFAULT

fun AppSettingsEntity.isCustomAppPatternMode(): Boolean =
    patternEnabled && patternCode.isNotBlank() && patternCode != PATTERN_MODE_DEVICE_DEFAULT

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
 * authentication (compatible from Android 7 API 24 through Android 16 API 36).
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
            bioResult == BiometricManager.BIOMETRIC_SUCCESS || isDeviceScreenLockSecure(context)
        }
    }.getOrDefault(false)
}

/**
 * Checks whether the phone has a system screen lock (Pattern / PIN / Password) configured.
 */
fun isDeviceScreenLockSecure(context: Context): Boolean {
    return runCatching {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        km?.isDeviceSecure == true
    }.getOrDefault(false)
}

/**
 * Launches the real Android system BiometricPrompt supporting Fingerprint, Face,
 * and Device Pattern/PIN/Password seamlessly without freezing or overlapping callbacks.
 */
fun launchDeviceBiometricOrCredentialPrompt(
    context: Context,
    title: String,
    subtitle: String,
    isArabic: Boolean,
    deviceCredentialOnly: Boolean = false,
    onSuccess: () -> Unit,
    onCancelled: () -> Unit = {},
    onFailure: (String) -> Unit
) {
    val activity = context.findFragmentActivity()
    if (activity == null || activity.isFinishing || activity.isDestroyed) {
        onFailure(tr(isArabic, "تعذر تشغيل نافذة التحقق على هذا الجهاز", "Unable to start verification"))
        return
    }
    runCatching {
        var settled = false
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                if (!settled) {
                    settled = true
                    onSuccess()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (settled) return
                settled = true
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    onCancelled()
                } else {
                    onFailure(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // System prompt stays open for immediate retry
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val authenticators = if (deviceCredentialOnly) {
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            } else {
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            }
            builder.setAllowedAuthenticators(authenticators)
        } else {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }

        prompt.authenticate(builder.build())
    }.onFailure {
        onFailure(
            tr(
                isArabic,
                "تأكد من تفعيل البصمة أو نمط/قفل الشاشة في إعدادات الهاتف أولاً",
                "Please enable fingerprint or screen lock in device settings first"
            )
        )
    }
}
