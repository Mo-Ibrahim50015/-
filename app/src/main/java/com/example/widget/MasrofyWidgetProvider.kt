package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R

/**
 * Lightweight 1x1 Icon-Only Home Screen Widget & Pinned App Icon Shortcut («أيقونة مصروفي فقط»).
 * Performs ZERO database queries and ZERO blocking IPC on transactions so it never causes
 * freezes or crashes on any Android 8–16 launcher.
 */
class MasrofyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        runCatching {
            updateIconWidgets(context.applicationContext, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        const val EXTRA_INITIAL_TAB = "extra_initial_tab"

        /**
         * Requests the phone launcher to pin a native 1x1 Masrofy app icon directly onto
         * the Home Screen (no large dashboard card). Tries ShortcutManagerCompat pinned shortcut
         * first, then falls back to the 1x1 icon-only AppWidget on launchers that prefer widgets.
         */
        fun requestPinWidget(context: Context): Boolean {
            val appCtx = context.applicationContext
            // 1. Prefer native 1x1 Home Screen Shortcut Icon (pure app icon + name)
            val shortcutPinned = runCatching {
                if (ShortcutManagerCompat.isRequestPinShortcutSupported(appCtx)) {
                    val launchIntent = Intent(appCtx, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(EXTRA_INITIAL_TAB, "home")
                    }
                    val shortcut = ShortcutInfoCompat.Builder(appCtx, "masrofy_home_icon_v2")
                        .setShortLabel(appCtx.getString(R.string.app_name))
                        .setLongLabel(appCtx.getString(R.string.app_name))
                        .setIcon(IconCompat.createWithResource(appCtx, R.mipmap.ic_launcher_round))
                        .setIntent(launchIntent)
                        .build()
                    ShortcutManagerCompat.requestPinShortcut(appCtx, shortcut, null)
                } else {
                    false
                }
            }.getOrDefault(false)
            if (shortcutPinned) return true

            // 2. Fallback: pin the 1x1 icon-only AppWidget on Android 8+
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            return runCatching {
                val appWidgetManager = AppWidgetManager.getInstance(appCtx)
                if (!appWidgetManager.isRequestPinAppWidgetSupported) return@runCatching false
                val provider = ComponentName(appCtx, MasrofyWidgetProvider::class.java)
                appWidgetManager.requestPinAppWidget(provider, null, null)
            }.getOrDefault(false)
        }

        internal fun updateIconWidgets(
            context: Context,
            manager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val pendingIntent = launchPendingIntent(context, "home", 101)
            val appTitle = context.getString(R.string.app_name)
            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_masrofy)
                views.setTextViewText(R.id.widget_title, appTitle)
                views.setImageViewResource(R.id.widget_icon, R.mipmap.ic_launcher_round)
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                manager.updateAppWidget(widgetId, views)
            }
        }

        private fun launchPendingIntent(context: Context, tabRoute: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_INITIAL_TAB, tabRoute)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            return PendingIntent.getActivity(context, requestCode, intent, flags)
        }
    }
}
