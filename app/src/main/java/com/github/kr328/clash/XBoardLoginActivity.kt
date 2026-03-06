package com.github.kr328.clash

import android.app.Activity
import com.github.kr328.clash.design.XBoardLoginDesign
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.remote.RemoteConfig
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.withProfile
import com.github.kr328.clash.xboard.XBoardApi
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

            val brandName = getString(R.string.xboard_brand_name)
            val uuid = withProfile {
                // 同名订阅先删除再创建，保证链接最新
                queryAll()
                    .filter { it.name == brandName }
                    .forEach { delete(it.uuid) }
                create(Profile.Type.Url, brandName, result.subscribeUrl)
            }

            // 自动下载并导入配置
            withProfile { commit(uuid, null) }

            // 激活订阅
            val profile = withProfile { queryByUUID(uuid) }
            if (profile != null) {
                withProfile { setActive(profile) }
            }

            design.showToast(getString(R.string.subscription_synced), ToastDuration.Short)
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
}
