package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppSettingsEntity
import com.example.data.TransactionType
import com.example.ui.AppTab
import com.example.ui.MizanityViewModel
import com.example.ui.components.AppLockGateScreen
import com.example.ui.components.BackupDrawerMode
import com.example.ui.components.BackupSideDrawerOverlay
import com.example.ui.components.MizanityBottomBar
import com.example.ui.components.MizanityTopBar
import com.example.ui.components.NotificationCenterSheet
import com.example.ui.components.SplashVideoScreen
import com.example.ui.screens.AddTransactionScreen
import com.example.ui.screens.DebtsScreen
import com.example.ui.screens.FlowDetailsScreen
import com.example.ui.screens.FlowDrillDownScreen
import com.example.ui.screens.FlowDrillDownSpec
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LifetimeMoneyScreen
import com.example.ui.screens.LogScreen
import com.example.ui.screens.MoneyFlowGuideScreen
import com.example.ui.screens.ObligationsScreen
import com.example.ui.screens.PaymentsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.ReportsTopTab
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SettingsSection
import com.example.ui.theme.LocalSereneColors
import com.example.ui.theme.MizanityTheme
import com.example.util.PaymentNotificationManager
import com.example.util.observeFirebaseUserFlow
import com.example.util.signInWithGoogleAccountChooser
import com.example.util.signOutGoogleAccount
import com.example.util.tr
import com.example.widget.MasrofyWidgetProvider

class MainActivity : FragmentActivity() {
    private val pendingRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingRoute.value = intent?.getStringExtra(MasrofyWidgetProvider.EXTRA_INITIAL_TAB)
        setContent {
            MizanityApp(
                externalRoute = pendingRoute.value,
                onExternalRouteConsumed = { pendingRoute.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(MasrofyWidgetProvider.EXTRA_INITIAL_TAB)?.let { route ->
            pendingRoute.value = route
        }
    }
}

@Composable
fun MizanityApp(
    externalRoute: String? = null,
    onExternalRouteConsumed: () -> Unit = {},
    viewModel: MizanityViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val showSplash by viewModel.showSplash.collectAsStateWithLifecycle()
    val isLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val isSecurityReady by viewModel.isSecurityReady.collectAsStateWithLifecycle()

    val isArabic = settings.language != "en"
    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    val layoutDir = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        MizanityTheme(darkTheme = isDark) {
            // Brief splash backdrop while persisted lock state initializes (< 30ms)
            if (!isSecurityReady) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0A1628))
                )
                return@MizanityTheme
            }

            // CRITICAL: Security Lock Gate runs BEFORE the Splash Video when launch lock is enabled!
            if (isLocked && settings.hasAnyLockMethodConfigured()) {
                AppLockGateScreen(
                    settings = settings,
                    isArabic = isArabic,
                    onUnlocked = viewModel::unlockApp
                )
                return@MizanityTheme
            }

            if (showSplash) {
                SplashVideoScreen(
                    isArabic = isArabic,
                    onFinished = viewModel::finishSplash
                )
                return@MizanityTheme
            }

            MainAppShell(
                viewModel = viewModel,
                settings = settings,
                isArabic = isArabic,
                isDark = isDark,
                externalRoute = externalRoute,
                onExternalRouteConsumed = onExternalRouteConsumed
            )
        }
    }
}

@Composable
private fun MainAppShell(
    viewModel: MizanityViewModel,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    isDark: Boolean,
    externalRoute: String?,
    onExternalRouteConsumed: () -> Unit
) {
    val ext = LocalSereneColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val alertSummary by viewModel.alertSummary.collectAsStateWithLifecycle()
    val allTransactions by viewModel.transactions.collectAsStateWithLifecycle()
    val allObligations by viewModel.obligations.collectAsStateWithLifecycle()
    val isCloudBackupBusy by viewModel.isCloudBackupBusy.collectAsStateWithLifecycle()
    val cloudBackupSummary by viewModel.cloudBackupSummary.collectAsStateWithLifecycle()
    val cloudBackupError by viewModel.cloudBackupError.collectAsStateWithLifecycle()
    val firebaseUser by remember { observeFirebaseUserFlow() }
        .collectAsStateWithLifecycle(initialValue = null)

    // Saveable tab, section, drill-down, and backup side-drawer state
    var currentTabName by rememberSaveable { mutableStateOf(AppTab.HOME.name) }
    var previousTabName by rememberSaveable { mutableStateOf(AppTab.HOME.name) }
    var guideOriginTabName by rememberSaveable { mutableStateOf(AppTab.HOME.name) }
    var reportsRequestedTabName by rememberSaveable { mutableStateOf(ReportsTopTab.REPORTS.name) }
    var reportsTabNonce by rememberSaveable { mutableStateOf(0) }
    var settingsTargetSectionName by rememberSaveable { mutableStateOf(SettingsSection.PERSONAL_INFO.name) }
    var addInitialType by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var addInitialDebt by rememberSaveable { mutableStateOf(false) }
    var debtsInitialTab by rememberSaveable { mutableStateOf("all") }
    var paymentsInitialTab by rememberSaveable { mutableStateOf("all") }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var backupDrawerModeName by rememberSaveable { mutableStateOf<String?>(null) }
    var isGoogleAuthBusy by remember { mutableStateOf(false) }
    var activeDrillDownSpec by remember { mutableStateOf<FlowDrillDownSpec?>(null) }
    var drillDownReturnTabName by rememberSaveable { mutableStateOf<String?>(null) }

    val backupDrawerMode = remember(backupDrawerModeName) {
        backupDrawerModeName?.let { raw ->
            runCatching { BackupDrawerMode.valueOf(raw) }.getOrNull()
        }
    }

    // Load cloud backup status for the currently signed-in email whenever drawer opens or account changes
    LaunchedEffect(backupDrawerMode, firebaseUser?.uid) {
        if (firebaseUser == null) {
            viewModel.clearCloudBackupState()
        } else if (backupDrawerMode != null) {
            viewModel.loadCloudBackupSummary()
        }
    }

    val currentTab = remember(currentTabName) {
        runCatching { AppTab.valueOf(currentTabName) }.getOrDefault(AppTab.HOME)
    }
    val previousTab = remember(previousTabName) {
        runCatching { AppTab.valueOf(previousTabName) }.getOrDefault(AppTab.HOME)
    }
    val settingsTargetSection = remember(settingsTargetSectionName) {
        runCatching { SettingsSection.valueOf(settingsTargetSectionName) }.getOrDefault(SettingsSection.PERSONAL_INFO)
    }
    val reportsRequestedTab = remember(reportsRequestedTabName) {
        runCatching { ReportsTopTab.valueOf(reportsRequestedTabName) }.getOrDefault(ReportsTopTab.REPORTS)
    }

    // Hoisted ActivityResult launchers registered ONCE at shell root (never unregistered on tab switches)
    // Primary profile photo picker opens Device Files (DocumentsUI) directly via zero-permission OpenDocument
    val deviceFilesImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.updateUserAvatar(uri)
    }

    val fallbackImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.updateUserAvatar(uri)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.updateUserAvatar(uri)
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.writeJsonBackupToUri(uri)
    }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) viewModel.writeReportPdfToUri(uri)
    }

    val createExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri ->
        if (uri != null) viewModel.writeReportExcelToUri(uri)
    }

    val createImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) viewModel.writeReportImageToUri(uri)
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.restoreJsonBackupFromUri(uri)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val requestNotificationPermissionIfNeeded: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !PaymentNotificationManager.hasNotificationPermission(context)
        ) {
            runCatching {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Automatically request POST_NOTIFICATIONS once on Android 13+ when notifications are enabled
    LaunchedEffect(settings.notificationsEnabled) {
        if (settings.notificationsEnabled) {
            requestNotificationPermissionIfNeeded()
        }
    }

    // Safe launch wrapper: opens Device Files (DocumentsUI) for image selection with zero-permission fallbacks
    val launchPhotoPicker: () -> Unit = {
        val launchedDeviceFiles = runCatching {
            deviceFilesImagePickerLauncher.launch(arrayOf("image/*"))
            true
        }.getOrDefault(false)
        if (!launchedDeviceFiles) {
            val launchedContent = runCatching {
                fallbackImagePickerLauncher.launch("image/*")
                true
            }.getOrDefault(false)
            if (!launchedContent) {
                val launchedPhotoPicker = runCatching {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                    true
                }.getOrDefault(false)
                if (!launchedPhotoPicker) {
                    viewModel.emitToast(
                        tr(
                            isArabic,
                            "تعذر فتح ملفات الجهاز لاختيار الصورة",
                            "Unable to open device files on this device"
                        )
                    )
                }
            }
        }
    }

    val launchSaveJsonBackupSaf: () -> Unit = {
        val fileName = com.example.util.ExportManager.buildSmartFileName(
            reportTitle = tr(isArabic, "نسخة احتياطية مصروفي", "Masrofy Backup"),
            extension = "json",
            isArabic = isArabic
        )
        val launched = runCatching {
            createBackupLauncher.launch(fileName)
            true
        }.getOrDefault(false)
        if (!launched) {
            // Seamless automatic fallback to direct device Downloads/Masrofy folder
            viewModel.shareJsonBackup()
        }
    }

    val launchSavePdfSaf: () -> Unit = {
        val fileName = com.example.util.ExportManager.buildSmartFileName(
            reportTitle = tr(isArabic, "تقرير مصروفي الكامل", "Masrofy Full Report"),
            extension = "pdf",
            isArabic = isArabic
        )
        val launched = runCatching {
            createPdfLauncher.launch(fileName)
            true
        }.getOrDefault(false)
        if (!launched) {
            viewModel.exportAllPdfDirect()
        }
    }

    val launchSaveExcelSaf: () -> Unit = {
        val fileName = com.example.util.ExportManager.buildSmartFileName(
            reportTitle = tr(isArabic, "سجل مصروفي الكامل", "Masrofy Full Ledger"),
            extension = "xls",
            isArabic = isArabic
        )
        val launched = runCatching {
            createExcelLauncher.launch(fileName)
            true
        }.getOrDefault(false)
        if (!launched) {
            viewModel.exportAllExcelDirect()
        }
    }

    val launchSaveImageSaf: () -> Unit = {
        val fileName = com.example.util.ExportManager.buildSmartFileName(
            reportTitle = tr(isArabic, "ملخص حساب مصروفي", "Masrofy Account Summary"),
            extension = "png",
            isArabic = isArabic
        )
        val launched = runCatching {
            createImageLauncher.launch(fileName)
            true
        }.getOrDefault(false)
        if (!launched) {
            viewModel.exportAllImageDirect()
        }
    }

    val launchRestoreBackupSaf: () -> Unit = {
        val launchedPrimary = runCatching {
            openBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            true
        }.getOrDefault(false)
        if (!launchedPrimary) {
            val launchedFallback = runCatching {
                openBackupLauncher.launch(arrayOf("*/*"))
                true
            }.getOrDefault(false)
            if (!launchedFallback) {
                viewModel.emitToast(
                    tr(
                        isArabic,
                        "تعذر فتح مدير الملفات على هذا الجهاز",
                        "Unable to open file picker on this device"
                    )
                )
            }
        }
    }

    // Route direct launches from Home Screen Icon or App Icon Long-Press Shortcuts
    LaunchedEffect(externalRoute) {
        if (externalRoute != null) {
            when (externalRoute) {
                AppTab.ADD.route -> {
                    addInitialType = TransactionType.EXPENSE
                    addInitialDebt = false
                    currentTabName = AppTab.ADD.name
                }
                AppTab.DEBTS.route -> {
                    previousTabName = AppTab.HOME.name
                    debtsInitialTab = "all"
                    currentTabName = AppTab.DEBTS.name
                }
                AppTab.PAYMENTS.route -> {
                    previousTabName = AppTab.HOME.name
                    paymentsInitialTab = "all"
                    currentTabName = AppTab.PAYMENTS.name
                }
                AppTab.OBLIGATIONS.route -> {
                    previousTabName = AppTab.HOME.name
                    currentTabName = AppTab.OBLIGATIONS.name
                }
                AppTab.REPORTS.route -> currentTabName = AppTab.REPORTS.name
                AppTab.LOG.route -> currentTabName = AppTab.LOG.name
                AppTab.SETTINGS.route -> currentTabName = AppTab.SETTINGS.name
                else -> currentTabName = AppTab.HOME.name
            }
            onExternalRouteConsumed()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val saveableStateHolder = rememberSaveableStateHolder()

    val isDetailScreen = currentTab == AppTab.DEBTS ||
        currentTab == AppTab.PAYMENTS ||
        currentTab == AppTab.OBLIGATIONS ||
        currentTab == AppTab.INCOME_DETAILS ||
        currentTab == AppTab.EXPENSE_DETAILS ||
        currentTab == AppTab.LIFETIME_MONEY ||
        currentTab == AppTab.MONEY_FLOW_GUIDE

    LaunchedEffect(viewModel) {
        viewModel.toastMessages.collect { msg ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

    val openFlowDrillDown: (FlowDrillDownSpec, AppTab) -> Unit = { spec, origin ->
        drillDownReturnTabName = origin.name
        currentTabName = AppTab.REPORTS.name
        activeDrillDownSpec = spec
    }

    val closeFlowDrillDown: () -> Unit = {
        val returnTab = drillDownReturnTabName
        activeDrillDownSpec = null
        drillDownReturnTabName = null
        if (!returnTab.isNullOrBlank()) {
            currentTabName = returnTab
        }
    }

    val navigateBackFromDetail: () -> Unit = {
        when {
            activeDrillDownSpec != null -> {
                closeFlowDrillDown()
            }
            currentTab == AppTab.LIFETIME_MONEY && previousTab == AppTab.MONEY_FLOW_GUIDE -> {
                previousTabName = guideOriginTabName
                currentTabName = AppTab.MONEY_FLOW_GUIDE.name
            }
            currentTab == AppTab.MONEY_FLOW_GUIDE -> {
                currentTabName = guideOriginTabName
            }
            isDetailScreen -> {
                currentTabName = previousTab.name
            }
            else -> {
                currentTabName = AppTab.HOME.name
            }
        }
    }

    // Back press returns to originating screen (Home or Reports) from detail screens, or Home from secondary tabs
    BackHandler(enabled = activeDrillDownSpec != null || currentTab != AppTab.HOME) {
        navigateBackFromDetail()
    }

    val screenTitle = activeDrillDownSpec?.title ?: when (currentTab) {
        AppTab.HOME -> tr(isArabic, "الرئيسية", "Home")
        AppTab.ADD -> tr(isArabic, "إضافة عملية", "Add Transaction")
        AppTab.LOG -> tr(isArabic, "سجل العمليات", "Transactions Log")
        AppTab.REPORTS -> tr(isArabic, "التقارير والتحليلات", "Reports & Analytics")
        AppTab.SETTINGS -> tr(isArabic, "الإعدادات", "Settings")
        AppTab.DEBTS -> tr(isArabic, "كل المبالغ المطلوب دفعها قريباً", "Upcoming Payments Due")
        AppTab.PAYMENTS -> tr(isArabic, "كل المبالغ اللي اندفعت بالفعل", "Actually Paid Amounts")
        AppTab.OBLIGATIONS -> tr(isArabic, "التزامات بمواعيد سداد", "Scheduled Payment Dues")
        AppTab.INCOME_DETAILS -> tr(isArabic, "تفاصيل الدخل", "Income Details")
        AppTab.EXPENSE_DETAILS -> tr(isArabic, "تفاصيل المصاريف", "Expenses Details")
        AppTab.LIFETIME_MONEY -> tr(isArabic, "الفلوس اللي دخلت لك من يوم ما استخدمت البرنامج", "Lifetime Money Added")
        AppTab.MONEY_FLOW_GUIDE -> tr(isArabic, "المخطط المالي", "Money Flow Plan")
    }

    val onSelectBottomTab: (AppTab) -> Unit = remember {
        { tab ->
            activeDrillDownSpec = null
            drillDownReturnTabName = null
            if (tab == AppTab.ADD) {
                addInitialType = TransactionType.EXPENSE
                addInitialDebt = false
            }
            currentTabName = tab.name
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MizanityTopBar(
                title = screenTitle,
                isDark = isDark,
                isArabic = isArabic,
                hasAlertDot = alertSummary.hasAlertDot,
                userName = settings.userName,
                userAvatarPath = settings.userAvatarPath,
                onNotificationsClick = { showNotificationsSheet = true },
                onToggleTheme = viewModel::toggleDarkMode,
                onToggleLanguage = viewModel::toggleLanguage,
                onProfileClick = {
                    activeDrillDownSpec = null
                    settingsTargetSectionName = SettingsSection.PERSONAL_INFO.name
                    currentTabName = AppTab.SETTINGS.name
                },
                onBackClick = if (activeDrillDownSpec != null || isDetailScreen) {
                    { navigateBackFromDetail() }
                } else null
            )
        },
        bottomBar = {
            MizanityBottomBar(
                currentTab = currentTab,
                isArabic = isArabic,
                onSelectTab = onSelectBottomTab
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(14.dp),
                    containerColor = ext.heroCardBackground,
                    contentColor = ext.heroCardOnBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val drillSpec = activeDrillDownSpec
            if (drillSpec != null) {
                FlowDrillDownScreen(
                    spec = drillSpec,
                    transactions = allTransactions,
                    obligations = allObligations,
                    settings = settings,
                    isArabic = isArabic,
                    onUpdateTransactionReminder = viewModel::updateTransactionReminder,
                    onBack = closeFlowDrillDown
                )
            } else saveableStateHolder.SaveableStateProvider(key = currentTab.route) {
                when (currentTab) {
                    AppTab.HOME -> HomeTabHost(
                        viewModel = viewModel,
                        settings = settings,
                        isArabic = isArabic,
                        onNavigate = { target ->
                            if (target == AppTab.MONEY_FLOW_GUIDE) {
                                reportsRequestedTabName = ReportsTopTab.PLAN.name
                                reportsTabNonce += 1
                                currentTabName = AppTab.REPORTS.name
                            } else {
                                if (target == AppTab.SETTINGS) {
                                    settingsTargetSectionName = SettingsSection.PERSONAL_INFO.name
                                }
                                if (target == AppTab.ADD) {
                                    addInitialType = TransactionType.EXPENSE
                                    addInitialDebt = false
                                }
                                if (target == AppTab.DEBTS || target == AppTab.PAYMENTS ||
                                    target == AppTab.OBLIGATIONS ||
                                    target == AppTab.INCOME_DETAILS || target == AppTab.EXPENSE_DETAILS ||
                                    target == AppTab.LIFETIME_MONEY
                                ) {
                                    previousTabName = AppTab.HOME.name
                                }
                                if (target == AppTab.DEBTS) debtsInitialTab = "all"
                                if (target == AppTab.PAYMENTS) paymentsInitialTab = "all"
                                currentTabName = target.name
                            }
                        },
                        onOpenDebtsTab = { debtTab ->
                            previousTabName = AppTab.HOME.name
                            debtsInitialTab = debtTab
                            currentTabName = AppTab.DEBTS.name
                        },
                        onOpenPaymentsTab = { payTab ->
                            previousTabName = AppTab.HOME.name
                            paymentsInitialTab = payTab
                            currentTabName = AppTab.PAYMENTS.name
                        },
                        onOpenFlowDrillDown = { spec ->
                            openFlowDrillDown(spec, AppTab.HOME)
                        },
                        onRequestNotificationPermission = requestNotificationPermissionIfNeeded
                    )

                    AppTab.ADD -> AddTabHost(
                        viewModel = viewModel,
                        settings = settings,
                        isArabic = isArabic,
                        initialType = addInitialType,
                        initialIsDebt = addInitialDebt,
                        onRequestNotificationPermission = requestNotificationPermissionIfNeeded
                    )

                    AppTab.LOG -> LogTabHost(
                        viewModel = viewModel,
                        settings = settings,
                        isArabic = isArabic,
                        onAddClick = {
                            addInitialType = TransactionType.EXPENSE
                            addInitialDebt = false
                            currentTabName = AppTab.ADD.name
                        }
                    )

                    AppTab.REPORTS -> ReportsTabHost(
                        viewModel = viewModel,
                        settings = settings,
                        isArabic = isArabic,
                        requestedTopTab = reportsRequestedTab,
                        requestedTopTabNonce = reportsTabNonce,
                        onViewFullLog = { currentTabName = AppTab.LOG.name },
                        onOpenIncomeDetails = {
                            previousTabName = AppTab.REPORTS.name
                            currentTabName = AppTab.INCOME_DETAILS.name
                        },
                        onOpenExpenseDetails = {
                            previousTabName = AppTab.REPORTS.name
                            currentTabName = AppTab.EXPENSE_DETAILS.name
                        },
                        onOpenDebts = {
                            previousTabName = AppTab.REPORTS.name
                            debtsInitialTab = "all"
                            currentTabName = AppTab.DEBTS.name
                        },
                        onOpenPayments = {
                            previousTabName = AppTab.REPORTS.name
                            paymentsInitialTab = "all"
                            currentTabName = AppTab.PAYMENTS.name
                        },
                        onOpenLifetimeMoney = {
                            previousTabName = AppTab.REPORTS.name
                            currentTabName = AppTab.LIFETIME_MONEY.name
                        },
                        onOpenObligations = {
                            previousTabName = AppTab.REPORTS.name
                            currentTabName = AppTab.OBLIGATIONS.name
                        },
                        onOpenMoneyFlowGuide = {
                            reportsRequestedTabName = ReportsTopTab.PLAN.name
                            reportsTabNonce += 1
                        },
                        onOpenDrillDown = { spec ->
                            openFlowDrillDown(spec, AppTab.REPORTS)
                        }
                    )

                    AppTab.SETTINGS -> SettingsTabHost(
                        viewModel = viewModel,
                        settings = settings,
                        initialSection = settingsTargetSection,
                        isArabic = isArabic,
                        signedInEmail = firebaseUser?.email,
                        onPickUserAvatar = launchPhotoPicker,
                        onOpenBackupSideDrawer = { mode ->
                            viewModel.setCloudBackupError(null)
                            backupDrawerModeName = mode.name
                        },
                        onLaunchSaveBackupSaf = launchSaveJsonBackupSaf,
                        onLaunchSavePdfSaf = launchSavePdfSaf,
                        onLaunchSaveExcelSaf = launchSaveExcelSaf,
                        onLaunchSaveImageSaf = launchSaveImageSaf,
                        onLaunchRestoreBackupSaf = launchRestoreBackupSaf
                    )

                    AppTab.DEBTS -> DebtsTabHost(
                        viewModel = viewModel,
                        settings = settings,
                        isArabic = isArabic,
                        initialDebtTab = debtsInitialTab,
                        onAddDebtClick = { presetType ->
                            addInitialType = presetType
                            addInitialDebt = true
                            currentTabName = AppTab.ADD.name
                        },
                        onOpenPayments = {
                            previousTabName = AppTab.DEBTS.name
                            paymentsInitialTab = "all"
                            currentTabName = AppTab.PAYMENTS.name
                        },
                        onOpenObligations = {
                            previousTabName = AppTab.DEBTS.name
                            currentTabName = AppTab.OBLIGATIONS.name
                        },
                        onRequestNotificationPermission = requestNotificationPermissionIfNeeded
                    )

                    AppTab.PAYMENTS -> PaymentsTabHost(
                        viewModel = viewModel,
                        settings = settings,
                        isArabic = isArabic,
                        initialPaymentTab = paymentsInitialTab,
                        onOpenDebts = {
                            debtsInitialTab = "all"
                            currentTabName = AppTab.DEBTS.name
                        }
                    )

                    AppTab.OBLIGATIONS -> ObligationsTabHost(
                        viewModel = viewModel,
                        settings = settings,
                        isArabic = isArabic,
                        onRequestNotificationPermission = requestNotificationPermissionIfNeeded
                    )

                    AppTab.INCOME_DETAILS -> FlowDetailsTabHost(
                        viewModel = viewModel,
                        settings = settings,
                        flowType = TransactionType.INCOME,
                        isArabic = isArabic,
                        onAddClick = {
                            addInitialType = TransactionType.INCOME
                            addInitialDebt = false
                            currentTabName = AppTab.ADD.name
                        }
                    )

                    AppTab.EXPENSE_DETAILS -> FlowDetailsTabHost(
                        viewModel = viewModel,
                        settings = settings,
                        flowType = TransactionType.EXPENSE,
                        isArabic = isArabic,
                        onAddClick = {
                            addInitialType = TransactionType.EXPENSE
                            addInitialDebt = false
                            currentTabName = AppTab.ADD.name
                        }
                    )

                    AppTab.LIFETIME_MONEY -> LifetimeMoneyTabHost(
                        viewModel = viewModel,
                        settings = settings,
                        isArabic = isArabic,
                        onAddMoneyClick = {
                            addInitialType = TransactionType.INCOME
                            addInitialDebt = false
                            currentTabName = AppTab.ADD.name
                        }
                    )

                    AppTab.MONEY_FLOW_GUIDE -> MoneyFlowGuideTabHost(
                        viewModel = viewModel,
                        settings = settings,
                        isArabic = isArabic,
                        onOpenLifetimeMoney = {
                            previousTabName = AppTab.MONEY_FLOW_GUIDE.name
                            currentTabName = AppTab.LIFETIME_MONEY.name
                        },
                        onOpenExpenseDetails = {
                            previousTabName = AppTab.MONEY_FLOW_GUIDE.name
                            currentTabName = AppTab.EXPENSE_DETAILS.name
                        },
                        onOpenObligations = {
                            previousTabName = AppTab.MONEY_FLOW_GUIDE.name
                            currentTabName = AppTab.OBLIGATIONS.name
                        },
                        onOpenDrillDown = { spec ->
                            openFlowDrillDown(spec, AppTab.MONEY_FLOW_GUIDE)
                        }
                    )
                }
            }
        }
    }

    if (showNotificationsSheet) {
        NotificationCenterSheet(
            todayCount = alertSummary.todayCount,
            monthExpenseMinor = alertSummary.monthExpenseMinor,
            settings = settings,
            transactions = allTransactions,
            obligations = allObligations,
            isArabic = isArabic,
            hasUnreadAlerts = alertSummary.hasAlertDot,
            onMarkAllAsRead = viewModel::markAllNotificationsRead,
            onToggleNotifications = viewModel::setNotificationsEnabled,
            onUpdateAlertDefaults = viewModel::updateDefaultAlertPreferences,
            onUpdateDailyReminderConfig = viewModel::updateDailyExpenseReminderConfig,
            onClearTransactionDueDates = viewModel::clearAllExpenseTransactionDueDates,
            onUnifyScheduledReminderTime = viewModel::unifyScheduledReminderTime,
            onUpdateTransactionReminder = viewModel::updateTransactionReminder,
            onUpdateObligationReminder = viewModel::updateObligationReminder,
            onRequestNotificationPermission = requestNotificationPermissionIfNeeded,
            onOpenObligations = {
                activeDrillDownSpec = null
                previousTabName = currentTab.name
                currentTabName = AppTab.OBLIGATIONS.name
            },
            onOpenSettings = {
                activeDrillDownSpec = null
                settingsTargetSectionName = SettingsSection.NOTIFICATIONS.name
                currentTabName = AppTab.SETTINGS.name
            },
            onDismiss = { showNotificationsSheet = false }
        )
    }

    val startGoogleChooser: () -> Unit = {
        isGoogleAuthBusy = true
        viewModel.setCloudBackupError(null)
        signInWithGoogleAccountChooser(
            context = context,
            credentialManager = credentialManager,
            scope = coroutineScope,
            isArabic = isArabic,
            onSuccess = { user ->
                isGoogleAuthBusy = false
                viewModel.emitToast(
                    tr(
                        isArabic,
                        "تم تسجيل الدخول بالإيميل: ${user.email ?: ""}",
                        "Signed in as: ${user.email ?: ""}"
                    )
                )
                viewModel.loadCloudBackupSummary()
            },
            onCancelled = {
                isGoogleAuthBusy = false
            },
            onError = { errMsg ->
                isGoogleAuthBusy = false
                viewModel.setCloudBackupError(errMsg)
            }
        )
    }

    BackupSideDrawerOverlay(
        mode = backupDrawerMode,
        isArabic = isArabic,
        signedInEmail = firebaseUser?.email,
        signedInName = firebaseUser?.displayName,
        isAuthBusy = isGoogleAuthBusy,
        isCloudBusy = isCloudBackupBusy,
        cloudSummary = cloudBackupSummary,
        cloudError = cloudBackupError,
        localTransactionCount = allTransactions.size,
        onSwitchMode = { nextMode -> backupDrawerModeName = nextMode.name },
        onChooseGoogleEmailSignIn = startGoogleChooser,
        onSwitchGoogleEmail = {
            isGoogleAuthBusy = true
            viewModel.clearCloudBackupState()
            signOutGoogleAccount(
                credentialManager = credentialManager,
                scope = coroutineScope,
                onComplete = { startGoogleChooser() }
            )
        },
        onSignOutGoogle = {
            isGoogleAuthBusy = true
            viewModel.clearCloudBackupState()
            signOutGoogleAccount(
                credentialManager = credentialManager,
                scope = coroutineScope,
                onComplete = {
                    isGoogleAuthBusy = false
                    viewModel.emitToast(
                        tr(isArabic, "تم تسجيل الخروج من الحساب السحابي", "Signed out from cloud account")
                    )
                }
            )
        },
        onRefreshCloudSummary = viewModel::loadCloudBackupSummary,
        onSaveCloudBackup = {
            val email = firebaseUser?.email.orEmpty()
            viewModel.saveCloudBackupToSignedInEmail(email)
        },
        onRestoreCloudBackup = viewModel::restoreCloudBackupFromSignedInEmail,
        onSaveJsonDeviceFiles = launchSaveJsonBackupSaf,
        onRestoreJsonDeviceFiles = launchRestoreBackupSaf,
        onDownloadJsonDirect = viewModel::shareJsonBackup,
        onSavePdfDeviceFiles = launchSavePdfSaf,
        onSaveExcelDeviceFiles = launchSaveExcelSaf,
        onSaveImageDeviceFiles = launchSaveImageSaf,
        onDismiss = { backupDrawerModeName = null }
    )
}

@Composable
private fun HomeTabHost(
    viewModel: MizanityViewModel,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    onNavigate: (AppTab) -> Unit,
    onOpenDebtsTab: (String) -> Unit,
    onOpenPaymentsTab: (String) -> Unit,
    onOpenFlowDrillDown: (FlowDrillDownSpec) -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    val homeAnalytics by viewModel.homeAnalytics.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val obligations by viewModel.obligations.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val showWelcomeBanner by viewModel.showWelcomeBanner.collectAsStateWithLifecycle()

    HomeScreen(
        analytics = homeAnalytics,
        transactions = transactions,
        categories = categories,
        settings = settings,
        pendingObligationsCount = remember(obligations) { obligations.count { !it.isPaid } },
        showWelcomeBanner = showWelcomeBanner,
        isArabic = isArabic,
        onDismissWelcomeBanner = viewModel::dismissWelcomeBanner,
        onNavigate = onNavigate,
        onOpenDebtsTab = onOpenDebtsTab,
        onOpenPaymentsTab = onOpenPaymentsTab,
        onToggleDebtSettled = viewModel::setDebtSettled,
        onOpenFlowDrillDown = onOpenFlowDrillDown,
        onAddObligation = viewModel::addObligation,
        onRequestNotificationPermission = onRequestNotificationPermission
    )
}

@Composable
private fun AddTabHost(
    viewModel: MizanityViewModel,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    initialType: String,
    initialIsDebt: Boolean,
    onRequestNotificationPermission: () -> Unit
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    AddTransactionScreen(
        categories = categories,
        accounts = accounts,
        defaultCurrency = settings.defaultCurrency,
        lastSelectedSource = settings.lastSelectedSource,
        isArabic = isArabic,
        settings = settings,
        initialType = initialType,
        initialIsDebt = initialIsDebt,
        onRequestNotificationPermission = onRequestNotificationPermission,
        onSaveTransaction = viewModel::addTransaction
    )
}

@Composable
private fun LogTabHost(
    viewModel: MizanityViewModel,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    onAddClick: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    LogScreen(
        transactions = transactions,
        categories = categories,
        settings = settings,
        isArabic = isArabic,
        onAddClick = onAddClick,
        onDeleteSingle = viewModel::deleteTransaction,
        onDeleteBatch = viewModel::deleteTransactions,
        onDeleteWeek = viewModel::deleteCurrentWeekTransactions,
        onDeleteMonth = viewModel::deleteCurrentMonthTransactions,
        onDeleteByCategory = viewModel::deleteTransactionsByCategory,
        onWipeAll = viewModel::wipeAllTransactions,
        onExportExcel = viewModel::exportTransactionsExcel,
        onExportCsv = viewModel::exportTransactionsCsv,
        onToggleDebtSettled = viewModel::setDebtSettled
    )
}

@Composable
private fun ReportsTabHost(
    viewModel: MizanityViewModel,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    requestedTopTab: ReportsTopTab,
    requestedTopTabNonce: Int,
    onViewFullLog: () -> Unit,
    onOpenIncomeDetails: () -> Unit,
    onOpenExpenseDetails: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenPayments: () -> Unit,
    onOpenLifetimeMoney: () -> Unit,
    onOpenObligations: () -> Unit,
    onOpenMoneyFlowGuide: () -> Unit,
    onOpenDrillDown: (FlowDrillDownSpec) -> Unit
) {
    val reportAnalytics by viewModel.reportAnalytics.collectAsStateWithLifecycle()
    val reportFilter by viewModel.reportFilter.collectAsStateWithLifecycle()
    val allTransactions by viewModel.transactions.collectAsStateWithLifecycle()
    val obligations by viewModel.obligations.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val savedFilters by viewModel.savedFilters.collectAsStateWithLifecycle()

    ReportsScreen(
        analytics = reportAnalytics,
        filter = reportFilter,
        categories = categories,
        accounts = accounts,
        savedFilters = savedFilters,
        settings = settings,
        isArabic = isArabic,
        allTransactions = allTransactions,
        obligations = obligations,
        requestedTopTab = requestedTopTab,
        requestedTopTabNonce = requestedTopTabNonce,
        onUpdateFilter = viewModel::updateReportFilter,
        onResetFilter = viewModel::resetReportFilter,
        onSaveFilter = viewModel::saveCurrentFilter,
        onApplySavedFilter = viewModel::applySavedFilter,
        onDeleteSavedFilter = viewModel::deleteSavedFilter,
        onExportPdf = viewModel::exportFilteredPdf,
        onExportImage = viewModel::exportFilteredImage,
        onExportExcel = viewModel::exportFilteredExcel,
        onDeleteTransaction = viewModel::deleteTransaction,
        onToggleDebtSettled = viewModel::setDebtSettled,
        onSetObligationPaid = viewModel::setObligationPaid,
        onViewFullLog = onViewFullLog,
        onOpenIncomeDetails = onOpenIncomeDetails,
        onOpenExpenseDetails = onOpenExpenseDetails,
        onOpenDebts = onOpenDebts,
        onOpenPayments = onOpenPayments,
        onOpenLifetimeMoney = onOpenLifetimeMoney,
        onOpenObligations = onOpenObligations,
        onOpenMoneyFlowGuide = onOpenMoneyFlowGuide,
        onOpenDrillDown = onOpenDrillDown
    )
}

@Composable
private fun SettingsTabHost(
    viewModel: MizanityViewModel,
    settings: AppSettingsEntity,
    initialSection: SettingsSection,
    isArabic: Boolean,
    signedInEmail: String?,
    onPickUserAvatar: () -> Unit,
    onOpenBackupSideDrawer: (BackupDrawerMode) -> Unit,
    onLaunchSaveBackupSaf: () -> Unit,
    onLaunchSavePdfSaf: () -> Unit,
    onLaunchSaveExcelSaf: () -> Unit,
    onLaunchSaveImageSaf: () -> Unit,
    onLaunchRestoreBackupSaf: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val obligations by viewModel.obligations.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    SettingsScreen(
        settings = settings,
        transactions = transactions,
        obligations = obligations,
        categories = categories,
        accounts = accounts,
        initialSection = initialSection,
        isArabic = isArabic,
        signedInEmail = signedInEmail,
        onSetQuickDelete = viewModel::setQuickDeleteMode,
        onDeleteCurrentWeek = viewModel::deleteCurrentWeekTransactions,
        onDeleteCurrentMonth = viewModel::deleteCurrentMonthTransactions,
        onDeleteOlderThanMonths = viewModel::deleteTransactionsOlderThanMonths,
        onDeleteByCategory = viewModel::deleteTransactionsByCategory,
        onDeleteBySource = viewModel::deleteTransactionsBySource,
        onDeleteSingleTransaction = viewModel::deleteTransaction,
        onDeleteSingleObligation = viewModel::deleteObligation,
        onWipeAll = viewModel::wipeAllTransactions,
        onResetLifetimeIncome = viewModel::resetLifetimeIncomeNumbers,
        onResetEarnedIncomeOnly = viewModel::resetEarnedIncomeOnlyNumbers,
        onResetUnpaidIncomeDebts = viewModel::resetUnpaidIncomeDebtNumbers,
        onResetSettledIncomeDebts = viewModel::resetSettledIncomeDebtNumbers,
        onResetExpenses = viewModel::resetExpenseNumbers,
        onResetPaidExpensesOnly = viewModel::resetPaidExpensesOnlyNumbers,
        onResetUnpaidExpenseDebts = viewModel::resetUnpaidExpenseDebtNumbers,
        onResetSettledExpenseDebts = viewModel::resetSettledExpenseDebtNumbers,
        onResetUnpaidDebts = viewModel::resetUnpaidDebtNumbers,
        onResetSettledPayments = viewModel::resetSettledPaymentNumbers,
        onResetObligations = viewModel::resetObligationsNumbers,
        onResetPendingObligations = viewModel::resetPendingObligationsNumbers,
        onResetPaidObligations = viewModel::resetPaidObligationsNumbers,
        onResetMonthlyBudget = viewModel::resetMonthlyBudgetLimit,
        onResetAllAppNumbers = viewModel::resetAllAppNumbers,
        onUpdateAnalyticsConfig = viewModel::updateAnalyticsDashboardConfig,
        onSetPinProtection = viewModel::setPinProtection,
        onSetBiometricProtection = viewModel::setBiometricProtection,
        onSetPatternProtection = viewModel::setPatternProtection,
        onSetConfirmPaymentWithSecurity = viewModel::setConfirmPaymentWithSecurity,
        onSetAutoLock = viewModel::setAutoLockSensitive,
        onPinHomeWidget = viewModel::requestPinHomeWidget,
        onSetDefaultCurrency = viewModel::setDefaultCurrency,
        onAddAccount = viewModel::addAccount,
        onDeleteAccount = viewModel::deleteAccount,
        onSetThemeMode = viewModel::setThemeMode,
        onSetLanguage = viewModel::setLanguage,
        onSetReminders = viewModel::setReminderSettings,
        onAddCategory = viewModel::addCategory,
        onDeleteCategory = viewModel::deleteCategory,
        onExportExcelAll = { viewModel.exportTransactionsExcel(transactions) },
        onShareJsonBackup = viewModel::shareJsonBackup,
        onOpenBackupSideDrawer = onOpenBackupSideDrawer,
        onLaunchSaveBackupSaf = onLaunchSaveBackupSaf,
        onLaunchSavePdfSaf = onLaunchSavePdfSaf,
        onLaunchSaveExcelSaf = onLaunchSaveExcelSaf,
        onLaunchSaveImageSaf = onLaunchSaveImageSaf,
        onLaunchRestoreBackupSaf = onLaunchRestoreBackupSaf,
        onUpdateUserName = viewModel::updateUserName,
        onUpdateGreetingPreferences = viewModel::updateGreetingPreferences,
        onSetGreetingVisibility = viewModel::setGreetingVisibility,
        onPickUserAvatar = onPickUserAvatar,
        onRemoveUserAvatar = viewModel::removeUserAvatar,
        onEmitMessage = viewModel::emitToast
    )
}

@Composable
private fun DebtsTabHost(
    viewModel: MizanityViewModel,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    initialDebtTab: String,
    onAddDebtClick: (String) -> Unit,
    onOpenPayments: () -> Unit,
    onOpenObligations: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val obligations by viewModel.obligations.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    DebtsScreen(
        transactions = transactions,
        categories = categories,
        settings = settings,
        isArabic = isArabic,
        obligations = obligations,
        initialDebtTab = initialDebtTab,
        onAddDebtClick = onAddDebtClick,
        onOpenPayments = onOpenPayments,
        onOpenObligations = onOpenObligations,
        onToggleSettled = viewModel::setDebtSettled,
        onSetObligationPaid = viewModel::setObligationPaid,
        onUpdateTransactionReminder = viewModel::updateTransactionReminder,
        onUpdateObligationReminder = viewModel::updateObligationReminder,
        onRequestNotificationPermission = onRequestNotificationPermission,
        onDeleteTransaction = viewModel::deleteTransaction
    )
}

@Composable
private fun PaymentsTabHost(
    viewModel: MizanityViewModel,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    initialPaymentTab: String,
    onOpenDebts: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val obligations by viewModel.obligations.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    PaymentsScreen(
        transactions = transactions,
        categories = categories,
        settings = settings,
        isArabic = isArabic,
        obligations = obligations,
        initialTab = initialPaymentTab,
        onOpenDebts = onOpenDebts,
        onReopenAsDebt = { id -> viewModel.setDebtSettled(id, false) },
        onSetObligationPaid = viewModel::setObligationPaid,
        onDeleteTransaction = viewModel::deleteTransaction
    )
}

@Composable
private fun FlowDetailsTabHost(
    viewModel: MizanityViewModel,
    settings: AppSettingsEntity,
    flowType: String,
    isArabic: Boolean,
    onAddClick: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    FlowDetailsScreen(
        flowType = flowType,
        transactions = transactions,
        categories = categories,
        settings = settings,
        isArabic = isArabic,
        onAddClick = { onAddClick() },
        onToggleDebtSettled = viewModel::setDebtSettled,
        onDeleteTransaction = viewModel::deleteTransaction
    )
}

@Composable
private fun ObligationsTabHost(
    viewModel: MizanityViewModel,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    onRequestNotificationPermission: () -> Unit
) {
    val obligations by viewModel.obligations.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val homeAnalytics by viewModel.homeAnalytics.collectAsStateWithLifecycle()

    ObligationsScreen(
        obligations = obligations,
        transactions = transactions,
        accounts = accounts,
        analytics = homeAnalytics,
        settings = settings,
        isArabic = isArabic,
        onAddObligation = viewModel::addObligation,
        onSetObligationPaid = viewModel::setObligationPaid,
        onSetObligationReminderMuted = viewModel::setObligationReminderMuted,
        onUpdateObligationReminder = viewModel::updateObligationReminder,
        onUpdateTransactionReminder = viewModel::updateTransactionReminder,
        onSetTransactionDebtSettled = viewModel::setDebtSettled,
        onDeleteObligation = viewModel::deleteObligation,
        onRequestNotificationPermission = onRequestNotificationPermission
    )
}

@Composable
private fun LifetimeMoneyTabHost(
    viewModel: MizanityViewModel,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    onAddMoneyClick: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    LifetimeMoneyScreen(
        transactions = transactions,
        settings = settings,
        isArabic = isArabic,
        onAddMoneyClick = onAddMoneyClick,
        onDeleteTransaction = viewModel::deleteTransaction
    )
}

@Composable
private fun MoneyFlowGuideTabHost(
    viewModel: MizanityViewModel,
    settings: AppSettingsEntity,
    isArabic: Boolean,
    onOpenLifetimeMoney: () -> Unit,
    onOpenExpenseDetails: () -> Unit,
    onOpenObligations: () -> Unit,
    onOpenDrillDown: (FlowDrillDownSpec) -> Unit
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val obligations by viewModel.obligations.collectAsStateWithLifecycle()

    MoneyFlowGuideScreen(
        transactions = transactions,
        obligations = obligations,
        settings = settings,
        isArabic = isArabic,
        onOpenLifetimeMoney = onOpenLifetimeMoney,
        onOpenExpenseDetails = onOpenExpenseDetails,
        onOpenObligations = onOpenObligations,
        onSetObligationPaid = viewModel::setObligationPaid,
        onToggleDebtSettled = viewModel::setDebtSettled,
        onOpenDrillDown = onOpenDrillDown
    )
}
