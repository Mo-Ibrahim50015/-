package com.example.ui

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.example.data.AppDatabase
import com.example.data.ReminderAlertMode
import com.example.ui.theme.AmountDisplayStyle
import com.example.ui.theme.MizanityTheme
import com.example.util.PaymentNotificationManager
import com.example.util.currentIsoDate
import com.example.util.formatAlertModeLabel
import com.example.util.tr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class PaymentAlarmActivity : ComponentActivity() {
    companion object {
        const val EXTRA_TITLE = "alarm_title"
        const val EXTRA_AMOUNT_TEXT = "alarm_amount_text"
        const val EXTRA_PARTY = "alarm_party"
        const val EXTRA_DUE_INFO = "alarm_due_info"
        const val EXTRA_NOTE = "alarm_note"
        const val EXTRA_ALERT_MODE = "alarm_alert_mode"
        const val EXTRA_SPEAK_ALOUD = "alarm_speak_aloud"
        const val EXTRA_PLAY_RING = "alarm_play_ring"
        const val EXTRA_IS_ARABIC = "alarm_is_arabic"
        const val EXTRA_TX_ID = "alarm_tx_id"
        const val EXTRA_OBLIGATION_ID = "alarm_obligation_id"
        const val EXTRA_NOTIF_ID = "alarm_notif_id"
        const val EXTRA_IS_TEST = "alarm_is_test"
    }

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var tts: TextToSpeech? = null
    private var isSoundActive by mutableStateOf(false)
    private var speechText: String = ""
    private var isArabicMode: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreenDisplay()
        PaymentNotificationManager.stopBackgroundAlertMedia()

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val amountText = intent.getStringExtra(EXTRA_AMOUNT_TEXT).orEmpty()
        val party = intent.getStringExtra(EXTRA_PARTY).orEmpty()
        val dueInfo = intent.getStringExtra(EXTRA_DUE_INFO).orEmpty()
        val note = intent.getStringExtra(EXTRA_NOTE).orEmpty()
        val alertMode = intent.getStringExtra(EXTRA_ALERT_MODE) ?: ReminderAlertMode.NOTIFICATION_RING
        val speakAloud = intent.getBooleanExtra(EXTRA_SPEAK_ALOUD, false) ||
            alertMode == ReminderAlertMode.NOTIFICATION_VOICE
        val playRing = intent.getBooleanExtra(EXTRA_PLAY_RING, true) &&
            (alertMode == ReminderAlertMode.RING || alertMode == ReminderAlertMode.NOTIFICATION_RING)
        isArabicMode = intent.getBooleanExtra(EXTRA_IS_ARABIC, true)
        val txId = intent.getLongExtra(EXTRA_TX_ID, -1L)
        val obligationId = intent.getLongExtra(EXTRA_OBLIGATION_ID, -1L)
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
        val isTest = intent.getBooleanExtra(EXTRA_IS_TEST, false)

        speechText = buildSpeechAnnouncement(
            isArabic = isArabicMode,
            title = title,
            amountText = amountText,
            party = party,
            dueInfo = dueInfo,
            note = note
        )

        if (playRing) {
            startAlarmRingtoneAndVibration()
        }
        if (speakAloud) {
            startVoiceAnnouncement()
        }

        setContent {
            val layoutDir = if (isArabicMode) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
                MizanityTheme(darkTheme = true) {
                    PaymentAlarmFullScreenContent(
                        title = title,
                        amountText = amountText,
                        party = party,
                        dueInfo = dueInfo,
                        note = note,
                        alertMode = alertMode,
                        speakAloud = speakAloud,
                        isSoundActive = isSoundActive,
                        isArabic = isArabicMode,
                        isTest = isTest,
                        onSpeakAgain = {
                            stopRingtoneAndVibration()
                            startVoiceAnnouncement()
                        },
                        onStopSound = {
                            stopRingtoneAndVibration()
                            runCatching { tts?.stop() }
                        },
                        onMarkPaid = {
                            stopAllMedia()
                            dismissNotification(notifId)
                            if (!isTest) {
                                markItemPaidInBackground(txId, obligationId)
                            }
                            finish()
                        },
                        onSnooze15Min = {
                            stopAllMedia()
                            dismissNotification(notifId)
                            if (!isTest) {
                                PaymentNotificationManager.scheduleSnoozeReminder(
                                    context = applicationContext,
                                    txId = txId,
                                    obligationId = obligationId,
                                    delayMinutes = 15
                                )
                            }
                            finish()
                        },
                        onDismiss = {
                            stopAllMedia()
                            dismissNotification(notifId)
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun configureLockScreenDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            runCatching {
                val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                km?.requestDismissKeyguard(this, null)
            }
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun buildSpeechAnnouncement(
        isArabic: Boolean,
        title: String,
        amountText: String,
        party: String,
        dueInfo: String,
        note: String
    ): String {
        return if (isArabic) {
            buildString {
                append("تنبيه من تطبيق مصروفي. ")
                append("حان موعد سداد ")
                append(title.ifBlank { "مبلغ مستحق" })
                if (amountText.isNotBlank()) {
                    append(". بقيمة ")
                    append(amountText)
                }
                if (party.isNotBlank()) {
                    append(". للجهة أو الشخص ")
                    append(party)
                }
                if (dueInfo.isNotBlank()) {
                    append(". موعد السداد ")
                    append(dueInfo)
                }
                if (note.isNotBlank()) {
                    append(". ملاحظة: ")
                    append(note)
                }
            }
        } else {
            buildString {
                append("Masrofy payment reminder. ")
                append("Payment due for ")
                append(title.ifBlank { "scheduled amount" })
                if (amountText.isNotBlank()) {
                    append(". Amount ")
                    append(amountText)
                }
                if (party.isNotBlank()) {
                    append(". Pay to ")
                    append(party)
                }
                if (dueInfo.isNotBlank()) {
                    append(". Due at ")
                    append(dueInfo)
                }
                if (note.isNotBlank()) {
                    append(". Note: ")
                    append(note)
                }
            }
        }
    }

    private fun startAlarmRingtoneAndVibration() {
        runCatching {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(applicationContext, alarmUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                r?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                r?.isLooping = true
            }
            r?.play()
            ringtone = r
            isSoundActive = true
        }
        runCatching {
            val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator = v
            val pattern = longArrayOf(0, 500, 350, 500, 350, 700)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                v?.vibrate(pattern, 0)
            }
        }
    }

    private fun startVoiceAnnouncement() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
            tts = TextToSpeech(applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val targetLocale = if (isArabicMode) Locale("ar", "EG") else Locale.US
                    val res = tts?.setLanguage(targetLocale)
                    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(if (isArabicMode) Locale("ar") else Locale.ENGLISH)
                    }
                    tts?.setSpeechRate(0.95f)
                    tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "masrofy_alarm_tts")
                }
            }
        }
    }

    private fun stopRingtoneAndVibration() {
        runCatching { ringtone?.stop() }
        ringtone = null
        runCatching { vibrator?.cancel() }
        vibrator = null
        isSoundActive = false
    }

    private fun stopAllMedia() {
        stopRingtoneAndVibration()
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
    }

    private fun dismissNotification(notifId: Int) {
        if (notifId > 0) {
            runCatching { NotificationManagerCompat.from(applicationContext).cancel(notifId) }
        }
    }

    private fun markItemPaidInBackground(txId: Long, obligationId: Long) {
        val ctx = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val db = AppDatabase.getInstance(ctx)
                if (obligationId > 0L) {
                    val settings = db.settingsDao().getSettingsOnce()
                    val src = settings?.lastSelectedSource?.ifBlank { "كاش / نقدي" } ?: "كاش / نقدي"
                    db.obligationDao().setPaidStatus(obligationId, true, currentIsoDate(), src)
                    PaymentNotificationManager.cancelObligationReminder(ctx, obligationId)
                } else if (txId > 0L) {
                    db.transactionDao().setDebtSettled(txId, true)
                    PaymentNotificationManager.cancelTransactionReminder(ctx, txId)
                }
            }
        }
    }

    override fun onDestroy() {
        stopAllMedia()
        super.onDestroy()
    }
}

@Composable
private fun PaymentAlarmFullScreenContent(
    title: String,
    amountText: String,
    party: String,
    dueInfo: String,
    note: String,
    alertMode: String,
    speakAloud: Boolean,
    isSoundActive: Boolean,
    isArabic: Boolean,
    isTest: Boolean,
    onSpeakAgain: () -> Unit,
    onStopSound: () -> Unit,
    onMarkPaid: () -> Unit,
    onSnooze15Min: () -> Unit,
    onDismiss: () -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "alarm_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF071224),
                        Color(0xFF0E1F3D),
                        Color(0xFF0A1426)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFD97706).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isTest) {
                                tr(isArabic, "معاينة إشعار ملء الشاشة • مصروفي", "Full-Screen Alert Preview • Masrofy")
                            } else {
                                tr(isArabic, "تنبيه موعد سداد • مصروفي", "Payment Due Alarm • Masrofy")
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFDE68A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Alarm,
                            contentDescription = null,
                            tint = Color(0xFF0A1426),
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Text(
                    text = title.ifBlank { tr(isArabic, "مبلغ مطلوب سداده الآن", "Scheduled Payment Due") },
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                if (amountText.isNotBlank()) {
                    Text(
                        text = amountText,
                        style = AmountDisplayStyle.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color(0xFF82F5C1),
                        textAlign = TextAlign.Center
                    )
                }

                // Details Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (party.isNotBlank()) {
                            AlarmDetailLine(
                                icon = Icons.Outlined.Person,
                                label = tr(isArabic, "الجهة / الشخص:", "Pay To:"),
                                value = party
                            )
                        }
                        if (dueInfo.isNotBlank()) {
                            AlarmDetailLine(
                                icon = Icons.Outlined.Schedule,
                                label = tr(isArabic, "موعد السداد:", "Due Time:"),
                                value = dueInfo
                            )
                        }
                        AlarmDetailLine(
                            icon = Icons.Outlined.NotificationsActive,
                            label = tr(isArabic, "وضع التنبيه:", "Alert Mode:"),
                            value = formatAlertModeLabel(alertMode, isArabic)
                        )
                        if (note.isNotBlank()) {
                            Text(
                                text = "${tr(isArabic, "التفاصيل:", "Details:")} $note",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }

                // Voice & Sound Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onSpeakAgain,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("alarm_speak_btn"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF82F5C1).copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RecordVoiceOver,
                            contentDescription = null,
                            tint = Color(0xFF82F5C1),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (speakAloud) {
                                tr(isArabic, "إعادة النطق", "Speak Again")
                            } else {
                                tr(isArabic, "نطق التفاصيل", "Read Aloud")
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (isSoundActive) {
                        OutlinedButton(
                            onClick = onStopSound,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("alarm_mute_btn"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VolumeOff,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr(isArabic, "كتم الرنين", "Silence Ring"),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onMarkPaid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("alarm_mark_paid_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00A86B),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTest) {
                            tr(isArabic, "تمت المعاينة بنجاح ✓", "Preview Looks Good ✓")
                        } else {
                            tr(isArabic, "تم السداد الآن ✓", "Mark as Paid Now ✓")
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isTest) {
                        OutlinedButton(
                            onClick = onSnooze15Min,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("alarm_snooze_btn"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Snooze,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr(isArabic, "غفوة ١٥ دقيقة", "Snooze 15m"),
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("alarm_dismiss_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr(isArabic, "إغلاق التنبيه", "Dismiss"),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmDetailLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFFBBF24),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF94A3B8)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
    }
}
