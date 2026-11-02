package com.example.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface OpenRouterService {

    @POST("chat/completions")
    @Headers(
        "Content-Type: application/json",
        "HTTP-Referer: https://devlkakkoth.devai",
        "X-Title: Dart AI Android Assistant"
    )
    suspend fun getChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>
}
