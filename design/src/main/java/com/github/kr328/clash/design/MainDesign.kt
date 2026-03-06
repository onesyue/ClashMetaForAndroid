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
                if (expired) 0xFFD32F2F.toInt() else 0xFF5C7CAB.toInt()
            )
        }
    }

    suspend fun setTrafficPercent(percent: Int) {
        withContext(Dispatchers.Main) {
            binding.trafficLabel.text = context.getString(R.string.traffic_usage_label)
                .replace("0%", "$percent%")
            binding.trafficProgress.progress = percent
        }
    }

    suspend fun setConnectionTime(time: String) {
        withContext(Dispatchers.Main) { binding.connectionTimeText.text = time }
    }

    suspend fun setDownloadSpeed(speed: String) {
        withContext(Dispatchers.Main) { binding.downloadSpeedText.text = speed }
    }

    suspend fun setUploadSpeed(speed: String) {
        withContext(Dispatchers.Main) { binding.uploadSpeedText.text = speed }
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
                if (expired) 0xFFD32F2F.toInt() else 0xFF1A237E.toInt()
            )
        }
    }

    suspend fun setProfileTrafficPercent(percent: Int) {
        withContext(Dispatchers.Main) {
            binding.profileTrafficLabel.text = context.getString(R.string.traffic_usage_label)
                .replace("0%", "$percent%")
            binding.profileTrafficProgress.progress = percent
        }
    }

    suspend fun setTrafficDetail(detail: String) {
        withContext(Dispatchers.Main) { binding.profileTrafficDetailText.text = detail }
    }

    suspend fun setBalance(cents: Long) {
        withContext(Dispatchers.Main) {
            binding.profileBalanceText.text =
                context.getString(R.string.balance_format, "%.2f".format(cents / 100.0))
        }
    }

    suspend fun setCommissionBalance(cents: Long) {
        withContext(Dispatchers.Main) {
            binding.profileCommissionText.text =
                context.getString(R.string.balance_format, "%.2f".format(cents / 100.0))
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
            binding.expiryText.setTextColor(0xFF5C7CAB.toInt())
            binding.trafficLabel.text = context.getString(R.string.traffic_usage_label)
            binding.trafficProgress.progress = 0

            // Profile tab header
            binding.profileHeaderEmailText.text = unknownEmail
            binding.profileAvatarLetter.text = "U"
            binding.profileHeaderPlanBadge.text = unknownPlan

            // Profile tab cards
            binding.profilePlanNameText.text = unknownPlan
            binding.profileExpiryText.text = "--"
            binding.profileExpiryText.setTextColor(0xFF1A237E.toInt())
            binding.profileTrafficLabel.text = context.getString(R.string.traffic_usage_label)
            binding.profileTrafficProgress.progress = 0
            binding.profileTrafficDetailText.text = ""
            binding.profileBalanceText.text = "¥ --"
            binding.profileCommissionText.text = "¥ --"
            binding.profileInviteCard.visibility = View.GONE
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────

    suspend fun showAbout(versionName: String) {
        withContext(Dispatchers.Main) {
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.xboard_brand_name))
                .setMessage(
                    context.getString(R.string.xboard_brand_subtitle) + "\n\n版本：$versionName"
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
