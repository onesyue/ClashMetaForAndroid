package com.github.kr328.clash

import android.app.Activity
import android.content.Intent
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.design.XBoardLoginDesign
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.remote.RemoteConfig
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.withProfile
import com.github.kr328.clash.xboard.XBoardApi
import com.github.kr328.clash.xboard.XBoardSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
                        is XBoardLoginDesign.Request.WebLogin -> {
                            // CF 验证触发时降级到 WebView 登录
                            val result = startActivityForResult(
                                androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                                Intent(this@XBoardLoginActivity, AccountActivity::class.java)
                            )
                            if (result.resultCode == Activity.RESULT_OK) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
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

            // commit() 完成后再 setActive，否则 ImportedDao 还没有该记录，setActive 会静默失败
            @Suppress("OPT_IN_USAGE")
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    withProfile { commit(uuid, null) }
                    val imported = withProfile { queryByUUID(uuid) }
                    if (imported != null) {
                        withProfile { setActive(imported) }
                    }
                } catch (_: Exception) {}
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
