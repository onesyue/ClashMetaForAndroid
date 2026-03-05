package com.github.kr328.clash

import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setUUID
import com.github.kr328.clash.design.XBoardLoginDesign
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.withProfile
import com.github.kr328.clash.xboard.XBoardApi
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import java.util.UUID

class XBoardLoginActivity : BaseActivity<XBoardLoginDesign>() {
    override suspend fun main() {
        val design = XBoardLoginDesign(this)

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
            val uuid = withProfile {
                create(Profile.Type.Url, email, result.subscribeUrl)
            }
            launchProperties(uuid)
        } catch (e: Exception) {
            design.showToast(
                e.message ?: getString(R.string.xboard_request_failed),
                ToastDuration.Long
            )
        } finally {
            design.processing = false
        }
    }

    private suspend fun launchProperties(uuid: UUID) {
        val r = startActivityForResult(
            ActivityResultContracts.StartActivityForResult(),
            PropertiesActivity::class.intent.setUUID(uuid)
        )
        if (r.resultCode == Activity.RESULT_OK) finish()
    }
}
