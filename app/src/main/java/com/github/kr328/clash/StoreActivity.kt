package com.github.kr328.clash

import com.github.kr328.clash.design.AccountDesign
import com.github.kr328.clash.remote.RemoteConfig
import com.github.kr328.clash.xboard.XBoardSession
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class StoreActivity : BaseActivity<AccountDesign>() {

    override suspend fun main() {
        val baseUrl = RemoteConfig.getXboardUrl(this)
        val design = AccountDesign(this, baseUrl, "/#/buy", showSyncButton = false)

        setContentDesign(design)

        while (isActive) {
            select<Unit> {
                events.onReceive { }

                design.requests.onReceive { request ->
                    when (request) {
                        is AccountDesign.Request.AuthDataChanged -> {
                            XBoardSession.save(this@StoreActivity, request.authData, request.baseUrl)
                        }
                        is AccountDesign.Request.SyncSubscription -> {
                            // Sync not needed from store page; ignore
                        }
                    }
                }
            }
        }
    }
}
