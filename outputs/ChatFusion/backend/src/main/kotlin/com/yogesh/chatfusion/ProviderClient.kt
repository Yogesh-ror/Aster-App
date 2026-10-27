package com.yogesh.chatfusion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ProviderClient(
    private val openAiKey: String,
    private val openAiModel: String,
    private val geminiKey: String,
    private val geminiModel: String
) {
    suspend fun generateResponse(provider: String, history: List<ConversationTurn>, prompt: String): Pair<String, String> {
        val selected = provider.lowercase()
        return when (selected) {
            "openai" -> {
                runCatching { "openai" to generateOpenAi(history, prompt) }
                    .getOrElse { openAiError ->
                        // Fallback to Gemini if OpenAI fails
                        runCatching { "gemini" to generateGemini(history, prompt) }
                            .getOrElse { geminiError ->
                                throw RuntimeException("Aster AI service is temporarily unavailable. OpenAI: ${openAiError.message} | Gemini: ${geminiError.message}")
                            }
                    }
            }
            "gemini" -> {
                runCatching { "gemini" to generateGemini(history, prompt) }
                    .getOrElse { geminiError ->
                        // Fallback to OpenAI if Gemini fails
                        runCatching { "openai" to generateOpenAi(history, prompt) }
                            .getOrElse { openAiError ->
                                throw RuntimeException("Aster AI service is temporarily unavailable. Gemini: ${geminiError.message} | OpenAI: ${openAiError.message}")
                            }
                    }
            }
            else -> generateFastest(history, prompt)
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private suspend fun generateFastest(history: List<ConversationTurn>, prompt: String): Pair<String, String> = coroutineScope {
        val openAiDeferred = async(Dispatchers.IO) {
            runCatching { "openai" to generateOpenAi(history, prompt) }
        }
        val geminiDeferred = async(Dispatchers.IO) {
            runCatching { "gemini" to generateGemini(history, prompt) }
        }

        val firstFinished = select {
            openAiDeferred.onAwait { it }
            geminiDeferred.onAwait { it }
        }

        if (firstFinished.isSuccess) {
            firstFinished.getOrThrow()
        } else {
            val secondResult = if (openAiDeferred.isCompleted) geminiDeferred.await() else openAiDeferred.await()
            secondResult.getOrElse {
                val openAiErr = openAiDeferred.getCompletionExceptionOrNull()?.message
                val geminiErr = geminiDeferred.getCompletionExceptionOrNull()?.message
                throw RuntimeException("Aster AI service is temporarily unavailable. OpenAI: $openAiErr | Gemini: $geminiErr")
            }
        }
    }

    fun generateOpenAi(history: List<ConversationTurn>, prompt: String): String {
        if (openAiKey.isBlank()) throw IllegalArgumentException("OpenAI API key is missing.")
        val messages = JSONArray()
        history.forEach { turn ->
            messages.put(JSONObject().put("role", turn.role).put("content", turn.content))
        }
        messages.put(JSONObject().put("role", "user").put("content", prompt))

        val body = JSONObject()
            .put("model", openAiModel)
            .put("messages", messages)

        @Suppress("DEPRECATION")
        val connection = (URL("https://api.openai.com/v1/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 60_000
            setRequestProperty("Authorization", "Bearer $openAiKey")
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
        }

        connection.outputStream.use { it.write(body.toString().toByteArray()) }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream.bufferedReader().readText()

        if (status !in 200..299) {
            val err = runCatching { JSONObject(responseText).getJSONObject("error").getString("message") }.getOrDefault(responseText)
            throw RuntimeException("OpenAI Error ($status): $err")
        }

        val choices = JSONObject(responseText).getJSONArray("choices")
        if (choices.length() == 0) return "No response generated."
        return choices.getJSONObject(0).getJSONObject("message").getString("content")
    }

    fun generateGemini(history: List<ConversationTurn>, prompt: String): String {
        if (geminiKey.isBlank()) throw IllegalArgumentException("Gemini API key is missing.")
        val contents = JSONArray()
        history.forEach { turn ->
            val role = if (turn.role == "assistant") "model" else "user"
            val parts = JSONArray().put(JSONObject().put("text", turn.content))
            contents.put(JSONObject().put("role", role).put("parts", parts))
        }
        val userParts = JSONArray().put(JSONObject().put("text", prompt))
        contents.put(JSONObject().put("role", "user").put("parts", userParts))

        val body = JSONObject().put("contents", contents)
        val modelName = if (geminiModel.isBlank() || geminiModel.contains("3.8")) "gemini-1.5-flash" else geminiModel
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$geminiKey"

        @Suppress("DEPRECATION")
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 60_000
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
        }

        connection.outputStream.use { it.write(body.toString().toByteArray()) }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream.bufferedReader().readText()

        if (status !in 200..299) {
            val err = runCatching { JSONObject(responseText).getJSONObject("error").getString("message") }.getOrDefault(responseText)
            throw RuntimeException("Gemini Error ($status): $err")
        }

        val candidates = JSONObject(responseText).optJSONArray("candidates") ?: return "No response generated."
        if (candidates.length() == 0) return "No response generated."
        val parts = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts")
        return parts.getJSONObject(0).getString("text")
    }
}
