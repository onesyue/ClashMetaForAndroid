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
        val inviteUrl: String,
        val referralCount: Int
    )

    data class PaymentMethod(
        val id: Int,
        val name: String,
        val payment: String   // gateway type, e.g. "AlipayF2F"
    )

    data class CheckoutResult(
        val type: Int,        // -1=free, 0=URL, 1=HTML
        val data: String      // URL or HTML content
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

    data class Notice(
        val id: Int,
        val title: String,
        val content: String,
        val createdAt: Long         // Unix timestamp
    )

    data class Order(
        val tradeNo: String,
        val planName: String,
        val period: String,
        val totalAmount: Long,      // cents
        val status: Int,            // 0=pending, 1=processing, 2=cancelled, 3=completed, 4=discounted
        val createdAt: Long         // Unix timestamp
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
     * 获取邀请信息 + 邀请人数（合并调用 /api/v1/user/invite）
     * codes[0].code → 邀请码；stat[0] → 已注册用户数
     */
    suspend fun getInviteInfo(baseUrl: String, authData: String): InviteInfo? {
        return try {
            withContext(Dispatchers.IO) {
                val data = httpGet(baseUrl, "/api/v1/user/invite", authData)
                    .optJSONObject("data") ?: return@withContext null
                val codes = data.optJSONArray("codes")
                val code = codes?.optJSONObject(0)?.optString("code", "") ?: ""
                val url = if (code.isNotBlank())
                    "${baseUrl.trimEnd('/')}/#/register?code=$code"
                else ""
                val stat = data.optJSONArray("stat")
                val referralCount = stat?.optInt(0, 0) ?: 0
                InviteInfo(url, referralCount)
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
     * 获取用户信息，失败时静默返回 null
     * 注意：u/d 流量字段在 getSubscribe 里，info 里不含
     */
    suspend fun getUserInfo(baseUrl: String, authData: String): UserInfo? {
        return try {
            withContext(Dispatchers.IO) {
                val infoData = httpGet(baseUrl, "/api/v1/user/info", authData)
                    .getJSONObject("data")

                val subData = try {
                    httpGet(baseUrl, "/api/v1/user/getSubscribe", authData)
                        .getJSONObject("data")
                } catch (_: Exception) { null }

                val planName = subData?.optJSONObject("plan")
                    ?.optString("name")?.takeIf { it.isNotBlank() }

                UserInfo(
                    email = infoData.optString("email", ""),
                    balance = infoData.optLong("balance", 0),
                    commissionBalance = infoData.optLong("commission_balance", 0),
                    expiredAt = if (infoData.isNull("expired_at")) null
                                else infoData.optLong("expired_at").takeIf { it > 0 },
                    transferEnable = infoData.optLong("transfer_enable", 0),
                    usedDownload = subData?.optLong("d", 0) ?: 0,
                    usedUpload = subData?.optLong("u", 0) ?: 0,
                    uuid = infoData.optString("uuid", ""),
                    planName = planName
                )
            }
        } catch (_: Exception) { null }
    }

    /**
     * 获取支付方式列表
     */
    suspend fun getPaymentMethods(baseUrl: String, authData: String): List<PaymentMethod> {
        return try {
            withContext(Dispatchers.IO) {
                val arr = httpGet(baseUrl, "/api/v1/user/order/getPaymentMethod", authData)
                    .optJSONArray("data") ?: return@withContext emptyList()
                (0 until arr.length()).mapNotNull { i ->
                    val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                    PaymentMethod(
                        id      = obj.optInt("id"),
                        name    = obj.optString("name", ""),
                        payment = obj.optString("payment", "")
                    )
                }
            }
        } catch (_: Exception) { emptyList() }
    }

    /**
     * 创建订单，返回 trade_no
     */
    suspend fun createOrder(baseUrl: String, authData: String, planId: Int, period: String): String {
        return withContext(Dispatchers.IO) {
            val root = httpPostAuth(
                baseUrl, "/api/v1/user/order/save",
                JSONObject().apply {
                    put("plan_id", planId)
                    put("period", period)
                },
                authData
            )
            root.getString("data")
        }
    }

    /**
     * 结算订单，返回支付结果
     * type=-1: 免费/余额支付成功; type=0: 跳转URL; type=1: HTML内容
     */
    suspend fun checkoutOrder(baseUrl: String, authData: String, tradeNo: String, methodId: Int): CheckoutResult {
        return withContext(Dispatchers.IO) {
            val root = httpPostAuth(
                baseUrl, "/api/v1/user/order/checkout",
                JSONObject().apply {
                    put("trade_no", tradeNo)
                    put("method", methodId)
                },
                authData
            )
            val type = root.optInt("type", 0)
            val data = root.opt("data")?.toString() ?: ""
            CheckoutResult(type, data)
        }
    }

    /**
     * 获取公告列表
     */
    suspend fun getNotices(baseUrl: String, authData: String): List<Notice> {
        return try {
            withContext(Dispatchers.IO) {
                val root = httpGet(baseUrl, "/api/v1/user/notice", authData)
                val arr = root.optJSONArray("data") ?: return@withContext emptyList()
                (0 until arr.length()).mapNotNull { i ->
                    val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                    Notice(
                        id        = obj.optInt("id"),
                        title     = obj.optString("title", ""),
                        content   = obj.optString("content", ""),
                        createdAt = obj.optLong("created_at", 0)
                    )
                }
            }
        } catch (_: Exception) { emptyList() }
    }

    /**
     * 获取订单列表
     */
    suspend fun getOrders(baseUrl: String, authData: String): List<Order> {
        return try {
            withContext(Dispatchers.IO) {
                val root = httpGet(baseUrl, "/api/v1/user/order/fetch", authData)
                val arr = root.optJSONArray("data") ?: return@withContext emptyList()
                (0 until arr.length()).mapNotNull { i ->
                    val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                    val planObj = obj.optJSONObject("plan")
                    Order(
                        tradeNo     = obj.optString("trade_no", ""),
                        planName    = planObj?.optString("name", "") ?: obj.optString("plan_name", ""),
                        period      = obj.optString("period", ""),
                        totalAmount = obj.optLong("total_amount", 0),
                        status      = obj.optInt("status", 0),
                        createdAt   = obj.optLong("created_at", 0)
                    )
                }
            }
        } catch (_: Exception) { emptyList() }
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
