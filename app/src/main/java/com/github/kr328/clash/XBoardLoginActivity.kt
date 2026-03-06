package com.github.kr328.clash

import android.app.Activity
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.design.XBoardLoginDesign
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.remote.RemoteConfig
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.withProfile
import com.github.kr328.clash.xboard.XBoardApi
import com.github.kr328.clash.xboard.XBoardSession
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class XBoardLoginActivity : BaseActivity<XBoardLoginDesign>() {

    override suspend fun main() {
        val design = XBoardLoginDesign(this, RemoteConfig.getXboardUrl(this))

        setContentDesign(design)

        while (isActive) {
            select<Unit> {
                events.onReceive {}
                design.requests.onReceive { request ->
                    when (request) {
                        is XBoardLoginDesign.Request.Login -> {
                            performAuth(design, request.url, request.email, request.password) {
                                XBoardApi.login(request.url, request.email, request.password)
                            }
                        }
                        is XBoardLoginDesign.Request.Register -> {
                            performAuth(design, request.url, request.email, request.password) {
                                XBoardApi.register(
                                    request.url,
                                    request.email,
                                    request.password,
                                    request.inviteCode
                                )
                            }
                        }
                        is XBoardLoginDesign.Request.ForgotPassword -> {
                            performForgotPassword(design, request.email)
                        }
                    }
                }
            }
        }
    }

    private suspend fun performAuth(
        design: XBoardLoginDesign,
        url: String,
        email: String,
        password: String,
        action: suspend () -> XBoardApi.AuthResult
    ) {
        design.processing = true
        try {
            val result = action()

            if (result.authData.isNotBlank()) {
                XBoardSession.save(this@XBoardLoginActivity, result.authData, url)
            }

            val brandName = getString(R.string.xboard_brand_name)
            val uuid = withProfile {
                queryAll()
                    .filter { it.name == brandName }
                    .forEach { delete(it.uuid) }
                create(Profile.Type.Url, brandName, result.subscribeUrl)
            }

            withProfile { commit(uuid, null) }

            val profile = withProfile { queryByUUID(uuid) }
            if (profile != null) {
                withProfile { setActive(profile) }
            }

            design.showToast(getString(R.string.subscription_synced), ToastDuration.Short)

            // 若是从退出登录后以根 Activity 启动，则重新拉起主界面
            if (isTaskRoot) {
                startActivity(MainActivity::class.intent)
            }

            setResult(Activity.RESULT_OK)
            finish()
        } catch (e: Exception) {
            design.showToast(
                e.message ?: getString(R.string.xboard_request_failed),
                ToastDuration.Long
            )
        } finally {
            design.processing = false
        }
    }

    private suspend fun performForgotPassword(design: XBoardLoginDesign, email: String) {
        design.processing = true
        try {
            val url = RemoteConfig.getXboardUrl(this)
            XBoardApi.forgotPassword(url, email)
            design.showToast(getString(R.string.forgot_password_sent), ToastDuration.Long)
        } catch (e: Exception) {
            design.showToast(
                e.message ?: getString(R.string.xboard_request_failed),
                ToastDuration.Long
            )
        } finally {
            design.processing = false
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // 登录页按返回键退出整个 App（强制登录策略）
        finishAffinity()
    }
}
