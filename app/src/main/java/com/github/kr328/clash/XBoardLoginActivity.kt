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
                design.requests.onReceive {
                    when (it) {
                        is XBoardLoginDesign.Request.Login ->
                            performLogin(design, it.url, it.email, it.password)
                    }
                }
            }
        }
    }

    private suspend fun performLogin(
        design: XBoardLoginDesign,
        url: String,
        email: String,
        password: String
    ) {
        design.processing = true
        try {
            val result = XBoardApi.login(url, email, password)
            val uuid = withProfile {
                create(Profile.Type.Url, email, result.subscribeUrl)
            }
            launchProperties(uuid)
        } catch (e: Exception) {
            design.showToast(
                e.message ?: getString(R.string.xboard_login_failed),
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
