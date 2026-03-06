package com.github.kr328.clash

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.FetchStatus
import com.github.kr328.clash.design.MainDesign
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.service.remote.IFetchObserver
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.util.withProfile
import com.github.kr328.clash.xboard.XBoardApi
import com.github.kr328.clash.xboard.XBoardSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : BaseActivity<MainDesign>() {

    private var connectStartMs: Long = 0L
    private var lastTrafficRaw: Long = -1L

    override suspend fun main() {
        // 强制登录：未登录则跳登录页，返回后仍无 auth → 退出
        if (XBoardSession.getAuthData(this) == null) {
            startActivityForResult(
                ActivityResultContracts.StartActivityForResult(),
                XBoardLoginActivity::class.intent
            )
            if (XBoardSession.getAuthData(this) == null) return
        }

        val design = MainDesign(this)
        setContentDesign(design)

        design.fetch()
        // Apply fixed geo settings silently on every start (CDN URLs accessible in China)
        launch { applyGeoDefaults() }
        // fetchUserData 放后台，不阻塞事件循环启动（避免 API 延迟导致按钮无响应）
        launch { design.fetchUserData() }

        val ticker = ticker(TimeUnit.SECONDS.toMillis(1))

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ActivityStart,
                        Event.ServiceRecreated,
                        Event.ClashStop,
                        Event.ClashStart,
                        Event.ProfileLoaded,
                        Event.ProfileChanged -> {
                            if (it == Event.ClashStart) connectStartMs = System.currentTimeMillis()
                            if (it == Event.ClashStop) { connectStartMs = 0L; lastTrafficRaw = -1L }
                            design.fetch()
                        }
                        else -> Unit
                    }
                }
                design.requests.onReceive {
                    when (it) {
                        MainDesign.Request.ToggleStatus -> {
                            if (clashRunning) {
                                stopClashService()
                            } else {
                                design.startClash()
                            }
                        }
                        MainDesign.Request.OpenProxy ->
                            startActivity(ProxyActivity::class.intent)
                        MainDesign.Request.OpenAccount -> {
                            startActivityForResult(
                                ActivityResultContracts.StartActivityForResult(),
                                AccountActivity::class.intent
                            )
                            design.fetch()
                            design.fetchUserData()
                        }
                        MainDesign.Request.OpenProfiles ->
                            startActivity(ProfilesActivity::class.intent)
                        MainDesign.Request.OpenStore ->
                            startActivity(StoreActivity::class.intent)
                        MainDesign.Request.OpenLogs -> {
                            if (LogcatService.running) {
                                startActivity(LogcatActivity::class.intent)
                            } else {
                                startActivity(LogsActivity::class.intent)
                            }
                        }
                        MainDesign.Request.OpenSettings ->
                            startActivity(SettingsActivity::class.intent)
                        MainDesign.Request.OpenAbout ->
                            design.showAbout(queryAppVersionName())
                        MainDesign.Request.OpenNotices ->
                            startActivity(NoticesActivity::class.intent)
                        MainDesign.Request.OpenOrders ->
                            startActivity(OrdersActivity::class.intent)
                        MainDesign.Request.ChangePassword -> {
                            val pending = design.pendingPasswordChange ?: return@onReceive
                            design.pendingPasswordChange = null
                            val authData = XBoardSession.getAuthData(this@MainActivity) ?: return@onReceive
                            val baseUrl = XBoardSession.getBaseUrl(this@MainActivity)
                            launch(Dispatchers.IO) {
                                try {
                                    XBoardApi.changePassword(baseUrl, authData, pending.first, pending.second)
                                    design.showToast(
                                        getString(R.string.password_changed),
                                        ToastDuration.Short
                                    )
                                } catch (e: Exception) {
                                    design.showToast(
                                        e.message ?: getString(R.string.xboard_request_failed),
                                        ToastDuration.Long
                                    )
                                }
                            }
                        }
                        MainDesign.Request.Logout -> {
                            val authData = XBoardSession.getAuthData(this@MainActivity)
                            val baseUrl = XBoardSession.getBaseUrl(this@MainActivity)
                            if (authData != null) {
                                launch(Dispatchers.IO) {
                                    XBoardApi.logout(baseUrl, authData)
                                }
                            }
                            XBoardSession.clear(this@MainActivity)
                            design.resetUserData()
                            startActivity(
                                XBoardLoginActivity::class.intent.apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                            )
                        }
                    }
                }
                if (clashRunning) {
                    ticker.onReceive {
                        design.fetchTraffic()
                        if (connectStartMs > 0L) {
                            design.setConnectionTime(formatDuration(System.currentTimeMillis() - connectStartMs))
                        }
                    }
                }
            }
        }
    }

    private suspend fun MainDesign.fetch() {
        setClashRunning(clashRunning)

        val state = withClash { queryTunnelState() }
        setMode(state.mode)

        withProfile {
            val active = queryActive()
            setProfileName(active?.name)
        }
    }

    private suspend fun MainDesign.fetchTraffic() {
        val current = withClash { queryTrafficTotal() }

        val curUpBytes = decodeTrafficHalf(current ushr 32)
        val curDlBytes = decodeTrafficHalf(current and 0xFFFFFFFFL)

        if (lastTrafficRaw >= 0) {
            val prevUpBytes = decodeTrafficHalf(lastTrafficRaw ushr 32)
            val prevDlBytes = decodeTrafficHalf(lastTrafficRaw and 0xFFFFFFFFL)
            val upSpeed = (curUpBytes - prevUpBytes).coerceAtLeast(0)
            val dlSpeed = (curDlBytes - prevDlBytes).coerceAtLeast(0)
            setUploadSpeed(formatSpeed(upSpeed))
            setDownloadSpeed(formatSpeed(dlSpeed))
        }
        lastTrafficRaw = current

        setForwarded(current)
    }

    private fun decodeTrafficHalf(half: Long): Long {
        val type = (half ushr 30) and 0x3L
        val data = half and 0x3FFFFFFFL
        return when (type) {
            0L -> data
            1L -> data * 1024
            2L -> data * 1024 * 1024
            3L -> data * 1024 * 1024 * 1024
            else -> 0L
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String = when {
        bytesPerSec >= 1024 * 1024 -> "%.1f MB/s".format(bytesPerSec / 1024.0 / 1024.0)
        bytesPerSec >= 1024         -> "%.1f KB/s".format(bytesPerSec / 1024.0)
        else                        -> "$bytesPerSec B/s"
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024 * 1024 -> "%.1f GB".format(bytes / 1024.0 / 1024.0 / 1024.0)
        bytes >= 1024L * 1024        -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
        else                         -> "${bytes / 1024} KB"
    }

    private suspend fun MainDesign.fetchUserData() {
        val authData = XBoardSession.getAuthData(this@MainActivity) ?: return
        val baseUrl = XBoardSession.getBaseUrl(this@MainActivity)

        val info = XBoardApi.getUserInfo(baseUrl, authData) ?: return

        setUserEmail(info.email.takeIf { it.isNotBlank() })

        val now = System.currentTimeMillis() / 1000
        val expired = info.expiredAt != null && info.expiredAt > 0 && info.expiredAt < now
        val expiryDisplay = when {
            info.expiredAt == null || info.expiredAt == 0L ->
                getString(R.string.expiry_permanent)
            expired -> getString(R.string.expiry_expired)
            else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(info.expiredAt * 1000))
        }
        setExpiryDate(expiryDisplay, expired)

        val usedBytes = info.usedDownload + info.usedUpload
        val trafficPercent: Float = if (info.transferEnable > 0) {
            ((usedBytes.toDouble() / info.transferEnable) * 100.0)
                .coerceIn(0.0, 100.0).toFloat()
        } else 0f
        setTrafficPercent(trafficPercent)

        setPlanName(info.planName)
        setProfileExpiryDate(expiryDisplay, expired)
        setProfileTrafficPercent(trafficPercent)

        val trafficDetail = when {
            info.transferEnable == 0L -> getString(R.string.unlimited_traffic)
            usedBytes == 0L           -> getString(R.string.not_used_yet)
            else -> getString(
                R.string.traffic_detail_format,
                formatBytes(usedBytes),
                formatBytes(info.transferEnable)
            )
        }
        setTrafficDetail(trafficDetail)

        setBalance(info.balance)
        setCommissionBalance(info.commissionBalance)

        val inviteInfo = XBoardApi.getInviteInfo(baseUrl, authData)
        setInviteLink(inviteInfo?.inviteUrl?.takeIf { it.isNotBlank() })
        setReferralCount(inviteInfo?.referralCount ?: 0)
    }

    private suspend fun MainDesign.startClash() {
        var active = withProfile { queryActive() }

        if (active == null) {
            val all = withProfile { queryAll() }
            val pending = all.firstOrNull { it.pending && !it.imported }

            if (pending == null) {
                // 无 pending profile — 检查是否有已下载但未激活的 profile（Bug Fix: imported-but-not-active）
                val importedProfile = all.firstOrNull { it.imported }
                if (importedProfile != null) {
                    withProfile { setActive(importedProfile) }
                    active = withProfile { queryActive() }
                }
                if (active == null) {
                    showToast(getString(R.string.no_subscription_hint), ToastDuration.Long)
                    return
                }
            } else {
                // 有 pending profile — 触发 commit 并显示实时进度
                showSyncDialog()
                val pendingObserver = buildFetchObserver(this)
                try {
                    withProfile { commit(pending.uuid, pendingObserver) }
                } catch (e: Exception) {
                    dismissSyncDialog()
                    showToast(
                        e.message ?: getString(R.string.subscription_commit_failed),
                        ToastDuration.Long
                    )
                    return
                }
                dismissSyncDialog()
                active = withProfile { queryActive() }
                if (active == null) {
                    showToast(getString(R.string.subscription_sync_failed), ToastDuration.Long)
                    return
                }
            }
        }

        // 若 profile 尚未同步节点，触发同步并等待完成
        if (!active.imported) {
            showSyncDialog()
            val commitObserver = buildFetchObserver(this)
            try {
                withProfile { commit(active.uuid, commitObserver) }
            } catch (e: Exception) {
                dismissSyncDialog()
                showToast(
                    e.message ?: getString(R.string.subscription_sync_failed),
                    ToastDuration.Long
                )
                return
            }
            dismissSyncDialog()

            val updated = withProfile { queryByUUID(active.uuid) }
            if (updated?.imported != true) {
                showToast(getString(R.string.subscription_sync_failed), ToastDuration.Long)
                return
            }
        }

        connectStartMs = System.currentTimeMillis()

        val vpnRequest = startClashService()
        try {
            if (vpnRequest != null) {
                val result = startActivityForResult(
                    ActivityResultContracts.StartActivityForResult(),
                    vpnRequest
                )
                if (result.resultCode == RESULT_OK)
                    startClashService()
            }
        } catch (e: Exception) {
            design?.showToast(R.string.unable_to_start_vpn, ToastDuration.Long)
        }
    }

    private fun buildFetchObserver(design: MainDesign): IFetchObserver = IFetchObserver { status ->
        val msg = when (status.action) {
            FetchStatus.Action.FetchConfiguration ->
                getString(R.string.sync_fetching_config)
            FetchStatus.Action.FetchProviders -> {
                val name = status.args.firstOrNull() ?: ""
                getString(R.string.sync_fetching_provider, status.progress + 1, status.max, name)
            }
            FetchStatus.Action.Verifying ->
                getString(R.string.sync_verifying)
        }
        val current = when (status.action) {
            FetchStatus.Action.FetchProviders -> status.progress + 1
            FetchStatus.Action.Verifying -> status.max
            else -> 0
        }
        val total = status.max.coerceAtLeast(1)
        this@MainActivity.launch { design.updateSyncProgress(current, total, msg) }
    }

    private suspend fun applyGeoDefaults() {
        withClash {
            val cfg = queryOverride(Clash.OverrideSlot.Persist)
            cfg.geodataMode = true
            cfg.geoAutoUpdate = true
            cfg.geoUpdateInterval = 24
            cfg.geoxurl.geoip = "https://cdn.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/geoip.dat"
            cfg.geoxurl.geosite = "https://cdn.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/geosite.dat"
            cfg.geoxurl.mmdb = "https://cdn.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/country.mmdb"
            cfg.geoxurl.asn = "https://cdn.jsdelivr.net/gh/xishang0128/geoip@release/GeoLite2-ASN.mmdb"
            patchOverride(Clash.OverrideSlot.Persist, cfg)
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    private suspend fun queryAppVersionName(): String {
        return withContext(Dispatchers.IO) {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher =
                registerForActivityResult(RequestPermission()) { }
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

val mainActivityAlias = "${MainActivity::class.java.name}Alias"
