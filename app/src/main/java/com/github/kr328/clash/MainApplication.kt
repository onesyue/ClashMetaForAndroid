package com.github.kr328.clash

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.common.compat.currentProcessName
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.remote.Remote
import com.github.kr328.clash.service.util.sendServiceRecreated
import com.github.kr328.clash.util.OfflineCache
import com.github.kr328.clash.xboard.XBoardSession
import com.github.kr328.clash.design.R as DesignR


@Suppress("unused")
class MainApplication : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        Global.init(this)
    }

    override fun onCreate() {
        super.onCreate()

        val processName = currentProcessName

        Log.d("Process $processName started")

        if (processName == packageName) {
            // Migrate plaintext tokens to encrypted storage (one-time)
            XBoardSession.migrateIfNeeded(this)
            OfflineCache.migrateIfNeeded(this)

            Remote.launch()
            setupShortcuts()
            createNotificationChannels()
            com.github.kr328.clash.util.SubscriptionCheckWorker.schedule(this)
        } else {
            sendServiceRecreated()
        }
    }

    private fun setupShortcuts() {
        val icon = IconCompat.createWithResource(this, R.mipmap.ic_launcher)
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
            Intent.FLAG_ACTIVITY_NO_ANIMATION

        val toggle = ShortcutInfoCompat.Builder(this, "toggle_clash")
            .setShortLabel(getString(DesignR.string.shortcut_toggle_short))
            .setLongLabel(getString(DesignR.string.shortcut_toggle_long))
            .setIcon(icon)
            .setIntent(
                Intent(Intents.ACTION_TOGGLE_CLASH)
                    .setClassName(this, ExternalControlActivity::class.java.name)
                    .addFlags(flags)
            )
            .setRank(0)
            .build()

        val start = ShortcutInfoCompat.Builder(this, "start_clash")
            .setShortLabel(getString(DesignR.string.shortcut_start_short))
            .setLongLabel(getString(DesignR.string.shortcut_start_long))
            .setIcon(icon)
            .setIntent(
                Intent(Intents.ACTION_START_CLASH)
                    .setClassName(this, ExternalControlActivity::class.java.name)
                    .addFlags(flags)
            )
            .setRank(1)
            .build()

        val stop = ShortcutInfoCompat.Builder(this, "stop_clash")
            .setShortLabel(getString(DesignR.string.shortcut_stop_short))
            .setLongLabel(getString(DesignR.string.shortcut_stop_long))
            .setIcon(icon)
            .setIntent(
                Intent(Intents.ACTION_STOP_CLASH)
                    .setClassName(this, ExternalControlActivity::class.java.name)
                    .addFlags(flags)
            )
            .setRank(2)
            .build()

        ShortcutManagerCompat.setDynamicShortcuts(this, listOf(toggle, start, stop))
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return

        val channels = listOf(
            NotificationChannel(
                "yt_subscription",
                getString(DesignR.string.channel_subscription),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = getString(DesignR.string.channel_subscription_desc) },
            NotificationChannel(
                "yt_payment",
                getString(DesignR.string.channel_payment),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = getString(DesignR.string.channel_payment_desc) },
            NotificationChannel(
                "yt_announcement",
                getString(DesignR.string.channel_announcement),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = getString(DesignR.string.channel_announcement_desc) }
        )
        manager.createNotificationChannels(channels)
    }

    fun finalize() {
        Global.destroy()
    }
}
