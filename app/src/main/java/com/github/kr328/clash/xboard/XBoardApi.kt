package com.github.kr328.clash.xboard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object XBoardApi {
    data class AuthResult(val subscribeUrl: String, val authData: String = "")

    data class UserInfo(
        val email: String,
        val balance: Long,            // in cents (divide by 100 for yuan)
        val commissionBalance: Long,  // in cents
        val expiredAt: Long?,         // Unix timestamp, null = unlimited
        val transferEnable: Long,     // bytes total
        val usedDownload: Long,       // bytes downloaded (d)
        val usedUpload: Long,         // bytes uploaded (u)
        val inviteCode: String,
        val uuid: String,
        val planName: String?
    )

    /**
     * 登录流程：
     * 1. POST /api/v1/passport/auth/login → 拿 auth_data（JWT session token）
     * 2. GET  /api/v1/user/getSubscribe   → 拿真实的 subscribe_url
     */
    suspend fun login(baseUrl: String, email: String, password: String): AuthResult {
        val authData = postAuth(
            baseUrl = baseUrl,
            path = "/api/v1/passport/auth/login",
            body = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
        )
        return AuthResult(fetchSubscribeUrl(baseUrl, authData), authData)
    }

    /**
     * 注册流程（同登录，注册成功后直接拿订阅）
     */
    suspend fun register(
        baseUrl: String,
        email: String,
        password: String,
        inviteCode: String
    ): AuthResult {
        val authData = postAuth(
            baseUrl = baseUrl,
            path = "/api/v1/passport/auth/register",
            body = JSONObject().apply {
                put("email", email)
                put("password", password)
                if (inviteCode.isNotBlank()) put("invite_code", inviteCode)
            }
        )
        return AuthResult(fetchSubscribeUrl(baseUrl, authData), authData)
    }

    /**
     * 发送登录/注册请求，返回 auth_data（JWT session token）
     */
    private suspend fun postAuth(baseUrl: String, path: String, body: JSONObject): String {
        return withContext(Dispatchers.IO) {
            val response = httpPost(baseUrl, path, body)
            response.getJSONObject("data").getString("auth_data")
        }
    }

    /**
     * WebView 登录后，用页面存储的 auth_data 直接同步订阅
     */
    suspend fun syncFromSession(baseUrl: String, authData: String): AuthResult {
        return AuthResult(fetchSubscribeUrl(baseUrl, authData), authData)
    }

    /**
     * 用 auth_data 调用 getSubscribe，返回服务端生成的 subscribe_url
     */
    private suspend fun fetchSubscribeUrl(baseUrl: String, authData: String): String {
        return withContext(Dispatchers.IO) {
            val url = URL("${baseUrl.trimEnd('/')}/api/v1/user/getSubscribe")
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                conn.setRequestProperty("authorization", authData)
                conn.setRequestProperty("Accept", "application/json")
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000

                val responseCode = conn.responseCode
                val responseText = (if (responseCode == 200) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.readText() ?: ""

                val root = JSONObject(responseText)
                if (responseCode != 200) {
                    throw Exception(root.optString("message", "Failed to get subscription ($responseCode)"))
                }

                val data = root.getJSONObject("data")

                // 只使用服务端返回的 subscribe_url，不自行拼接
                val subscribeUrl = data.optString("subscribe_url", "").trim()
                if (subscribeUrl.isEmpty()) {
                    throw Exception("服务器未返回订阅地址，请联系管理员")
                }
                subscribeUrl
            } finally {
                conn.disconnect()
            }
        }
    }

    /**
     * 退出登录（最佳努力，失败静默忽略）
     */
    suspend fun logout(baseUrl: String, authData: String) {
        try {
            withContext(Dispatchers.IO) {
                val url = URL("${baseUrl.trimEnd('/')}/api/v1/user/logout")
                val conn = url.openConnection() as HttpURLConnection
                try {
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("authorization", authData)
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Accept", "application/json")
                    conn.doOutput = true
                    conn.connectTimeout = 10_000
                    conn.readTimeout = 10_000
                    conn.outputStream.close()
                    conn.responseCode // trigger request
                } finally {
                    conn.disconnect()
                }
            }
        } catch (_: Exception) { /* silent */ }
    }

    /**
     * 获取邀请人数，失败静默返回 0
     */
    suspend fun getReferralCount(baseUrl: String, authData: String): Int {
        return try {
            withContext(Dispatchers.IO) {
                val root = httpGet(baseUrl, "/api/v1/user/stat", authData)
                val data = root.optJSONObject("data")
                data?.optInt("register_count", 0) ?: 0
            }
        } catch (_: Exception) { 0 }
    }

    /**
     * 获取用户信息（email、余额、到期时间、流量、邀请码、套餐名称）
     * 失败时静默返回 null
     */
    suspend fun getUserInfo(baseUrl: String, authData: String): UserInfo? {
        return try {
            withContext(Dispatchers.IO) {
                val infoData = httpGet(baseUrl, "/api/v1/user/info", authData)
                    .getJSONObject("data")

                val planName = try {
                    val subData = httpGet(baseUrl, "/api/v1/user/getSubscribe", authData)
                        .getJSONObject("data")
                    subData.optJSONObject("plan")?.optString("name")?.takeIf { it.isNotBlank() }
                } catch (_: Exception) { null }

                UserInfo(
                    email = infoData.optString("email", ""),
                    balance = infoData.optLong("balance", 0),
                    commissionBalance = infoData.optLong("commission_balance", 0),
                    expiredAt = if (infoData.isNull("expired_at")) null
                                else infoData.optLong("expired_at").takeIf { it > 0 },
                    transferEnable = infoData.optLong("transfer_enable", 0),
                    usedDownload = infoData.optLong("d", 0),
                    usedUpload = infoData.optLong("u", 0),
                    inviteCode = infoData.optString("invite_code", ""),
                    uuid = infoData.optString("uuid", ""),
                    planName = planName
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 通用 GET 请求，返回解析后的 JSON 根对象（在 IO 线程调用）
     */
    private fun httpGet(baseUrl: String, path: String, authData: String): JSONObject {
        val url = URL("${baseUrl.trimEnd('/')}$path")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.setRequestProperty("authorization", authData)
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000

            val responseCode = conn.responseCode
            val responseText = (if (responseCode == 200) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.readText() ?: ""

            val root = JSONObject(responseText)
            if (responseCode != 200) {
                throw Exception(root.optString("message", "Request failed ($responseCode)"))
            }
            return root
        } finally {
            conn.disconnect()
        }
    }

    /**
     * 通用 POST 请求，返回解析后的 JSON 根对象
     */
    private fun httpPost(baseUrl: String, path: String, body: JSONObject): JSONObject {
        val url = URL("${baseUrl.trimEnd('/')}$path")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000

            OutputStreamWriter(conn.outputStream).use {
                it.write(body.toString())
            }

            val responseCode = conn.responseCode
            val responseText = (if (responseCode == 200) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.readText() ?: ""

            val root = JSONObject(responseText)
            if (responseCode != 200) {
                throw Exception(root.optString("message", "Request failed ($responseCode)"))
            }
            return root
        } finally {
            conn.disconnect()
        }
    }
}
