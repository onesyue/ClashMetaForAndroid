package com.github.kr328.clash

import com.github.kr328.clash.design.NoticesDesign
import com.github.kr328.clash.xboard.XBoardApi
import com.github.kr328.clash.xboard.XBoardSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class NoticesActivity : BaseActivity<NoticesDesign>() {

    override suspend fun main() {
        val design = NoticesDesign(this)
        setContentDesign(design)

        loadNotices(design)

        while (isActive) {
            select<Unit> {
                events.onReceive { }
            }
        }
    }

    private fun loadNotices(design: NoticesDesign) {
        design.showLoading()
        launch(Dispatchers.IO) {
            try {
                val authData = XBoardSession.getAuthData(this@NoticesActivity)
                    ?: return@launch
                val baseUrl = XBoardSession.getBaseUrl(this@NoticesActivity)
                val notices = XBoardApi.getNotices(baseUrl, authData)
                val items = notices.map {
                    NoticesDesign.Notice(
                        id        = it.id,
                        title     = it.title,
                        content   = it.content,
                        createdAt = it.createdAt
                    )
                }
                withContext(Dispatchers.Main) { design.showNotices(items) }
            } catch (e: XBoardApi.AuthExpiredException) {
                withContext(Dispatchers.Main) { handleAuthExpired() }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { design.showNotices(emptyList()) }
            }
        }
    }

}
