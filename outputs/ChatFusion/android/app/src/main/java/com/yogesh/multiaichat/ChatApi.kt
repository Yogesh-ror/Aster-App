package com.yogesh.multiaichat

import android.content.Context
import android.content.pm.PackageManager
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AuthResult(
    val token: String,
    val userId: String,
    val email: String,
    val name: String = "",
    val mobileNumber: String = "",
    val photoUrl: String = ""
)

data class UserProfile(
    val userId: String,
    val email: String,
    val name: String = "",
    val mobileNumber: String = "",
    val photoUrl: String = ""
)

data class ChatItem(
    val id: String,
    val title: String,
    val provider: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class ChatMessage(
    val id: String,
    val chatId: String,
    val role: String,
    val content: String,
    val provider: String,
    val createdAt: Long
)

data class SendResult(val userMessage: ChatMessage, val assistantMessage: ChatMessage)

class ApiException(message: String) : Exception(message)

object ChatApi {
    private var baseUrl: String = "https://aster-app-jsng.onrender.com"

    fun configure(context: Context) {
        @Suppress("DEPRECATION")
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        baseUrl = appInfo.metaData
            ?.getString("com.yogesh.multiaichat.API_BASE_URL")
            ?.trimEnd('/')
            ?.takeIf(String::isNotBlank)
            ?: "https://aster-app-jsng.onrender.com"
    }

    fun register(
        name: String,
        email: String,
        password: String,
        mobileNumber: String? = null,
        photoUrl: String? = null
    ): AuthResult {
        val body = JSONObject()
            .put("name", name)
            .put("email", email)
            .put("password", password)
        if (!mobileNumber.isNullOrBlank()) body.put("mobileNumber", mobileNumber)
        if (!photoUrl.isNullOrBlank()) body.put("photoUrl", photoUrl)

        val result = request("POST", "/v1/auth/register", body) as JSONObject
        return AuthResult(
            token = result.getString("token"),
            userId = result.getString("userId"),
            email = result.getString("email"),
            name = result.optString("name", name),
            mobileNumber = result.optString("mobileNumber", mobileNumber.orEmpty()),
            photoUrl = result.optString("photoUrl", photoUrl.orEmpty())
        )
    }

    fun login(email: String, password: String): AuthResult {
        val body = JSONObject().put("email", email).put("password", password)
        val result = request("POST", "/v1/auth/login", body) as JSONObject
        return AuthResult(
            token = result.getString("token"),
            userId = result.getString("userId"),
            email = result.getString("email"),
            name = result.optString("name", ""),
            mobileNumber = result.optString("mobileNumber", ""),
            photoUrl = result.optString("photoUrl", "")
        )
    }

    fun getProfile(token: String): UserProfile {
        val result = request("GET", "/v1/profile", token = token) as JSONObject
        return UserProfile(
            userId = result.getString("userId"),
            email = result.getString("email"),
            name = result.optString("name", ""),
            mobileNumber = result.optString("mobileNumber", ""),
            photoUrl = result.optString("photoUrl", "")
        )
    }

    fun updateProfile(
        token: String,
        name: String? = null,
        password: String? = null,
        mobileNumber: String? = null,
        photoUrl: String? = null
    ): UserProfile {
        val body = JSONObject()
        if (name != null) body.put("name", name)
        if (!password.isNullOrBlank()) body.put("password", password)
        if (mobileNumber != null) body.put("mobileNumber", mobileNumber)
        if (photoUrl != null) body.put("photoUrl", photoUrl)

        val result = request("PUT", "/v1/profile", body, token) as JSONObject
        return UserProfile(
            userId = result.getString("userId"),
            email = result.getString("email"),
            name = result.optString("name", ""),
            mobileNumber = result.optString("mobileNumber", ""),
            photoUrl = result.optString("photoUrl", "")
        )
    }

    fun listChats(token: String): List<ChatItem> =
        (request("GET", "/v1/chats", token = token) as JSONArray).toChatList()

    fun createChat(token: String, provider: String): ChatItem =
        (request("POST", "/v1/chats", JSONObject().put("provider", provider), token) as JSONObject).toChat()

    fun listMessages(token: String, chatId: String): List<ChatMessage> =
        (request("GET", "/v1/chats/$chatId/messages", token = token) as JSONArray).toMessageList()

    fun sendMessage(token: String, chatId: String, provider: String, message: String): SendResult {
        val response = request(
            "POST",
            "/v1/chats/$chatId/messages",
            JSONObject().put("provider", provider).put("message", message),
            token
        ) as JSONObject
        return SendResult(response.getJSONObject("userMessage").toMessage(), response.getJSONObject("assistantMessage").toMessage())
    }

    private fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        token: String? = null
    ): Any {
        val candidateUrls = listOf(
            baseUrl,
            "https://aster-app-jsng.onrender.com"
        ).distinct()

        var lastErrorMsg = "Unable to connect."

        for (targetUrl in candidateUrls) {
            val maxAttempts = if (targetUrl.contains("onrender.com")) 8 else 2
            for (attempt in 1..maxAttempts) {
                try {
                    val connection = (URL("$targetUrl$path").openConnection() as HttpURLConnection).apply {
                        requestMethod = method
                        connectTimeout = 45_000
                        readTimeout = 60_000
                        setRequestProperty("Accept", "application/json")
                        if (token != null) setRequestProperty("Authorization", "Bearer $token")
                        if (body != null) {
                            doOutput = true
                            setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        }
                    }

                    if (body != null) {
                        connection.outputStream.use { stream ->
                            stream.write(body.toString().toByteArray(Charsets.UTF_8))
                        }
                    }

                    val status = connection.responseCode
                    val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                    val responseText = stream?.use {
                        BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText()
                    }.orEmpty()

                    val response = runCatching { JSONTokener(responseText).nextValue() }.getOrElse { JSONObject() }
                    
                    if (status == 502 || status == 503) {
                        // Render cold start -> force retry
                        throw Exception("Render sleeping ($status)")
                    }

                    if (status !in 200..299) {
                        val detail = (response as? JSONObject)?.optString("error").orEmpty()
                        val cleanDetail = when {
                            detail.contains("temporarily unavailable", ignoreCase = true) ||
                            detail.contains("quota", ignoreCase = true) ||
                            detail.contains("429", ignoreCase = true) ->
                                "Aster AI service is currently busy or undergoing maintenance. Please try again in a moment."
                            else -> detail.ifBlank { "Server request failed ($status)." }
                        }
                        throw ApiException(cleanDetail)
                    }

                    baseUrl = targetUrl
                    connection.disconnect()
                    return response
                } catch (error: ApiException) {
                    throw error
                } catch (e: Exception) {
                    lastErrorMsg = e.toString()
                    if (attempt < maxAttempts) {
                        try { Thread.sleep(4000) } catch (_: Exception) {}
                    }
                }
            }
        }

        throw ApiException("Server unreachable. Please check your internet connection or try again later. (Detail: $lastErrorMsg)")
    }

    private fun JSONObject.toChat() = ChatItem(
        id = getString("id"),
        title = optString("title", "New chat"),
        provider = optString("provider", "openai"),
        createdAt = optLong("createdAt"),
        updatedAt = optLong("updatedAt")
    )

    private fun JSONArray.toChatList(): List<ChatItem> = buildList {
        for (index in 0 until length()) add(getJSONObject(index).toChat())
    }

    private fun JSONObject.toMessage() = ChatMessage(
        id = getString("id"),
        chatId = getString("chatId"),
        role = getString("role"),
        content = getString("content"),
        provider = optString("provider", "openai"),
        createdAt = optLong("createdAt")
    )

    private fun JSONArray.toMessageList(): List<ChatMessage> = buildList {
        for (index in 0 until length()) add(getJSONObject(index).toMessage())
    }
}
