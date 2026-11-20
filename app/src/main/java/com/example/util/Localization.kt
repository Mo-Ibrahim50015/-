package com.example.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LaptopMac
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

data class CurrencyInfo(
    val code: String,
    val symbolAr: String,
    val symbolEn: String,
    val nameAr: String,
    val nameEn: String
)

val SupportedCurrencies = listOf(
    CurrencyInfo("EGP", "ج.م", "EGP", "الجنيه المصري", "Egyptian Pound"),
    CurrencyInfo("USD", "$", "USD", "الدولار الأمريكي", "US Dollar"),
    CurrencyInfo("EUR", "€", "EUR", "اليورو الأوروبي", "Euro"),
    CurrencyInfo("SAR", "ر.س", "SAR", "الريال السعودي", "Saudi Riyal"),
    CurrencyInfo("AED", "د.إ", "AED", "الدرهم الإماراتي", "UAE Dirham")
)

fun currencyInfoFor(code: String): CurrencyInfo =
    SupportedCurrencies.firstOrNull { it.code.equals(code, ignoreCase = true) }
        ?: SupportedCurrencies.first()

fun currencySymbol(code: String, isArabic: Boolean): String {
    val info = currencyInfoFor(code)
    return if (isArabic) info.symbolAr else info.symbolEn
}

private val DecimalSymbolsUs = DecimalFormatSymbols(Locale.US)
private val FormatWithDecimals = ThreadLocal.withInitial { DecimalFormat("#,##0.00", DecimalSymbolsUs) }
private val FormatNoDecimals = ThreadLocal.withInitial { DecimalFormat("#,##0", DecimalSymbolsUs) }
private val IsoDateFormat = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
private val Time24Format = ThreadLocal.withInitial { SimpleDateFormat("HH:mm", Locale.US) }
private val IsoDateTimeFormat = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

private val MonthsAr = arrayOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
)
private val MonthsEn = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)
private val ArabicLocale = Locale("ar", "EG")

// Lightweight cached today/yesterday strings refreshed at most once per minute
@Volatile private var cachedDayStampMin: Long = -1L
@Volatile private var cachedTodayIso: String = ""
@Volatile private var cachedYesterdayIso: String = ""

private fun refreshDayCacheIfNeeded() {
    val nowMin = System.currentTimeMillis() / 60_000L
    if (nowMin != cachedDayStampMin) {
        val cal = Calendar.getInstance()
        val sdf = IsoDateFormat.get()!!
        val today = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yest = sdf.format(cal.time)
        cachedTodayIso = today
        cachedYesterdayIso = yest
        cachedDayStampMin = nowMin
    }
}

/**
 * Formats minor units (piasters/cents) into tabular lining number string like "7,530.00".
 * Always uses standard Western digits for crisp tabular financial alignment.
 */
fun formatAmountMinor(amountMinor: Long, includeDecimals: Boolean = true): String {
    val major = amountMinor / 100.0
    val fmt = if (includeDecimals) FormatWithDecimals.get()!! else FormatNoDecimals.get()!!
    return fmt.format(major)
}

fun formatSignedAmount(amountMinor: Long, currencyCode: String, isArabic: Boolean): String {
    val sym = currencySymbol(currencyCode, isArabic)
    val absFormatted = formatAmountMinor(abs(amountMinor))
    return when {
        amountMinor > 0 -> "+ $absFormatted $sym"
        amountMinor < 0 -> "- $absFormatted $sym"
        else -> "0.00 $sym"
    }
}

fun formatCompactAmount(amountMinor: Long): String {
    val major = abs(amountMinor) / 100.0
    return when {
        major >= 1_000_000 -> String.format(Locale.US, "%.1fM", major / 1_000_000.0)
        major >= 1_000 -> String.format(Locale.US, "%.1fk", major / 1_000.0)
        else -> String.format(Locale.US, "%.0f", major)
    }
}

/**
 * Converts user input string (supporting Arabic-Indic digits ٠-٩) into minor units (Long).
 */
fun parseAmountToMinor(raw: String): Long? {
    val normalized = raw.trim()
        .replace('٠', '0')
        .replace('١', '1')
        .replace('٢', '2')
        .replace('٣', '3')
        .replace('٤', '4')
        .replace('٥', '5')
        .replace('٦', '6')
        .replace('٧', '7')
        .replace('٨', '8')
        .replace('٩', '9')
        .replace('٫', '.')
        .replace(',', '.')
        .replace(" ", "")
    if (normalized.isEmpty()) return null
    val parsed = normalized.toDoubleOrNull() ?: return null
    if (parsed.isNaN() || parsed.isInfinite()) return null
    return (parsed * 100.0).roundToLong()
}

fun currentIsoDate(): String {
    refreshDayCacheIfNeeded()
    return cachedTodayIso
}

fun currentTime24(): String = Time24Format.get()!!.format(Date())

fun isoDateMonthsAgo(months: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, -months)
    return IsoDateFormat.get()!!.format(cal.time)
}

fun currentMonthRange(): Pair<String, String> {
    val cal = Calendar.getInstance()
    val sdf = IsoDateFormat.get()!!
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val start = sdf.format(cal.time)
    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
    val end = sdf.format(cal.time)
    return start to end
}

fun currentWeekRange(): Pair<String, String> {
    val cal = Calendar.getInstance()
    val sdf = IsoDateFormat.get()!!
    val end = sdf.format(cal.time)
    cal.add(Calendar.DAY_OF_YEAR, -6)
    val start = sdf.format(cal.time)
    return start to end
}

private fun formatTime12Fast(time24: String, isArabic: Boolean): String {
    if (time24.length < 5) return time24
    val hour = time24.substring(0, 2).toIntOrNull() ?: return time24
    val min = time24.substring(3, 5).toIntOrNull() ?: return time24
    val isPm = hour >= 12
    val h12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val suffix = if (isArabic) (if (isPm) "م" else "ص") else (if (isPm) "PM" else "AM")
    val hStr = if (h12 < 10) "0$h12" else "$h12"
    val mStr = if (min < 10) "0$min" else "$min"
    return "$hStr:$mStr $suffix"
}

/**
 * Zero-allocation fast display date-time formatter used inside scrolling transaction lists.
 */
fun formatDisplayDateTime(isoDate: String, time24: String, isArabic: Boolean): String {
    if (isoDate.length < 10) return "$isoDate $time24"
    refreshDayCacheIfNeeded()
    val formattedTime = formatTime12Fast(time24, isArabic)
    return when (isoDate) {
        cachedTodayIso -> if (isArabic) "اليوم، $formattedTime" else "Today, $formattedTime"
        cachedYesterdayIso -> if (isArabic) "أمس، $formattedTime" else "Yesterday, $formattedTime"
        else -> {
            val monthIdx = (isoDate.substring(5, 7).toIntOrNull() ?: 1).coerceIn(1, 12) - 1
            val dayNum = isoDate.substring(8, 10).toIntOrNull() ?: 1
            if (isArabic) {
                "$dayNum ${MonthsAr[monthIdx]}، $formattedTime"
            } else {
                "${MonthsEn[monthIdx]} $dayNum, $formattedTime"
            }
        }
    }
}

fun formatPickerDateTime(isoDate: String, time24: String, isArabic: Boolean): String {
    return try {
        val parsed = IsoDateTimeFormat.get()!!.parse("$isoDate $time24")
            ?: return "$isoDate $time24"
        val pattern = if (isArabic) "EEE، d MMM، hh:mm a" else "EEE, MMM d, hh:mm a"
        SimpleDateFormat(pattern, if (isArabic) ArabicLocale else Locale.US).format(parsed)
    } catch (_: Exception) {
        "$isoDate $time24"
    }
}

fun iconForKey(key: String): ImageVector = when (key) {
    "restaurant" -> Icons.Outlined.Restaurant
    "shopping_bag" -> Icons.Outlined.ShoppingBag
    "directions_car" -> Icons.Outlined.DirectionsCar
    "receipt" -> Icons.Outlined.Receipt
    "medication" -> Icons.Outlined.Medication
    "home" -> Icons.Outlined.Home
    "apartment" -> Icons.Outlined.Apartment
    "movie" -> Icons.Outlined.Movie
    "school" -> Icons.Outlined.School
    "payments" -> Icons.Outlined.Payments
    "laptop_mac" -> Icons.Outlined.LaptopMac
    "trending_up" -> Icons.Outlined.TrendingUp
    "card_giftcard" -> Icons.Outlined.CardGiftcard
    "storefront" -> Icons.Outlined.Storefront
    "attach_money" -> Icons.Outlined.AttachMoney
    "work" -> Icons.Outlined.Work
    "wallet" -> Icons.Outlined.AccountBalanceWallet
    "account_balance" -> Icons.Outlined.AccountBalance
    "phone_android" -> Icons.Outlined.PhoneAndroid
    "flash_on" -> Icons.Outlined.FlashOn
    "contactless" -> Icons.Outlined.Contactless
    "credit_card" -> Icons.Outlined.CreditCard
    else -> Icons.Outlined.Category
}

val SelectableIconKeys = listOf(
    "restaurant",
    "shopping_bag",
    "directions_car",
    "receipt",
    "medication",
    "home",
    "movie",
    "school",
    "payments",
    "laptop_mac",
    "trending_up",
    "card_giftcard",
    "storefront",
    "attach_money",
    "wallet",
    "account_balance",
    "phone_android",
    "credit_card",
    "category"
)

/** Simple bilingual helper so toggling AR/EN updates all labels smoothly. */
fun tr(isArabic: Boolean, ar: String, en: String): String = if (isArabic) ar else en
