package com.github.kr328.clash.xboard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object XBoardApi {
    data class AuthResult(val subscribeUrl: String)

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
        return AuthResult(fetchSubscribeUrl(baseUrl, authData))
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
        return AuthResult(fetchSubscribeUrl(baseUrl, authData))
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

                // 优先使用服务端直接给的 subscribe_url
                if (data.has("subscribe_url") && !data.isNull("subscribe_url")) {
                    data.getString("subscribe_url")
                } else {
                    // 降级：用 token 字段手动拼接
                    val token = data.getString("token")
                    "${baseUrl.trimEnd('/')}/api/v1/client/subscribe?token=$token"
                }
            } finally {
                conn.disconnect()
            }
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
