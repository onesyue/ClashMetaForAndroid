package com.github.kr328.clash

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.remote.StatusClient
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import com.github.kr328.clash.design.R as DesignR

class VpnLargeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val profile = StatusClient(context).currentProfile()
        val running = profile != null

        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId, running, profile)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_TOGGLE -> {
                val profile = StatusClient(context).currentProfile()
                if (profile != null) {
                    context.stopClashService()
                } else {
                    context.startClashService()
                }
            }
            Intents.ACTION_CLASH_STARTED,
            Intents.ACTION_CLASH_STOPPED,
            Intents.ACTION_SERVICE_RECREATED,
            Intents.ACTION_PROFILE_LOADED -> {
                refreshAllWidgets(context)
            }
        }
    }

    companion object {
        private const val ACTION_TOGGLE = "com.github.kr328.clash.action.LARGE_WIDGET_TOGGLE"

        fun refreshAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, VpnLargeWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)

            val profile = StatusClient(context).currentProfile()
            val running = profile != null

            for (id in ids) {
                updateWidget(context, manager, id, running, profile)
            }
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            running: Boolean,
            profileName: String?
        ) {
            val views = RemoteViews(context.packageName, DesignR.layout.widget_vpn_large)

            views.setTextViewText(
                DesignR.id.widget_status,
                context.getString(
                    if (running) DesignR.string.status_connected
                    else DesignR.string.status_disconnected
                )
            )

            views.setTextViewText(
                DesignR.id.widget_profile,
                if (running && !profileName.isNullOrEmpty()) profileName
                else context.getString(DesignR.string.tap_to_start)
            )

            views.setTextViewText(DesignR.id.widget_download_speed, "0 B/s")
            views.setTextViewText(DesignR.id.widget_upload_speed, "0 B/s")

            // Toggle button
            val toggleIntent = Intent(context, VpnLargeWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE
            }
            val togglePending = PendingIntent.getBroadcast(
                context, 200, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(DesignR.id.widget_toggle, togglePending)

            // Root click opens app
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context, 201, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(DesignR.id.widget_root, openPending)

            manager.updateAppWidget(widgetId, views)
        }
    }
}
