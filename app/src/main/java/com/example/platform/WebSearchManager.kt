package com.example.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class WebSearchManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun searchWeb(query: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            // Query DuckDuckGo Instant Answer API
            val url = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "DartAI-Android-Assistant/1.0")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (body.isBlank()) {
                return@withContext Result.failure(Exception("Empty response from web service"))
            }

            val json = JSONObject(body)
            val abstractText = json.optString("AbstractText", "")
            val heading = json.optString("Heading", "")
            val abstractSource = json.optString("AbstractSource", "")
            val abstractUrl = json.optString("AbstractURL", "")

            if (abstractText.isNotBlank()) {
                val formatted = buildString {
                    appendLine("🌐 **$heading** ($abstractSource)")
                    appendLine(abstractText)
                    if (abstractUrl.isNotBlank()) {
                        appendLine("\nSource: $abstractUrl")
                    }
                }
                return@withContext Result.success(formatted)
            }

            // Fallback to related topics
            val relatedTopics = json.optJSONArray("RelatedTopics")
            if (relatedTopics != null && relatedTopics.length() > 0) {
                val results = mutableListOf<String>()
                for (i in 0 until minOf(relatedTopics.length(), 4)) {
                    val topic = relatedTopics.optJSONObject(i)
                    val text = topic?.optString("Text", "") ?: ""
                    if (text.isNotBlank()) {
                        results.add("• $text")
                    }
                }
                if (results.isNotEmpty()) {
                    return@withContext Result.success("Web Knowledge Results for '$query':\n" + results.joinToString("\n\n"))
                }
            }

            Result.success("Web search completed for '$query', but no direct instant synopsis was found. You can browse further or refine the query.")
        } catch (e: Exception) {
            Result.failure(Exception("Web search error for '$query': ${e.localizedMessage}"))
        }
    }
}
