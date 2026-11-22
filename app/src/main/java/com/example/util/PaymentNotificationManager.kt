package com.example.util

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.AppSettingsEntity
import com.example.data.ObligationEntity
import com.example.data.ReminderAlertMode
import com.example.data.TransactionEntity
import com.example.ui.AppTab
import com.example.ui.PaymentAlarmActivity
import com.example.widget.MasrofyWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Locale

data class ReminderLeadOption(
    val minutes: Int,
    val labelAr: String,
    val labelEn: String
)

val PaymentReminderLeadOptions = listOf(
    ReminderLeadOption(0, "في نفس الموعد", "At due time"),
    ReminderLeadOption(15, "قبل ١٥ دقيقة", "15 min before"),
    ReminderLeadOption(30, "قبل ٣٠ دقيقة", "30 min before"),
    ReminderLeadOption(60, "قبل ساعة", "1 hour before"),
    ReminderLeadOption(180, "قبل ٣ ساعات", "3 hours before"),
    ReminderLeadOption(1440, "قبل يوم", "1 day before"),
    ReminderLeadOption(2880, "قبل يومين", "2 days before")
)

data class AlertModeOption(
    val mode: String,
    val labelAr: String,
    val labelEn: String,
    val descAr: String,
    val descEn: String
)

val PaymentAlertModeOptions = listOf(
    AlertModeOption(
        mode = ReminderAlertMode.NOTIFICATION,
        labelAr = "إشعار فقط",
        labelEn = "Notification Only",
        descAr = "إشعار هادئ على الشاشة بدون رنين متواصل",
        descEn = "Standard alert notification without continuous ringing"
    ),
    AlertModeOption(
        mode = ReminderAlertMode.RING,
        labelAr = "رنين فقط",
        labelEn = "Ringtone Alarm",
        descAr = "يرن الهاتف كمنبه قوي عند حلول الموعد",
        descEn = "Rings like an alarm when payment is due"
    ),
    AlertModeOption(
        mode = ReminderAlertMode.NOTIFICATION_RING,
        labelAr = "إشعار + رنين",
        labelEn = "Notification + Ring",
        descAr = "إشعار واضح مع رنين واهتزاز لتنبيهك فوراً",
        descEn = "Prominent notification with ringtone and vibration"
    ),
    AlertModeOption(
        mode = ReminderAlertMode.NOTIFICATION_VOICE,
        labelAr = "إشعار + نطق صوتي",
        labelEn = "Notification + Voice",
        descAr = "يعرض الإشعار وينطق اسم المبلغ والتفاصيل بالصوت",
        descEn = "Displays alert and speaks amount & details aloud"
    )
)

fun formatAlertModeLabel(mode: String, isArabic: Boolean): String {
    val found = PaymentAlertModeOptions.firstOrNull { it.mode == mode }
    return if (found != null) {
        if (isArabic) found.labelAr else found.labelEn
    } else {
        tr(isArabic, "إشعار + رنين", "Notification + Ring")
    }
}

fun formatReminderLeadTime(minutes: Int, isArabic: Boolean): String {
    PaymentReminderLeadOptions.firstOrNull { it.minutes == minutes }?.let {
        return if (isArabic) it.labelAr else it.labelEn
    }
    return when {
        minutes <= 0 -> tr(isArabic, "في نفس الموعد", "At due time")
        minutes < 60 -> tr(isArabic, "قبل $minutes دقيقة", "$minutes min before")
        minutes % 1440 == 0 -> {
            val days = minutes / 1440
            tr(isArabic, "قبل $days يوم", "$days day(s) before")
        }
        minutes % 60 == 0 -> {
            val hrs = minutes / 60
            tr(isArabic, "قبل $hrs ساعة", "$hrs hr(s) before")
        }
        else -> tr(isArabic, "قبل $minutes دقيقة", "$minutes min before")
    }
}

object PaymentNotificationManager {
    const val CHANNEL_ID = "masrofy_payment_reminders"
    const val CHANNEL_ID_ALARM = "masrofy_payment_alarms_v2"
    const val CHANNEL_ID_QUIET = "masrofy_payment_quiet_v2"
    const val EXTRA_TX_ID = "extra_payment_tx_id"
    const val EXTRA_OBLIGATION_ID = "extra_payment_obligation_id"
    const val EXTRA_DAILY_SLOT_INDEX = "extra_daily_reminder_slot_index"
    private const val OBLIGATION_REQ_OFFSET = 1_000_000
    private const val SNOOZE_REQ_OFFSET = 2_000_000
    private const val DAILY_REQ_OFFSET = 3_000_000

    private var activeBgRingtone: Ringtone? = null
    private var activeBgVibrator: Vibrator? = null
    private var activeBgTts: TextToSpeech? = null
    private val firedSlotKeys: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    fun markSlotFired(key: String): Boolean {
        return firedSlotKeys.add(key)
    }

    fun stopBackgroundAlertMedia() {
        runCatching { activeBgRingtone?.stop() }
        activeBgRingtone = null
        runCatching { activeBgVibrator?.cancel() }
        activeBgVibrator = null
        runCatching { activeBgTts?.stop() }
    }

    internal fun playShortBackgroundAlertTone(appCtx: Context) {
        stopBackgroundAlertMedia()
        runCatching {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(appCtx, alarmUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                r?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            r?.play()
            activeBgRingtone = r
        }
        runCatching {
            val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appCtx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appCtx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            activeBgVibrator = v
            val pattern = longArrayOf(0, 450, 250, 450, 250, 600)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                v?.vibrate(pattern, -1)
            }
        }
        CoroutineScope(Dispatchers.Default).launch {
            delay(8_000L)
            runCatching { activeBgRingtone?.stop() }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            am?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

    fun canUseFullScreenIntent(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.canUseFullScreenIntent() == true
        } else {
            true
        }
    }

    fun openNotificationSettings(context: Context) {
        runCatching {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun openExactAlarmSettings(context: Context) {
        runCatching {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun openFullScreenIntentSettings(context: Context) {
        runCatching {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            } else {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val legacy = NotificationChannel(
                    CHANNEL_ID,
                    "تنبيهات مواعيد الدفع • مصروفي",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "إشعارات التذكير بمواعيد سداد العمليات والمبالغ المطلوب دفعها"
                    enableVibration(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(legacy)
            }

            if (nm.getNotificationChannel(CHANNEL_ID_ALARM) == null) {
                val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val alarmChannel = NotificationChannel(
                    CHANNEL_ID_ALARM,
                    "منبه مواعيد السداد (ملء الشاشة ورنين) • مصروفي",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "تنبيهات رنين وملء الشاشة لمواعيد السداد المهمة"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 400, 250, 400, 250, 600)
                    setSound(alarmSound, audioAttrs)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(alarmChannel)
            }

            if (nm.getNotificationChannel(CHANNEL_ID_QUIET) == null) {
                val quietChannel = NotificationChannel(
                    CHANNEL_ID_QUIET,
                    "إشعارات سداد هادئة • مصروفي",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "إشعارات سداد بدون رنين مستمر"
                    enableVibration(false)
                    setSound(null, null)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(quietChannel)
            }
        }
    }

    fun computeTriggerMillis(dueDateIso: String, dueTime24: String, leadMinutes: Int): Long? {
        if (dueDateIso.isBlank()) return null
        val safeTime = dueTime24.ifBlank { "09:00" }
        return runCatching {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
                isLenient = false
            }
            val parsed = sdf.parse("$dueDateIso $safeTime") ?: return null
            val dueMillis = parsed.time
            val now = System.currentTimeMillis()
            val leadMillis = leadMinutes.coerceAtLeast(0).toLong() * 60_000L
            val withLead = dueMillis - leadMillis
            when {
                // If lead time is still in the future, trigger at (due - lead)
                withLead > now + 2_000L -> withLead
                // If (due - lead) already passed because user scheduled a near-term time, fall back to dueMillis!
                dueMillis > now + 2_000L -> dueMillis
                // If user picked the current minute (within last 90s), fire in 3 seconds
                dueMillis >= now - 90_000L -> now + 3_000L
                else -> dueMillis
            }
        }.getOrNull()
    }

    private fun computeNextDailyTriggerMillis(time24: String): Long? {
        val parts = time24.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val m = parts.getOrNull(1)?.toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return when {
            cal.timeInMillis > now + 2_000L -> cal.timeInMillis
            // If user just set the current minute, fire within 3 seconds
            cal.timeInMillis >= now - 55_000L -> now + 3_000L
            else -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
        }
    }

    private fun buildAlarmPendingIntent(context: Context, txId: Long): PendingIntent {
        val intent = Intent(context, PaymentReminderReceiver::class.java).apply {
            action = "com.example.ACTION_TX_REMINDER_$txId"
            putExtra(EXTRA_TX_ID, txId)
        }
        val reqCode = ((txId % 500_000L).toInt()).coerceAtLeast(1)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, reqCode, intent, flags)
    }

    private fun buildObligationAlarmPendingIntent(context: Context, obligationId: Long): PendingIntent {
        val intent = Intent(context, PaymentReminderReceiver::class.java).apply {
            action = "com.example.ACTION_OBLIGATION_REMINDER_$obligationId"
            putExtra(EXTRA_OBLIGATION_ID, obligationId)
        }
        val reqCode = OBLIGATION_REQ_OFFSET + ((obligationId % 500_000L).toInt()).coerceAtLeast(1)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, reqCode, intent, flags)
    }

    private fun buildDailySlotPendingIntent(context: Context, slotIndex: Int): PendingIntent {
        val intent = Intent(context, PaymentReminderReceiver::class.java).apply {
            action = "com.example.ACTION_DAILY_EXPENSE_REMINDER_$slotIndex"
            putExtra(EXTRA_DAILY_SLOT_INDEX, slotIndex)
        }
        val reqCode = DAILY_REQ_OFFSET + slotIndex.coerceIn(0, 10)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, reqCode, intent, flags)
    }

    private fun scheduleExactOrFallback(context: Context, triggerAt: Long, pi: PendingIntent) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val safeTrigger = maxOf(triggerAt, System.currentTimeMillis() + 2_500L)
        runCatching {
            if (canScheduleExactAlarms(context)) {
                val showIntent = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                am.setAlarmClock(AlarmManager.AlarmClockInfo(safeTrigger, showIntent), pi)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTrigger, pi)
            } else {
                am.set(AlarmManager.RTC_WAKEUP, safeTrigger, pi)
            }
        }.onFailure {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canScheduleExactAlarms(context)) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTrigger, pi)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTrigger, pi)
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, safeTrigger, pi)
                }
            }
        }
    }

    fun cancelTransactionReminder(context: Context, txId: Long) {
        runCatching {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pi = buildAlarmPendingIntent(context, txId)
            am.cancel(pi)
            pi.cancel()
        }
    }

    fun scheduleTransactionReminder(
        context: Context,
        tx: TransactionEntity,
        notificationsEnabled: Boolean
    ) {
        if (tx.id <= 0L) return
        if (!notificationsEnabled || !tx.hasPaymentReminder || tx.paymentDueDate.isBlank() || tx.reminderMuted || tx.isDebtSettled) {
            cancelTransactionReminder(context, tx.id)
            return
        }
        val triggerAt = computeTriggerMillis(tx.paymentDueDate, tx.paymentDueTime, tx.reminderLeadMinutes)
        if (triggerAt == null || triggerAt <= System.currentTimeMillis() - 90_000L) {
            cancelTransactionReminder(context, tx.id)
            return
        }
        ensureChannel(context)
        scheduleExactOrFallback(context, triggerAt, buildAlarmPendingIntent(context, tx.id))
    }

    fun cancelObligationReminder(context: Context, obligationId: Long) {
        runCatching {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pi = buildObligationAlarmPendingIntent(context, obligationId)
            am.cancel(pi)
            pi.cancel()
        }
    }

    fun scheduleObligationReminder(
        context: Context,
        obligation: ObligationEntity,
        notificationsEnabled: Boolean
    ) {
        if (obligation.id <= 0L) return
        if (!notificationsEnabled || !obligation.reminderEnabled || obligation.dueDate.isBlank() || obligation.reminderMuted || obligation.isPaid) {
            cancelObligationReminder(context, obligation.id)
            return
        }
        val triggerAt = computeTriggerMillis(
            obligation.dueDate,
            obligation.dueTime,
            obligation.reminderLeadMinutes
        )
        if (triggerAt == null || triggerAt <= System.currentTimeMillis() - 90_000L) {
            cancelObligationReminder(context, obligation.id)
            return
        }
        ensureChannel(context)
        scheduleExactOrFallback(context, triggerAt, buildObligationAlarmPendingIntent(context, obligation.id))
    }

    fun cancelDailyExpenseReminders(context: Context) {
        runCatching {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            for (slot in 0..7) {
                val pi = buildDailySlotPendingIntent(context, slot)
                am.cancel(pi)
                pi.cancel()
            }
        }
    }

    fun syncDailyExpenseReminders(
        context: Context,
        settings: AppSettingsEntity
    ) {
        cancelDailyExpenseReminders(context)
        if (!settings.notificationsEnabled || !settings.dailyReminderEnabled) return
        ensureChannel(context)
        val slots = settings.dailyReminderTimesList()
        slots.forEachIndexed { index, slotTime ->
            val triggerAt = computeNextDailyTriggerMillis(slotTime)
            if (triggerAt != null) {
                scheduleExactOrFallback(context, triggerAt, buildDailySlotPendingIntent(context, index))
            }
        }
    }

    fun scheduleSnoozeReminder(
        context: Context,
        txId: Long,
        obligationId: Long,
        delayMinutes: Int = 15
    ) {
        if (txId <= 0L && obligationId <= 0L) return
        ensureChannel(context)
        val rawId = if (obligationId > 0L) obligationId else txId
        val intent = Intent(context, PaymentReminderReceiver::class.java).apply {
            action = "com.example.ACTION_SNOOZE_${obligationId}_$txId"
            if (txId > 0L) putExtra(EXTRA_TX_ID, txId)
            if (obligationId > 0L) putExtra(EXTRA_OBLIGATION_ID, obligationId)
        }
        val reqCode = SNOOZE_REQ_OFFSET + ((rawId % 400_000L).toInt()).coerceAtLeast(1)
        val pi = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + (delayMinutes.coerceAtLeast(1) * 60_000L)
        scheduleExactOrFallback(context, triggerAt, pi)
    }

    fun syncAllReminders(
        context: Context,
        transactions: List<TransactionEntity>,
        obligations: List<ObligationEntity> = emptyList(),
        notificationsEnabled: Boolean,
        settings: AppSettingsEntity? = null
    ) {
        var scheduledCount = 0
        val maxScheduled = 35
        transactions
            .filter { it.hasPaymentReminder && it.paymentDueDate.isNotBlank() }
            .sortedBy { it.paymentDueDate }
            .forEach { tx ->
                if (notificationsEnabled && !tx.reminderMuted && !tx.isDebtSettled && scheduledCount < maxScheduled) {
                    scheduleTransactionReminder(context, tx, true)
                    scheduledCount++
                } else {
                    cancelTransactionReminder(context, tx.id)
                }
            }
        obligations
            .filter { it.reminderEnabled && it.dueDate.isNotBlank() }
            .sortedBy { it.dueDate }
            .forEach { ob ->
                if (notificationsEnabled && !ob.reminderMuted && !ob.isPaid && scheduledCount < maxScheduled) {
                    scheduleObligationReminder(context, ob, true)
                    scheduledCount++
                } else {
                    cancelObligationReminder(context, ob.id)
                }
            }
        if (settings != null) {
            syncDailyExpenseReminders(context, settings.copy(notificationsEnabled = notificationsEnabled))
        }
    }

    /**
     * Real-time check invoked while the app is open so alarms fire at the exact minute
     * even if the OS batches background broadcasts.
     */
    fun checkAndFireDueAlarmsNow(
        context: Context,
        transactions: List<TransactionEntity>,
        obligations: List<ObligationEntity>,
        settings: AppSettingsEntity
    ) {
        if (!settings.notificationsEnabled) return
        val appCtx = context.applicationContext
        val todayIso = currentIsoDate()
        val nowTime24 = currentTime24()
        val isArabic = settings.language != "en"

        // 1. Check scheduled obligations due right now
        for (ob in obligations) {
            if (!ob.isPaid && ob.reminderEnabled && !ob.reminderMuted && ob.dueDate == todayIso) {
                val dueTime = ob.dueTime.ifBlank { "09:00" }
                val triggerMillis = computeTriggerMillis(ob.dueDate, dueTime, ob.reminderLeadMinutes)
                val now = System.currentTimeMillis()
                val isDueMinute = (dueTime == nowTime24) ||
                    (triggerMillis != null && triggerMillis in (now - 45_000L)..(now + 4_000L))
                if (isDueMinute) {
                    val key = "OB_${ob.id}_${todayIso}_$nowTime24"
                    if (markSlotFired(key)) {
                        val intent = Intent(appCtx, PaymentReminderReceiver::class.java).apply {
                            putExtra(EXTRA_OBLIGATION_ID, ob.id)
                        }
                        appCtx.sendBroadcast(intent)
                    }
                }
            }
        }

        // 2. Check explicitly scheduled debt transactions due right now
        for (tx in transactions) {
            if (tx.isDebt && !tx.isDebtSettled && tx.hasPaymentReminder && !tx.reminderMuted &&
                tx.paymentDueDate.isNotBlank() && tx.paymentDueDate == todayIso
            ) {
                val dueTime = tx.paymentDueTime.ifBlank { "09:00" }
                val triggerMillis = computeTriggerMillis(tx.paymentDueDate, dueTime, tx.reminderLeadMinutes)
                val now = System.currentTimeMillis()
                val isDueMinute = (dueTime == nowTime24) ||
                    (triggerMillis != null && triggerMillis in (now - 45_000L)..(now + 4_000L))
                if (isDueMinute) {
                    val key = "TX_${tx.id}_${todayIso}_$nowTime24"
                    if (markSlotFired(key)) {
                        val intent = Intent(appCtx, PaymentReminderReceiver::class.java).apply {
                            putExtra(EXTRA_TX_ID, tx.id)
                        }
                        appCtx.sendBroadcast(intent)
                    }
                }
            }
        }

        // 3. Check daily transaction/expense reminder slots
        if (settings.dailyReminderEnabled) {
            val slots = settings.dailyReminderTimesList()
            slots.forEachIndexed { idx, slotTime ->
                if (slotTime == nowTime24) {
                    val key = "DAILY_${idx}_${todayIso}_$slotTime"
                    if (markSlotFired(key)) {
                        val intent = Intent(appCtx, PaymentReminderReceiver::class.java).apply {
                            putExtra(EXTRA_DAILY_SLOT_INDEX, idx)
                        }
                        appCtx.sendBroadcast(intent)
                    }
                }
            }
        }
    }

    /** Launches an immediate live preview of the full-screen alarm + ringtone + TTS voice announcement. */
    fun triggerTestAlarmPreview(
        context: Context,
        settings: AppSettingsEntity,
        isArabic: Boolean,
        customTitle: String? = null,
        customAmountText: String? = null,
        customParty: String? = null,
        customDueInfo: String? = null,
        alertModeOverride: String? = null,
        fullScreenOverride: Boolean? = null,
        speakAloudOverride: Boolean? = null
    ) {
        stopBackgroundAlertMedia()
        val sym = currencySymbol(settings.defaultCurrency, isArabic)
        val title = customTitle?.takeIf { it.isNotBlank() }
            ?: tr(isArabic, "فاتورة الكهرباء / قسط شهري", "Monthly Bill / Installment")
        val amountText = customAmountText?.takeIf { it.isNotBlank() }
            ?: "1,500.00 $sym"
        val party = customParty ?: tr(isArabic, "شركة الكهرباء", "Utility Company")
        val dueInfo = customDueInfo ?: "${currentIsoDate()} ${formatTime12Hour(currentTime24(), isArabic)}"
        val mode = alertModeOverride ?: settings.defaultAlertMode
        val fullScreen = fullScreenOverride ?: settings.fullScreenAlertsEnabled
        val speakAloud = (speakAloudOverride ?: settings.speakRemindersAloud) ||
            mode == ReminderAlertMode.NOTIFICATION_VOICE
        val playRing = settings.ringtoneAlertsEnabled &&
            (mode == ReminderAlertMode.RING || mode == ReminderAlertMode.NOTIFICATION_RING)

        if (fullScreen) {
            val alarmIntent = Intent(context, PaymentAlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(PaymentAlarmActivity.EXTRA_TITLE, title)
                putExtra(PaymentAlarmActivity.EXTRA_AMOUNT_TEXT, amountText)
                putExtra(PaymentAlarmActivity.EXTRA_PARTY, party)
                putExtra(PaymentAlarmActivity.EXTRA_DUE_INFO, dueInfo)
                putExtra(PaymentAlarmActivity.EXTRA_NOTE, tr(isArabic, "هذه معاينة حية لشكل إشعار السداد بملء الشاشة والصوت", "Live preview of your payment alarm"))
                putExtra(PaymentAlarmActivity.EXTRA_ALERT_MODE, mode)
                putExtra(PaymentAlarmActivity.EXTRA_SPEAK_ALOUD, speakAloud)
                putExtra(PaymentAlarmActivity.EXTRA_PLAY_RING, playRing)
                putExtra(PaymentAlarmActivity.EXTRA_IS_ARABIC, isArabic)
                putExtra(PaymentAlarmActivity.EXTRA_IS_TEST, true)
            }
            runCatching { context.startActivity(alarmIntent) }
        } else {
            ensureChannel(context)
            val channelId = if (playRing) CHANNEL_ID_ALARM else CHANNEL_ID_QUIET
            val body = "$amountText • $party • $dueInfo"
            val notif = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(tr(isArabic, "تجربة تنبيه: $title", "Test Alert: $title"))
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            if (hasNotificationPermission(context)) {
                runCatching { NotificationManagerCompat.from(context).notify(999_101, notif) }
            }
            if (playRing) {
                playShortBackgroundAlertTone(context.applicationContext)
            }
            if (speakAloud) {
                speakOnceInBackground(context.applicationContext, isArabic, "$title. $amountText")
            }
        }
    }

    internal fun speakOnceInBackground(appCtx: Context, isArabic: Boolean, message: String) {
        runCatching {
            activeBgTts?.stop()
            activeBgTts?.shutdown()
            var engine: TextToSpeech? = null
            engine = TextToSpeech(appCtx) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val loc = if (isArabic) Locale("ar", "EG") else Locale.US
                    val res = engine?.setLanguage(loc)
                    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                        engine?.setLanguage(if (isArabic) Locale("ar") else Locale.ENGLISH)
                    }
                    engine?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "masrofy_bg_tts")
                }
            }
            activeBgTts = engine
        }
    }
}

class PaymentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val txId = intent.getLongExtra(PaymentNotificationManager.EXTRA_TX_ID, -1L)
        val obligationId = intent.getLongExtra(PaymentNotificationManager.EXTRA_OBLIGATION_ID, -1L)
        val dailySlotIndex = intent.getIntExtra(PaymentNotificationManager.EXTRA_DAILY_SLOT_INDEX, -1)
        if (txId <= 0L && obligationId <= 0L && dailySlotIndex < 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appCtx = context.applicationContext
                val db = AppDatabase.getInstance(appCtx)
                val settings = db.settingsDao().getSettingsOnce()
                // Strict rule: if user turned off app notifications, NEVER ring or show anything!
                if (settings != null && !settings.notificationsEnabled) return@launch

                val isArabic = (settings?.language ?: "ar") != "en"
                val globalFullScreen = settings?.fullScreenAlertsEnabled ?: true
                val globalRing = settings?.ringtoneAlertsEnabled ?: true
                val globalSpeak = settings?.speakRemindersAloud ?: false
                PaymentNotificationManager.ensureChannel(appCtx)

                if (dailySlotIndex >= 0) {
                    val currentSettings = settings ?: AppSettingsEntity()
                    if (!currentSettings.dailyReminderEnabled) return@launch
                    val slots = currentSettings.dailyReminderTimesList()
                    val slotTime = slots.getOrElse(dailySlotIndex) { currentSettings.primaryDailyReminderTime() }
                    val todayKey = "DAILY_${dailySlotIndex}_${currentIsoDate()}_$slotTime"
                    PaymentNotificationManager.markSlotFired(todayKey)

                    val totalCount = currentSettings.dailyReminderCount()
                    val slotBadge = if (totalCount > 1) {
                        tr(isArabic, " (التذكير ${dailySlotIndex + 1} من $totalCount اليوم)", " (Reminder ${dailySlotIndex + 1} of $totalCount today)")
                    } else ""
                    val title = tr(
                        isArabic,
                        "تذكير إضافة العمليات والمصروفات$slotBadge",
                        "Daily Expense & Transaction Reminder$slotBadge"
                    )
                    val dueInfo = "${currentIsoDate()} • ${formatTime12Hour(slotTime, isArabic)}"
                    val note = tr(
                        isArabic,
                        "سجّل مصروفاتك وعملياتك اليومية الآن للحفاظ على دقة رصيدك المتاح للصرف",
                        "Log your daily expenses and transactions now to keep your available balance accurate"
                    )
                    val mode = currentSettings.defaultAlertMode
                    val notifId = 3_000_000 + dailySlotIndex

                    dispatchPaymentAlert(
                        appCtx = appCtx,
                        notifId = notifId,
                        title = title,
                        amountText = "",
                        party = tr(isArabic, "سجل مصروفي اليومي", "Daily Masrofy Log"),
                        dueInfo = dueInfo,
                        note = note,
                        alertMode = mode,
                        fullScreenEnabled = globalFullScreen,
                        speakAloud = globalSpeak || mode == ReminderAlertMode.NOTIFICATION_VOICE,
                        playRing = globalRing &&
                            (mode == ReminderAlertMode.RING || mode == ReminderAlertMode.NOTIFICATION_RING),
                        isArabic = isArabic,
                        txId = -1L,
                        obligationId = -1L,
                        targetRoute = AppTab.ADD.route,
                        isDailyExpenseReminder = true
                    )
                    // Schedule tomorrow's slots automatically
                    PaymentNotificationManager.syncDailyExpenseReminders(appCtx, currentSettings)
                    return@launch
                }

                if (obligationId > 0L) {
                    val ob = db.obligationDao().getByIdOnce(obligationId) ?: return@launch
                    if (!ob.reminderEnabled || ob.dueDate.isBlank() || ob.reminderMuted || ob.isPaid) return@launch
                    PaymentNotificationManager.markSlotFired("OB_${ob.id}_${currentIsoDate()}_${currentTime24()}")

                    val sym = currencySymbol(ob.currency, isArabic)
                    val amtStr = "${formatAmountMinor(ob.amountMinor)} $sym"
                    val dueInfo = "${ob.dueDate} ${formatTime12Hour(ob.dueTime, isArabic)}".trim()
                    val notifId = 1_000_000 + ((obligationId % 500_000L).toInt()).coerceAtLeast(1)

                    dispatchPaymentAlert(
                        appCtx = appCtx,
                        notifId = notifId,
                        title = ob.reason.ifBlank { tr(isArabic, "التزام بموعد سداد", "Scheduled Obligation") },
                        amountText = amtStr,
                        party = ob.party,
                        dueInfo = dueInfo,
                        note = ob.note,
                        alertMode = ob.reminderAlertMode,
                        fullScreenEnabled = ob.reminderFullScreen && globalFullScreen,
                        speakAloud = ob.reminderSpeakAloud || globalSpeak ||
                            ob.reminderAlertMode == ReminderAlertMode.NOTIFICATION_VOICE,
                        playRing = globalRing &&
                            (ob.reminderAlertMode == ReminderAlertMode.RING ||
                                ob.reminderAlertMode == ReminderAlertMode.NOTIFICATION_RING),
                        isArabic = isArabic,
                        txId = -1L,
                        obligationId = obligationId,
                        targetRoute = AppTab.OBLIGATIONS.route
                    )
                    return@launch
                }

                val tx = db.transactionDao().getByIdOnce(txId) ?: return@launch
                if (!tx.hasPaymentReminder || tx.paymentDueDate.isBlank() || tx.reminderMuted || tx.isDebtSettled) return@launch
                PaymentNotificationManager.markSlotFired("TX_${tx.id}_${currentIsoDate()}_${currentTime24()}")

                val itemLabel = tx.itemName.ifBlank { tx.category }
                val sym = currencySymbol(tx.currency, isArabic)
                val amtStr = "${formatAmountMinor(tx.amountMinor)} $sym"
                val dueInfo = "${tx.paymentDueDate} ${formatTime12Hour(tx.paymentDueTime, isArabic)}".trim()
                val notifId = ((txId % 500_000L).toInt()).coerceAtLeast(100)

                dispatchPaymentAlert(
                    appCtx = appCtx,
                    notifId = notifId,
                    title = itemLabel,
                    amountText = amtStr,
                    party = tx.debtParty,
                    dueInfo = dueInfo,
                    note = tx.note,
                    alertMode = tx.reminderAlertMode,
                    fullScreenEnabled = tx.reminderFullScreen && globalFullScreen,
                    speakAloud = tx.reminderSpeakAloud || globalSpeak ||
                        tx.reminderAlertMode == ReminderAlertMode.NOTIFICATION_VOICE,
                    playRing = globalRing &&
                        (tx.reminderAlertMode == ReminderAlertMode.RING ||
                            tx.reminderAlertMode == ReminderAlertMode.NOTIFICATION_RING),
                    isArabic = isArabic,
                    txId = txId,
                    obligationId = -1L,
                    targetRoute = AppTab.OBLIGATIONS.route
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun dispatchPaymentAlert(
        appCtx: Context,
        notifId: Int,
        title: String,
        amountText: String,
        party: String,
        dueInfo: String,
        note: String,
        alertMode: String,
        fullScreenEnabled: Boolean,
        speakAloud: Boolean,
        playRing: Boolean,
        isArabic: Boolean,
        txId: Long,
        obligationId: Long,
        targetRoute: String,
        isDailyExpenseReminder: Boolean = false
    ) {
        val alarmIntent = Intent(appCtx, PaymentAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(PaymentAlarmActivity.EXTRA_TITLE, title)
            putExtra(PaymentAlarmActivity.EXTRA_AMOUNT_TEXT, amountText)
            putExtra(PaymentAlarmActivity.EXTRA_PARTY, party)
            putExtra(PaymentAlarmActivity.EXTRA_DUE_INFO, dueInfo)
            putExtra(PaymentAlarmActivity.EXTRA_NOTE, note)
            putExtra(PaymentAlarmActivity.EXTRA_ALERT_MODE, alertMode)
            putExtra(PaymentAlarmActivity.EXTRA_SPEAK_ALOUD, speakAloud)
            putExtra(PaymentAlarmActivity.EXTRA_PLAY_RING, playRing)
            putExtra(PaymentAlarmActivity.EXTRA_IS_ARABIC, isArabic)
            putExtra(PaymentAlarmActivity.EXTRA_TX_ID, txId)
            putExtra(PaymentAlarmActivity.EXTRA_OBLIGATION_ID, obligationId)
            putExtra(PaymentAlarmActivity.EXTRA_NOTIF_ID, notifId)
            putExtra(PaymentAlarmActivity.EXTRA_IS_TEST, isDailyExpenseReminder)
        }

        val fullScreenPi = PendingIntent.getActivity(
            appCtx,
            notifId + 50_000,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openMainIntent = Intent(appCtx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MasrofyWidgetProvider.EXTRA_INITIAL_TAB, targetRoute)
        }
        val contentPi = PendingIntent.getActivity(
            appCtx,
            notifId,
            if (fullScreenEnabled) alarmIntent else openMainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val partyPart = if (party.isNotBlank()) " • $party" else ""
        val notifTitle = if (isDailyExpenseReminder) {
            title
        } else {
            tr(isArabic, "موعد سداد: $title", "Payment Due: $title")
        }
        val notifBody = if (isDailyExpenseReminder) {
            note.ifBlank { dueInfo }
        } else {
            tr(
                isArabic,
                "المبلغ: $amountText$partyPart • الموعد: $dueInfo",
                "Amount: $amountText$partyPart • Due: $dueInfo"
            )
        }

        val channelId = if (playRing) {
            PaymentNotificationManager.CHANNEL_ID_ALARM
        } else {
            PaymentNotificationManager.CHANNEL_ID_QUIET
        }

        val builder = NotificationCompat.Builder(appCtx, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notifTitle)
            .setContentText(notifBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notifBody))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPi)

        if (fullScreenEnabled) {
            builder.setFullScreenIntent(fullScreenPi, true)
        }

        if (PaymentNotificationManager.hasNotificationPermission(appCtx)) {
            runCatching {
                NotificationManagerCompat.from(appCtx).notify(notifId, builder.build())
            }
        }

        if (fullScreenEnabled) {
            val launched = runCatching {
                appCtx.startActivity(alarmIntent)
                true
            }.getOrDefault(false)
            if (!launched) {
                if (playRing) {
                    PaymentNotificationManager.playShortBackgroundAlertTone(appCtx)
                }
                if (speakAloud) {
                    PaymentNotificationManager.speakOnceInBackground(
                        appCtx,
                        isArabic,
                        "$notifTitle. $notifBody"
                    )
                }
            }
        } else {
            if (playRing) {
                PaymentNotificationManager.playShortBackgroundAlertTone(appCtx)
            }
            if (speakAloud) {
                PaymentNotificationManager.speakOnceInBackground(
                    appCtx,
                    isArabic,
                    "$notifTitle. $notifBody"
                )
            }
        }
    }
}
