package com.github.kr328.clash.util

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.github.kr328.clash.MainActivity
import android.app.PendingIntent
import android.content.Intent
import com.github.kr328.clash.design.R as DesignR
import com.github.kr328.clash.service.R as ServiceR

object SubscriptionChecker {

    private const val PREFS_NAME = "yt_subscription_alerts"
    private const val KEY_LAST_ALERT_LEVEL = "last_alert_level"
    private const val NOTIFICATION_ID = 9001
    private const val CHANNEL_ID = "yt_subscription"

    /**
     * Compute the alert level for a given number of days left.
     * Returns null if no alert is needed, otherwise 0/1/3/7.
     */
    internal fun computeAlertLevel(daysLeft: Int): Int? = when {
        daysLeft < 0 -> 0   // expired
        daysLeft <= 1 -> 1  // 1 day
        daysLeft <= 3 -> 3  // 3 days
        daysLeft <= 7 -> 7  // 7 days
        else -> null         // no alert needed
    }

    /**
     * Check subscription expiry and send notification if needed.
     * @param expiredAt Unix timestamp (seconds) of expiry, null = permanent
     */
    fun check(context: Context, expiredAt: Long?) {
        if (expiredAt == null || expiredAt == 0L) return

        val now = System.currentTimeMillis() / 1000
        val daysLeft = ((expiredAt - now) / 86400).toInt()

        val alertLevel = computeAlertLevel(daysLeft) ?: return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastAlert = prefs.getInt(KEY_LAST_ALERT_LEVEL, -1)

        // Only notify if alert level changed (more urgent)
        if (lastAlert == alertLevel) return

        prefs.edit().putInt(KEY_LAST_ALERT_LEVEL, alertLevel).apply()

        val message = when (alertLevel) {
            0 -> context.getString(DesignR.string.subscription_expired_alert)
            1 -> context.getString(DesignR.string.subscription_expiring_1day)
            3 -> context.getString(DesignR.string.subscription_expiring_3days)
            7 -> context.getString(DesignR.string.subscription_expiring_7days)
            else -> return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(ServiceR.drawable.ic_logo_service)
            .setContentTitle(context.getString(DesignR.string.subscription_alert_title))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Reset alert level (e.g. after user renews subscription).
     */
    fun reset(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_LAST_ALERT_LEVEL).apply()
    }
}
