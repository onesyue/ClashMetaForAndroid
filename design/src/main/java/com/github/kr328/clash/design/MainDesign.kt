package com.github.kr328.clash.design

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.core.util.trafficTotal
import com.github.kr328.clash.design.databinding.DesignMainBinding
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.resolveThemedColor
import com.github.kr328.clash.design.util.root
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainDesign(context: Context) : Design<MainDesign.Request>(context) {

    enum class Request {
        ToggleStatus,
        OpenProxy,
        OpenAccount,
        OpenProfiles,
        OpenStore,
        OpenLogs,
        OpenSettings,
        OpenAbout,
        Logout,
        ChangePassword,
        OpenNotices,
        OpenOrders,
        OpenUserSettings,
    }

    private val binding = DesignMainBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    // 修改密码临时状态，由 MainActivity 读取后清空
    var pendingPasswordChange: Pair<String, String>? = null

    // ── DataBinding variables ──────────────────────────────────────────────

    suspend fun setProfileName(name: String?) {
        withContext(Dispatchers.Main) { binding.profileName = name }
    }

    suspend fun setClashRunning(running: Boolean) {
        withContext(Dispatchers.Main) { binding.clashRunning = running }
    }

    suspend fun setForwarded(value: Long) {
        withContext(Dispatchers.Main) { binding.forwarded = value.trafficTotal() }
    }

    suspend fun setMode(mode: TunnelState.Mode) {
        withContext(Dispatchers.Main) {
            binding.mode = when (mode) {
                TunnelState.Mode.Direct -> context.getString(R.string.direct_mode)
                TunnelState.Mode.Global -> context.getString(R.string.global_mode)
                TunnelState.Mode.Rule   -> context.getString(R.string.rule_mode)
                else                    -> context.getString(R.string.rule_mode)
            }
        }
    }

    // ── Home tab ───────────────────────────────────────────────────────────

    suspend fun setUserEmail(email: String?) {
        withContext(Dispatchers.Main) {
            val display = email ?: context.getString(R.string.xboard_login_summary)
            val letter  = email?.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
            binding.emailText.text = display
            binding.avatarLetterHeader.text = letter
            binding.avatarLetterCard.text = letter
            binding.profileHeaderEmailText.text = display
            binding.profileAvatarLetter.text = letter
        }
    }

    suspend fun setExpiryDate(date: String?, expired: Boolean = false) {
        withContext(Dispatchers.Main) {
            binding.expiryText.text = if (date != null)
                context.getString(R.string.expiry_prefix) + date
            else
                context.getString(R.string.expiry_unknown)
            binding.expiryText.setTextColor(
                if (expired) ContextCompat.getColor(context, R.color.color_status_bad) else ContextCompat.getColor(context, R.color.color_text_secondary)
            )
        }
    }

    suspend fun setTrafficPercent(percent: Float) {
        withContext(Dispatchers.Main) {
            val label = formatPercent(percent)
            binding.trafficLabel.text = context.getString(R.string.traffic_usage_label, label)
            binding.trafficProgress.progress = percent.toInt().coerceIn(0, 100)
        }
    }

    suspend fun setConnectionTime(time: String) {
        withContext(Dispatchers.Main) { binding.connectionTimeText.text = time }
    }

    suspend fun setDownloadSpeed(speed: String) {
        withContext(Dispatchers.Main) {
            val parts = splitSpeedValueUnit(speed)
            binding.downloadSpeedValue.text = parts.first
            binding.downloadSpeedUnit.text = " ${parts.second}"
        }
    }

    suspend fun setUploadSpeed(speed: String) {
        withContext(Dispatchers.Main) {
            val parts = splitSpeedValueUnit(speed)
            binding.uploadSpeedValue.text = parts.first
            binding.uploadSpeedUnit.text = " ${parts.second}"
        }
    }

    /** Split "123.4 MB/s" → Pair("123.4", "MB/s") */
    private fun splitSpeedValueUnit(speed: String): Pair<String, String> {
        val idx = speed.indexOfFirst { it == ' ' }
        return if (idx > 0) Pair(speed.substring(0, idx), speed.substring(idx + 1))
        else Pair(speed, "")
    }

    // ── Profile tab ────────────────────────────────────────────────────────

    suspend fun setPlanName(name: String?) {
        withContext(Dispatchers.Main) {
            val display = name ?: context.getString(R.string.plan_unknown)
            binding.profilePlanNameText.text = display
            binding.profileHeaderPlanBadge.text = display
        }
    }

    suspend fun setProfileExpiryDate(date: String?, expired: Boolean = false) {
        withContext(Dispatchers.Main) {
            binding.profileExpiryText.text = date ?: "--"
            binding.profileExpiryText.setTextColor(
                if (expired) ContextCompat.getColor(context, R.color.color_status_bad) else ContextCompat.getColor(context, R.color.color_text_primary)
            )
        }
    }

    suspend fun setProfileTrafficPercent(percent: Float) {
        withContext(Dispatchers.Main) {
            val label = formatPercent(percent)
            binding.profileTrafficLabel.text = context.getString(R.string.traffic_usage_label, label)
            binding.profileTrafficProgress.progress = percent.toInt().coerceIn(0, 100)
        }
    }

    suspend fun setTrafficDetail(detail: String) {
        withContext(Dispatchers.Main) { binding.profileTrafficDetailText.text = detail }
    }

    suspend fun setBalance(cents: Long) {
        withContext(Dispatchers.Main) {
            binding.profileBalanceText.text = "%.2f".format(cents / 100.0)
        }
    }

    suspend fun setCommissionBalance(cents: Long) {
        withContext(Dispatchers.Main) {
            binding.profileCommissionText.text = "%.2f".format(cents / 100.0)
        }
    }

    suspend fun setInviteLink(link: String?) {
        withContext(Dispatchers.Main) {
            if (link.isNullOrBlank()) {
                binding.profileInviteCard.visibility = View.GONE
            } else {
                binding.profileInviteLinkText.text = link
                binding.profileInviteCard.visibility = View.VISIBLE
            }
        }
    }

    suspend fun setReferralCount(count: Int) {
        withContext(Dispatchers.Main) {
            binding.profileReferralCountText.text =
                context.getString(R.string.referral_count_format, count)
        }
    }

    /**
     * 退出登录后重置所有 UI 为初始占位符
     */
    suspend fun resetUserData() {
        withContext(Dispatchers.Main) {
            val unknownEmail = context.getString(R.string.xboard_login_summary)
            val unknownPlan  = context.getString(R.string.plan_unknown)
            val unknownExp   = context.getString(R.string.expiry_unknown)

            // Home tab
            binding.emailText.text = unknownEmail
            binding.avatarLetterHeader.text = "U"
            binding.avatarLetterCard.text = "U"
            binding.expiryText.text = unknownExp
            binding.expiryText.setTextColor(ContextCompat.getColor(context, R.color.color_text_secondary))
            binding.trafficLabel.text = context.getString(R.string.traffic_usage_label, "0%")
            binding.trafficProgress.progress = 0

            // Profile tab header
            binding.profileHeaderEmailText.text = unknownEmail
            binding.profileAvatarLetter.text = "U"
            binding.profileHeaderPlanBadge.text = unknownPlan

            // Profile tab cards
            binding.profilePlanNameText.text = unknownPlan
            binding.profileExpiryText.text = "--"
            binding.profileExpiryText.setTextColor(ContextCompat.getColor(context, R.color.color_text_primary))
            binding.profileTrafficLabel.text = context.getString(R.string.traffic_usage_label, "0%")
            binding.profileTrafficProgress.progress = 0
            binding.profileTrafficDetailText.text = ""
            binding.profileBalanceText.text = "--"
            binding.profileCommissionText.text = "--"
            binding.profileInviteCard.visibility = View.GONE
        }
    }

    // ── Sync progress dialog ────────────────────────────────────────────────

    private fun formatPercent(percent: Float): String = when {
        percent <= 0f -> "0%"
        percent < 1f  -> "<1%"
        percent >= 100f -> "100%"
        percent < 10f -> "%.1f%%".format(percent)
        else -> "${percent.toInt()}%"
    }

    private var syncDialog: AlertDialog? = null
    private var syncMsgView: android.widget.TextView? = null
    private var syncProgressBar: android.widget.ProgressBar? = null

    suspend fun showSyncDialog() {
        withContext(Dispatchers.Main) {
            if (syncDialog?.isShowing == true) return@withContext
            val dp = context.resources.displayMetrics.density
            val pad = (24 * dp).toInt()
            val padSm = (8 * dp).toInt()

            val msgView = android.widget.TextView(context).apply {
                text = context.getString(R.string.syncing_subscription_detail)
                textSize = 14f
            }
            val progressBar = android.widget.ProgressBar(
                context, null, android.R.attr.progressBarStyleHorizontal
            ).apply {
                isIndeterminate = false
                max = 100
                progress = 0
                visibility = android.view.View.GONE
            }
            syncMsgView = msgView
            syncProgressBar = progressBar

            val container = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(pad, padSm, pad, pad)
                addView(msgView)
                addView(progressBar, android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = padSm })
            }

            syncDialog = AlertDialog.Builder(context)
                .setTitle(R.string.syncing_subscription_title)
                .setView(container)
                .setCancelable(false)
                .show()
                .apply {
                    setOnDismissListener {
                        syncDialog = null
                        syncMsgView = null
                        syncProgressBar = null
                    }
                }
        }
    }

    suspend fun updateSyncProgress(current: Int, total: Int, message: String) {
        withContext(Dispatchers.Main) {
            syncMsgView?.text = message
            syncProgressBar?.apply {
                visibility = android.view.View.VISIBLE
                max = total.coerceAtLeast(1)
                progress = current
            }
        }
    }

    suspend fun dismissSyncDialog() {
        withContext(Dispatchers.Main) {
            syncDialog?.dismiss()
            syncDialog = null
            syncMsgView = null
            syncProgressBar = null
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────

    suspend fun showAbout(versionName: String) {
        withContext(Dispatchers.Main) {
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.xboard_brand_name))
                .setMessage(
                    context.getString(R.string.xboard_brand_subtitle) + "\n\nv$versionName"
                )
                .setPositiveButton(R.string.ok, null)
                .show()
        }
    }

    init {
        binding.self = this
        binding.colorConnected =
            context.resolveThemedColor(com.google.android.material.R.attr.colorPrimary)
        binding.colorDisconnected =
            context.resolveThemedColor(R.attr.colorClashStopped)

        // 连续点击品牌名 5 次（3 秒内）→ 隐藏设置入口
        var clickCount = 0
        var lastClickMs = 0L
        binding.brandNameText.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastClickMs > 3_000) clickCount = 0
            lastClickMs = now
            if (++clickCount >= 5) { clickCount = 0; requests.trySend(Request.OpenSettings) }
        }

        // 邀请链接复制
        binding.profileCopyInviteBtn.setOnClickListener {
            val link = binding.profileInviteLinkText.text?.toString()
                ?.takeIf { it.isNotBlank() } ?: return@setOnClickListener
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newPlainText("invite_link", link))
            android.widget.Toast.makeText(context, R.string.copied, android.widget.Toast.LENGTH_SHORT).show()
        }

        // 公告
        binding.profileNoticesBtn.setOnClickListener {
            requests.trySend(Request.OpenNotices)
        }

        // 我的订单
        binding.profileOrdersBtn.setOnClickListener {
            requests.trySend(Request.OpenOrders)
        }

        // 修改密码
        binding.profileChangePasswordBtn.setOnClickListener {
            val dp = (context.resources.displayMetrics.density + 0.5f).toInt()
            val pad = 20 * dp
            val oldEdit = EditText(context).apply {
                hint = context.getString(R.string.old_password)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                transformationMethod = PasswordTransformationMethod.getInstance()
            }
            val newEdit = EditText(context).apply {
                hint = context.getString(R.string.new_password)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                transformationMethod = PasswordTransformationMethod.getInstance()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 12 * dp }
            }
            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(pad, 0, pad, 0)
                addView(oldEdit, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                addView(newEdit)
            }
            AlertDialog.Builder(context)
                .setTitle(R.string.change_password)
                .setView(layout)
                .setPositiveButton(R.string.ok) { _, _ ->
                    val old = oldEdit.text?.toString() ?: ""
                    val new = newEdit.text?.toString() ?: ""
                    if (old.isNotBlank() && new.isNotBlank()) {
                        pendingPasswordChange = Pair(old, new)
                        requests.trySend(Request.ChangePassword)
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        // 设置
        binding.profileSettingsBtn.setOnClickListener {
            requests.trySend(Request.OpenUserSettings)
        }

        // 退出登录（带确认）
        binding.profileLogoutBtn.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_message)
                .setPositiveButton(R.string.ok) { _, _ -> requests.trySend(Request.Logout) }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        // 底部导航
        var currentTabId = R.id.nav_home
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    binding.homeContent.visibility = View.VISIBLE
                    binding.storeContent.visibility = View.GONE
                    binding.profileContent.visibility = View.GONE
                    currentTabId = R.id.nav_home
                    true
                }
                R.id.nav_store -> {
                    requests.trySend(Request.OpenStore)
                    binding.bottomNav.post { binding.bottomNav.selectedItemId = currentTabId }
                    false
                }
                R.id.nav_profile -> {
                    binding.homeContent.visibility = View.GONE
                    binding.storeContent.visibility = View.GONE
                    binding.profileContent.visibility = View.VISIBLE
                    currentTabId = R.id.nav_profile
                    true
                }
                else -> false
            }
        }
    }

    fun request(request: Request) {
        requests.trySend(request)
    }
}
