package com.github.kr328.clash

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.util.OfflineCache
import com.github.kr328.clash.util.SubscriptionChecker
import com.github.kr328.clash.xboard.XBoardApi
import com.github.kr328.clash.xboard.XBoardSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    data class UserState(
        val info: XBoardApi.UserInfo? = null,
        val inviteInfo: XBoardApi.InviteInfo? = null,
        val loading: Boolean = false,
        val authExpired: Boolean = false
    )

    private val _userState = MutableStateFlow(UserState())
    val userState: StateFlow<UserState> = _userState

    fun fetchUserData() {
        val context = getApplication<Application>()
        val authData = XBoardSession.getAuthData(context) ?: return
        val baseUrl = XBoardSession.getBaseUrl(context)

        viewModelScope.launch {
            _userState.update { it.copy(loading = true) }

            val info: XBoardApi.UserInfo? = try {
                val fetched = withContext(Dispatchers.IO) {
                    XBoardApi.getUserInfo(baseUrl, authData)
                }
                if (fetched != null) {
                    OfflineCache.put(context, OfflineCache.KEY_USER_INFO, fetched.toJson())
                }
                fetched ?: loadCached(context)
            } catch (e: XBoardApi.AuthExpiredException) {
                _userState.update { it.copy(loading = false, authExpired = true) }
                return@launch
            } catch (_: Exception) {
                loadCached(context)
            }

            val inviteInfo = try {
                withContext(Dispatchers.IO) {
                    XBoardApi.getInviteInfo(baseUrl, authData)
                }
            } catch (e: XBoardApi.AuthExpiredException) {
                _userState.update { it.copy(loading = false, authExpired = true) }
                return@launch
            } catch (_: Exception) { null }

            _userState.update {
                UserState(
                    info = info,
                    inviteInfo = inviteInfo,
                    loading = false,
                    authExpired = false
                )
            }

            if (info != null) {
                SubscriptionChecker.check(context, info.expiredAt)
                SubscriptionChecker.checkTraffic(
                    context, info.usedUpload, info.usedDownload, info.transferEnable
                )
            }
        }
    }

    fun consumeAuthExpired() {
        _userState.update { it.copy(authExpired = false) }
    }

    private fun loadCached(context: Context): XBoardApi.UserInfo? {
        val cached = OfflineCache.get(context, OfflineCache.KEY_USER_INFO)
        return cached?.let { XBoardApi.UserInfo.fromJson(it) }
    }
}
