package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class OpenRouterClient {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    private val service: OpenRouterService = Retrofit.Builder()
        .baseUrl("https://openrouter.ai/api/v1/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OpenRouterService::class.java)

    suspend fun testConnection(apiKey: String, model: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("OpenRouter API Key is empty. Please enter your key."))
        }
        try {
            val testMessage = listOf(
                ApiMessage(role = "user", content = "Reply with 'Dart AI Online' only.")
            )
            val request = ChatCompletionRequest(
                model = model.ifBlank { "nvidia/nemotron-3-ultra-550b-a55b" },
                messages = testMessage,
                maxTokens = 20
            )
            val authHeader = "Bearer ${apiKey.trim()}"
            val response = service.getChatCompletion(authHeader, request)

            if (response.isSuccessful) {
                val body = response.body()
                val text = body?.choices?.firstOrNull()?.message?.content?.trim() ?: "Connection OK"
                Result.success(text)
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                val errorMessage = when (response.code()) {
                    401 -> "Invalid OpenRouter API Key (401 Unauthorized)."
                    402 -> "Insufficient OpenRouter Credits (402 Payment Required)."
                    403 -> "Forbidden access to model or account restricted (403)."
                    404 -> "Model not found on OpenRouter (404)."
                    429 -> "Rate limit reached on OpenRouter (429)."
                    else -> "API Error ${response.code()}: $errorBody"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage ?: "Unable to connect to OpenRouter"}"))
        }
    }

    suspend fun getChatCompletion(
        apiKey: String,
        request: ChatCompletionRequest
    ): Result<ChatCompletionResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Please configure your OpenRouter API Key in Dart AI Settings first.")
            )
        }
        try {
            val authHeader = "Bearer ${apiKey.trim()}"
            val response = service.getChatCompletion(authHeader, request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Received empty response from OpenRouter."))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                val errorMsg = when (response.code()) {
                    401 -> "OpenRouter Error (401): Invalid API Key. Please verify in Settings."
                    402 -> "OpenRouter Error (402): Insufficient balance/credits for Nemotron 3 Ultra."
                    429 -> "OpenRouter Error (429): Rate limit exceeded. Please wait a moment."
                    500, 502, 503 -> "OpenRouter Service unavailable (${response.code()}). Retrying may resolve this."
                    else -> "OpenRouter Error (${response.code()}): $errorBody"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection failure: ${e.localizedMessage ?: "Network request failed"}"))
        }
    }
}
