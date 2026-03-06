package com.github.kr328.clash.design

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

    suspend fun setUserEmail(email: String?) {
        withContext(Dispatchers.Main) {
            binding.userEmail = email
        }
    }

    suspend fun setExpiryDate(date: String?) {
        withContext(Dispatchers.Main) {
            binding.expiryDate = date
        }
    }

    suspend fun setTrafficPercent(percent: Int) {
        withContext(Dispatchers.Main) {
            binding.trafficPercent = percent
        }
    }

    suspend fun setConnectionTime(time: String) {
        withContext(Dispatchers.Main) {
            binding.connectionTime = time
        }
    }

    suspend fun setDownloadSpeed(speed: String) {
        withContext(Dispatchers.Main) {
            binding.downloadSpeed = speed
        }
    }

    suspend fun setUploadSpeed(speed: String) {
        withContext(Dispatchers.Main) {
            binding.uploadSpeed = speed
        }
    }

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
        binding.trafficPercent = 0
        binding.colorConnected =
            context.resolveThemedColor(com.google.android.material.R.attr.colorPrimary)
        binding.colorDisconnected =
            context.resolveThemedColor(R.attr.colorClashStopped)

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
