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
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
private val FormatWithDecimals = ThreadLocal.withInitial {
    DecimalFormat("#,##0.00", DecimalSymbolsUs).apply {
        roundingMode = RoundingMode.HALF_UP
    }
}
private val FormatNoDecimals = ThreadLocal.withInitial {
    DecimalFormat("#,##0", DecimalSymbolsUs).apply {
        roundingMode = RoundingMode.HALF_UP
    }
}
private val IsoDateFormat = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
private val Time24Format = ThreadLocal.withInitial { SimpleDateFormat("HH:mm", Locale.US) }
private val IsoDateTimeFormat = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

// Safe maximum minor-unit value (~9.99 trillion major units) preventing Long addition overflow in commercial ledgers
const val MAX_SAFE_AMOUNT_MINOR: Long = 999_999_999_999_999L

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
 * Exact BigDecimal conversion from minor units (piasters/cents) to plain major decimal string
 * (e.g., 1999L -> "19.99", -50L -> "-0.50"). Zero floating-point drift.
 */
fun minorToPlainDecimal(amountMinor: Long): String =
    BigDecimal.valueOf(amountMinor, 2).toPlainString()

/**
 * Formats minor units (piasters/cents) into tabular lining number string like "7,530.00" or "-0.50".
 * Uses exact BigDecimal scaling so large commercial totals never suffer IEEE-754 double rounding errors.
 */
fun formatAmountMinor(amountMinor: Long, includeDecimals: Boolean = true): String {
    val decimal = BigDecimal.valueOf(amountMinor, 2)
    val fmt = if (includeDecimals) FormatWithDecimals.get()!! else FormatNoDecimals.get()!!
    val raw = fmt.format(decimal)
    return if (raw == "-0.00" || raw == "-0") raw.removePrefix("-") else raw
}

fun formatSignedAmount(amountMinor: Long, currencyCode: String, isArabic: Boolean): String {
    val sym = currencySymbol(currencyCode, isArabic)
    val absMinor = if (amountMinor == Long.MIN_VALUE) Long.MAX_VALUE else abs(amountMinor)
    val absFormatted = formatAmountMinor(absMinor)
    return when {
        amountMinor > 0L -> "+ $absFormatted $sym"
        amountMinor < 0L -> "- $absFormatted $sym"
        else -> "0.00 $sym"
    }
}

fun formatCompactAmount(amountMinor: Long): String {
    val absMinor = if (amountMinor == Long.MIN_VALUE) Long.MAX_VALUE else abs(amountMinor)
    val major = BigDecimal.valueOf(absMinor, 2)
    val prefix = if (amountMinor < 0L) "-" else ""
    return when {
        major >= BigDecimal("1000000000") -> {
            val v = major.divide(BigDecimal("1000000000"), 1, RoundingMode.HALF_UP)
            "$prefix${v.toPlainString()}B"
        }
        major >= BigDecimal("1000000") -> {
            val v = major.divide(BigDecimal("1000000"), 1, RoundingMode.HALF_UP)
            "$prefix${v.toPlainString()}M"
        }
        major >= BigDecimal("1000") -> {
            val v = major.divide(BigDecimal("1000"), 1, RoundingMode.HALF_UP)
            "$prefix${v.toPlainString()}k"
        }
        else -> "$prefix${major.setScale(0, RoundingMode.HALF_UP).toPlainString()}"
    }
}

/**
 * Exact half-up integer division for averages (e.g. average transaction or daily burn rate).
 */
fun divideMinorHalfUp(totalMinor: Long, count: Int): Long {
    if (count <= 0) return 0L
    return BigDecimal.valueOf(totalMinor)
        .divide(BigDecimal.valueOf(count.toLong()), 0, RoundingMode.HALF_UP)
        .longValueExact()
}

/**
 * Exact half-up percentage calculation returning integer percent clamped to [minVal, maxVal].
 */
fun percentHalfUp(numerator: Long, denominator: Long, minVal: Int = -999, maxVal: Int = 999): Int {
    if (denominator == 0L) return 0
    return BigDecimal.valueOf(numerator)
        .multiply(BigDecimal.valueOf(100L))
        .divide(BigDecimal.valueOf(denominator), 0, RoundingMode.HALF_UP)
        .toInt()
        .coerceIn(minVal, maxVal)
}

/**
 * Exact half-up percentage calculation returning Float with 2-decimal precision for charts.
 */
fun percentFloatExact(numerator: Long, denominator: Long): Float {
    if (denominator <= 0L || numerator <= 0L) return 0f
    return BigDecimal.valueOf(numerator)
        .multiply(BigDecimal.valueOf(100L))
        .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP)
        .toFloat()
        .coerceIn(0f, 100f)
}

/**
 * Exact BigDecimal parser converting user input (Western, Arabic-Indic ٠-٩, or Persian ۰-۹ digits,
 * with thousand separators and decimal separators) into exact minor units (Long) with HALF_UP rounding.
 * Rejects malformed strings, non-finite inputs, and overflow values.
 */
fun parseAmountToMinor(raw: String): Long? {
    var s = raw.trim()
        // Arabic-Indic digits
        .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3').replace('٤', '4')
        .replace('٥', '5').replace('٦', '6').replace('٧', '7').replace('٨', '8').replace('٩', '9')
        // Eastern Arabic / Persian digits
        .replace('۰', '0').replace('۱', '1').replace('۲', '2').replace('۳', '3').replace('۴', '4')
        .replace('۵', '5').replace('۶', '6').replace('۷', '7').replace('۸', '8').replace('۹', '9')
        // Arabic decimal & thousands separators
        .replace('٫', '.')
        .replace('٬', ',')
        .replace(" ", "")
        .replace("_", "")
        .replace("\u00A0", "")

    if (s.isEmpty()) return null

    // Smart handling of commas vs dots:
    // - If both '.' and ',' exist: ',' is thousands separator (e.g., "1,250.50") unless ',' appears after '.'
    // - If only ',' exists:
    //   - Multiple commas -> thousands separators (e.g., "1,000,000")
    //   - Single comma with exactly 3 trailing digits and >=1 leading digit -> thousands separator ("1,000")
    //   - Single comma with 1 or 2 trailing digits -> decimal separator ("125,50")
    if (',' in s && '.' in s) {
        val lastDot = s.lastIndexOf('.')
        val lastComma = s.lastIndexOf(',')
        s = if (lastComma > lastDot) {
            // European format: "1.250,50"
            s.replace(".", "").replace(',', '.')
        } else {
            // Standard format: "1,250.50"
            s.replace(",", "")
        }
    } else if (',' in s) {
        val commaCount = s.count { it == ',' }
        if (commaCount > 1) {
            s = s.replace(",", "")
        } else {
            val idx = s.indexOf(',')
            val before = s.substring(0, idx)
            val after = s.substring(idx + 1)
            val beforeDigits = before.trimStart('+', '-')
            s = if (after.length == 3 && beforeDigits.isNotEmpty() && beforeDigits != "0") {
                before + after
            } else {
                "$before.$after"
            }
        }
    }

    // Strict syntactic check: optional sign, digits, optional single decimal point + digits
    if (!Regex("^[+-]?(\\d+(\\.\\d*)?|\\.\\d+)$").matches(s)) return null

    return try {
        val bd = BigDecimal(s)
        val minorBd = bd.movePointRight(2).setScale(0, RoundingMode.HALF_UP)
        val result = minorBd.longValueExact()
        if (abs(result) > MAX_SAFE_AMOUNT_MINOR) null else result
    } catch (_: Exception) {
        null
    }
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

fun formatTime12Hour(time24: String, isArabic: Boolean): String =
    formatTime12Fast(time24, isArabic)

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
