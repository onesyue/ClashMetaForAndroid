package com.github.kr328.clash.xboard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object XBoardApi {
    data class LoginResult(val subscribeUrl: String)

    suspend fun login(baseUrl: String, email: String, password: String): LoginResult {
        return withContext(Dispatchers.IO) {
            val url = URL("${baseUrl.trimEnd('/')}/api/v1/passport/auth/login")
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000

                OutputStreamWriter(conn.outputStream).use {
                    it.write(
                        JSONObject().apply {
                            put("email", email)
                            put("password", password)
                        }.toString()
                    )
                }

                val responseCode = conn.responseCode
                val responseText = (if (responseCode == 200) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.readText() ?: ""

                val root = JSONObject(responseText)
                if (responseCode != 200) {
                    throw Exception(root.optString("message", "Login failed ($responseCode)"))
                }

                val token = root.getJSONObject("data").getString("auth_data")
                LoginResult("${baseUrl.trimEnd('/')}/api/v1/client/subscribe?token=$token")
            } finally {
                conn.disconnect()
            }
        }
    }
}
