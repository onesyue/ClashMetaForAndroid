package com.github.kr328.clash

import android.app.Activity
import com.github.kr328.clash.design.AccountDesign
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.remote.RemoteConfig
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.withProfile
import com.github.kr328.clash.xboard.XBoardApi
import com.github.kr328.clash.xboard.XBoardSession
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class AccountActivity : BaseActivity<AccountDesign>() {

    companion object {
        const val EXTRA_PATH     = "extra_initial_path"
        const val EXTRA_FULL_URL = "extra_full_url"
    }

    override suspend fun main() {
        val baseUrl    = RemoteConfig.getXboardUrl(this)
        val fullUrl    = intent.getStringExtra(EXTRA_FULL_URL)
        val initialPath = intent.getStringExtra(EXTRA_PATH) ?: ""
        val design = AccountDesign(
            this,
            fullUrl ?: baseUrl,
            if (fullUrl != null) "" else initialPath,
            showSyncButton = fullUrl == null
        )

        setContentDesign(design)

        while (isActive) {
            select<Unit> {
                events.onReceive { }

                design.requests.onReceive { request ->
                    when (request) {
                        is AccountDesign.Request.AuthDataChanged -> {
                            // Persist token so native API calls work without WebView
                            XBoardSession.save(this@AccountActivity, request.authData, request.baseUrl)
                        }

                        is AccountDesign.Request.SyncSubscription -> {
                            if (request.authData.isBlank()) {
                                design.showToast(
                                    getString(R.string.not_logged_in),
                                    ToastDuration.Long
                                )
                                return@onReceive
                            }

                            design.loading = true
                            try {
                                val result = XBoardApi.syncFromSession(
                                    request.baseUrl,
                                    request.authData
                                )

                                // Persist auth session after successful sync
                                XBoardSession.save(this@AccountActivity, request.authData, request.baseUrl)

                                val brandName = getString(R.string.xboard_brand_name)

                                val uuid = withProfile {
                                    queryAll()
                                        .filter { it.name == brandName }
                                        .forEach { delete(it.uuid) }

                                    create(
                                        Profile.Type.Url,
                                        brandName,
                                        result.subscribeUrl
                                    )
                                }

                                withProfile { commit(uuid, null) }

                                val profile = withProfile { queryByUUID(uuid) }
                                if (profile != null) {
                                    withProfile { setActive(profile) }
                                }

                                design.showToast(
                                    getString(R.string.subscription_synced),
                                    ToastDuration.Short
                                )

                                setResult(Activity.RESULT_OK)
                            } catch (e: Exception) {
                                design.showToast(
                                    e.message ?: getString(R.string.subscription_sync_failed),
                                    ToastDuration.Long
                                )
                            } finally {
                                design.loading = false
                            }
                        }
                    }
                }
            }
        }
    }
}
