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
        val usedDownload: Long,       // bytes downloaded
        val usedUpload: Long,         // bytes uploaded
        val uuid: String,
        val planName: String?
    )

    data class InviteInfo(
        val code: String,
        val inviteUrl: String
    )

    data class Plan(
        val id: Int,
        val name: String,
        val content: String,
        val transferGb: Long,       // 0 = unlimited
        val monthPrice: Long?,      // cents, null = not available
        val quarterPrice: Long?,
        val halfYearPrice: Long?,
        val yearPrice: Long?,
        val onetimePrice: Long?
    )

    /**
     * 登录流程
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
     * 注册流程
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
     * 发送忘记密码邮件（无需 auth）
     */
    suspend fun forgotPassword(baseUrl: String, email: String) {
        withContext(Dispatchers.IO) {
            httpPost(
                baseUrl, "/api/v1/passport/auth/forget",
                JSONObject().apply { put("email", email) }
            )
        }
    }

    /**
     * 修改密码
     */
    suspend fun changePassword(baseUrl: String, authData: String, oldPwd: String, newPwd: String) {
        withContext(Dispatchers.IO) {
            httpPostAuth(
                baseUrl, "/api/v1/user/changePassword",
                JSONObject().apply {
                    put("old_password", oldPwd)
                    put("new_password", newPwd)
                },
                authData
            )
        }
    }

    /**
     * WebView 登录后，用页面存储的 auth_data 直接同步订阅
     */
    suspend fun syncFromSession(baseUrl: String, authData: String): AuthResult {
        return AuthResult(fetchSubscribeUrl(baseUrl, authData), authData)
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
                    conn.responseCode
                } finally {
                    conn.disconnect()
                }
            }
        } catch (_: Exception) { /* silent */ }
    }

    /**
     * 获取邀请信息（专用接口，比 user/info 更可靠）
     */
    suspend fun getInviteInfo(baseUrl: String, authData: String): InviteInfo? {
        return try {
            withContext(Dispatchers.IO) {
                val data = httpGet(baseUrl, "/api/v1/user/invite", authData)
                    .optJSONObject("data") ?: return@withContext null
                val code = data.optString("code", "")
                val url = data.optString("invite_url", "")
                    .takeIf { it.isNotBlank() }
                    ?: if (code.isNotBlank()) "${baseUrl.trimEnd('/')}/#/register?code=$code" else ""
                if (code.isBlank() && url.isBlank()) null
                else InviteInfo(code, url)
            }
        } catch (_: Exception) { null }
    }

    /**
     * 获取套餐列表（公开接口，无需 auth）
     */
    suspend fun getPlans(baseUrl: String): List<Plan> {
        return withContext(Dispatchers.IO) {
            try {
                val arr = httpGetGuest(baseUrl, "/api/v1/guest/plan/fetch")
                    .optJSONArray("data") ?: return@withContext emptyList()
                (0 until arr.length()).mapNotNull { i ->
                    val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                    val transferBytes = obj.optLong("transfer_enable", 0)
                    Plan(
                        id = obj.optInt("id"),
                        name = obj.optString("name", ""),
                        content = obj.optString("content", ""),
                        transferGb = if (transferBytes > 0) transferBytes / (1024L * 1024 * 1024) else 0L,
                        monthPrice    = if (obj.isNull("month_price"))     null else obj.getLong("month_price"),
                        quarterPrice  = if (obj.isNull("quarter_price"))   null else obj.getLong("quarter_price"),
                        halfYearPrice = if (obj.isNull("half_year_price")) null else obj.getLong("half_year_price"),
                        yearPrice     = if (obj.isNull("year_price"))      null else obj.getLong("year_price"),
                        onetimePrice  = if (obj.isNull("onetime_price"))   null else obj.getLong("onetime_price")
                    )
                }
            } catch (_: Exception) { emptyList() }
        }
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
     * 获取用户信息，失败时静默返回 null
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
                    uuid = infoData.optString("uuid", ""),
                    planName = planName
                )
            }
        } catch (_: Exception) { null }
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private suspend fun postAuth(baseUrl: String, path: String, body: JSONObject): String {
        return withContext(Dispatchers.IO) {
            val response = httpPost(baseUrl, path, body)
            response.getJSONObject("data").getString("auth_data")
        }
    }

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

    private fun httpGetGuest(baseUrl: String, path: String): JSONObject {
        val url = URL("${baseUrl.trimEnd('/')}$path")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
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

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

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

    private fun httpPostAuth(baseUrl: String, path: String, body: JSONObject, authData: String): JSONObject {
        val url = URL("${baseUrl.trimEnd('/')}$path")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("authorization", authData)
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

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
