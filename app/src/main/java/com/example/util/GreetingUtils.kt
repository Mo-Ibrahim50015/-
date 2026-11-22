package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.AppSettingsEntity
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Morning is strictly 00:00 (12:00 AM midnight) through 11:59 AM (hour in 0..11).
 * Evening is strictly 12:00 (12:00 PM noon) through 23:59 (hour in 12..23).
 */
fun isMorningHour(hourOfDay: Int): Boolean = hourOfDay in 0..11

/**
 * Live ticking current hour of day (0..23) so the greeting never freezes/hangs across
 * midnight (12:00 AM) or noon (12:00 PM) while the app stays open.
 */
@Composable
fun rememberCurrentHourOfDay(): Int {
    var currentHour by remember {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    }
    LaunchedEffect(Unit) {
        while (true) {
            val freshHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (freshHour != currentHour) {
                currentHour = freshHour
            }
            delay(60_000L)
        }
    }
    return currentHour
}

/**
 * Structured representation of the user's active greeting lines, supporting independent
 * visibility toggles for the Base greeting (morning/evening) and Additional custom greeting.
 */
data class UserGreetingDisplay(
    val baseLine: String?,
    val additionalLine: String?
) {
    val hasAnyGreeting: Boolean
        get() = !baseLine.isNullOrBlank() || !additionalLine.isNullOrBlank()

    val primaryLine: String
        get() = baseLine ?: additionalLine.orEmpty()

    val secondaryLine: String?
        get() = if (!baseLine.isNullOrBlank() && !additionalLine.isNullOrBlank()) additionalLine else null

    val combinedText: String
        get() = listOfNotNull(
            baseLine?.takeIf { it.isNotBlank() },
            additionalLine?.takeIf { it.isNotBlank() }
        ).joinToString(" • ")
}

private fun applyNamePlaceholder(template: String, cleanName: String): String {
    if (!template.contains("{name}")) return template
    return template
        .replace("{name}", cleanName)
        .replace(Regex("\\s{2,}"), " ")
        .trim()
}

/**
 * Builds the structured live greeting respecting independent toggles for:
 * - [showBaseGreetingOverride] / [AppSettingsEntity.showBaseGreeting]: morning/evening greeting
 * - [showCustomGreetingOverride] / [AppSettingsEntity.showCustomGreeting]: additional custom greeting
 *
 * When both are enabled and an additional custom greeting is entered, BOTH are displayed together
 * (base greeting on top + additional greeting alongside/below it).
 * Either one can be independently toggled ON or OFF.
 */
fun buildUserGreetingDisplay(
    hourOfDay: Int,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    nameOverride: String? = null,
    customGreetingOverride: String? = null,
    morningGreetingOverride: String? = null,
    eveningGreetingOverride: String? = null,
    showBaseGreetingOverride: Boolean? = null,
    showCustomGreetingOverride: Boolean? = null
): UserGreetingDisplay {
    val cleanName = (nameOverride ?: settings.userName).trim()
    val customFull = (customGreetingOverride ?: settings.customGreetingText).trim()
    val customMorning = (morningGreetingOverride ?: settings.customMorningGreeting).trim()
    val customEvening = (eveningGreetingOverride ?: settings.customEveningGreeting).trim()
    val showBase = showBaseGreetingOverride ?: settings.showBaseGreeting
    val showCustom = showCustomGreetingOverride ?: settings.showCustomGreeting

    val baseLine: String? = if (showBase) {
        val morningBase = customMorning.ifBlank { tr(isArabic, "صباح الخير", "Good Morning") }
        val eveningBase = customEvening.ifBlank { tr(isArabic, "مساء الخير", "Good Evening") }
        val base = if (isMorningHour(hourOfDay)) morningBase else eveningBase
        when {
            base.contains("{name}") -> applyNamePlaceholder(base, cleanName)
            cleanName.isNotEmpty() -> if (isArabic) "$base يا $cleanName" else "$base, $cleanName"
            else -> tr(isArabic, "$base في مصروفي", "$base • Masrofy")
        }
    } else {
        null
    }

    val additionalLine: String? = if (showCustom && customFull.isNotEmpty()) {
        applyNamePlaceholder(customFull, cleanName).takeIf { it.isNotBlank() }
    } else {
        null
    }

    return UserGreetingDisplay(
        baseLine = baseLine,
        additionalLine = additionalLine
    )
}

/**
 * Convenience single-string greeting combining active greeting parts based on current settings.
 */
fun buildUserGreeting(
    hourOfDay: Int,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    nameOverride: String? = null,
    customGreetingOverride: String? = null,
    morningGreetingOverride: String? = null,
    eveningGreetingOverride: String? = null,
    showBaseGreetingOverride: Boolean? = null,
    showCustomGreetingOverride: Boolean? = null
): String = buildUserGreetingDisplay(
    hourOfDay = hourOfDay,
    settings = settings,
    isArabic = isArabic,
    nameOverride = nameOverride,
    customGreetingOverride = customGreetingOverride,
    morningGreetingOverride = morningGreetingOverride,
    eveningGreetingOverride = eveningGreetingOverride,
    showBaseGreetingOverride = showBaseGreetingOverride,
    showCustomGreetingOverride = showCustomGreetingOverride
).combinedText
