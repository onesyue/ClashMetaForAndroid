package com.github.kr328.clash.design

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
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
    }

    private val binding = DesignMainBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    // ── DataBinding variables ──────────────────────────────────────────────

    suspend fun setProfileName(name: String?) {
        withContext(Dispatchers.Main) {
            binding.profileName = name
        }
    }

    suspend fun setClashRunning(running: Boolean) {
        withContext(Dispatchers.Main) {
            binding.clashRunning = running
        }
    }

    suspend fun setForwarded(value: Long) {
        withContext(Dispatchers.Main) {
            binding.forwarded = value.trafficTotal()
        }
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

    // ── Home tab — programmatic setters ───────────────────────────────────

    suspend fun setUserEmail(email: String?) {
        withContext(Dispatchers.Main) {
            val display = email ?: context.getString(R.string.xboard_login_summary)
            binding.emailText.text = display
            val letter = email?.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
            binding.avatarLetterHeader.text = letter
            binding.avatarLetterCard.text = letter
        }
    }

    suspend fun setExpiryDate(date: String?) {
        withContext(Dispatchers.Main) {
            binding.expiryText.text = if (date != null) {
                context.getString(R.string.expiry_prefix) + date
            } else {
                context.getString(R.string.expiry_unknown)
            }
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
        withContext(Dispatchers.Main) {
            binding.connectionTimeText.text = time
        }
    }

    suspend fun setDownloadSpeed(speed: String) {
        withContext(Dispatchers.Main) {
            binding.downloadSpeedText.text = speed
        }
    }

    suspend fun setUploadSpeed(speed: String) {
        withContext(Dispatchers.Main) {
            binding.uploadSpeedText.text = speed
        }
    }

    // ── Profile tab — programmatic setters ───────────────────────────────

    suspend fun setPlanName(name: String?) {
        withContext(Dispatchers.Main) {
            binding.profilePlanNameText.text = name
                ?: context.getString(R.string.plan_unknown)
        }
    }

    suspend fun setProfileExpiryDate(date: String?) {
        withContext(Dispatchers.Main) {
            binding.profileExpiryText.text = date ?: "--"
        }
    }

    suspend fun setProfileTrafficPercent(percent: Int) {
        withContext(Dispatchers.Main) {
            binding.profileTrafficLabel.text = context.getString(R.string.traffic_usage_label)
                .replace("0%", "$percent%")
            binding.profileTrafficProgress.progress = percent
        }
    }

    suspend fun setBalance(cents: Long) {
        withContext(Dispatchers.Main) {
            val yuan = "%.2f".format(cents / 100.0)
            binding.profileBalanceText.text = context.getString(R.string.balance_format, yuan)
        }
    }

    suspend fun setCommissionBalance(cents: Long) {
        withContext(Dispatchers.Main) {
            val yuan = "%.2f".format(cents / 100.0)
            binding.profileCommissionText.text = context.getString(R.string.balance_format, yuan)
        }
    }

    suspend fun setInviteLink(link: String?) {
        withContext(Dispatchers.Main) {
            binding.profileInviteLinkText.text = link ?: "--"
        }
    }

    suspend fun setReferralCount(count: Int) {
        withContext(Dispatchers.Main) {
            binding.profileReferralCountText.text =
                context.getString(R.string.referral_count_format, count)
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────

    suspend fun showAbout(versionName: String) {
        withContext(Dispatchers.Main) {
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.xboard_brand_name))
                .setMessage(
                    context.getString(R.string.xboard_brand_subtitle) +
                        "\n\n版本：$versionName"
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

        // 连续点击品牌名 5 次（3 秒内）进入隐藏设置
        var clickCount = 0
        var lastClickMs = 0L
        binding.brandNameText.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastClickMs > 3_000) clickCount = 0
            lastClickMs = now
            if (++clickCount >= 5) {
                clickCount = 0
                requests.trySend(Request.OpenSettings)
            }
        }
        binding.profileCopyInviteBtn.setOnClickListener {
            val link = binding.profileInviteLinkText.text?.toString()
                ?.takeIf { it.isNotBlank() && it != "--" } ?: return@setOnClickListener
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newPlainText("invite_link", link))
            android.widget.Toast.makeText(context, R.string.copied, android.widget.Toast.LENGTH_SHORT).show()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    binding.homeContent.visibility = View.VISIBLE
                    binding.storeContent.visibility = View.GONE
                    binding.profileContent.visibility = View.GONE
                    true
                }
                R.id.nav_store -> {
                    binding.homeContent.visibility = View.GONE
                    binding.storeContent.visibility = View.VISIBLE
                    binding.profileContent.visibility = View.GONE
                    true
                }
                R.id.nav_profile -> {
                    binding.homeContent.visibility = View.GONE
                    binding.storeContent.visibility = View.GONE
                    binding.profileContent.visibility = View.VISIBLE
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
