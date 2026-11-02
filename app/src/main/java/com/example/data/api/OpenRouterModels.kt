package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<ApiMessage>,
    @Json(name = "tools") val tools: List<ApiTool>? = null,
    @Json(name = "temperature") val temperature: Double? = 0.3,
    @Json(name = "max_tokens") val maxTokens: Int? = 1500
)

@JsonClass(generateAdapter = true)
data class ApiMessage(
    @Json(name = "role") val role: String, // system, user, assistant, tool
    @Json(name = "content") val content: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "tool_call_id") val toolCallId: String? = null,
    @Json(name = "tool_calls") val toolCalls: List<ApiToolCall>? = null
)

@JsonClass(generateAdapter = true)
data class ApiTool(
    @Json(name = "type") val type: String = "function",
    @Json(name = "function") val function: ApiFunctionDefinition
)

@JsonClass(generateAdapter = true)
data class ApiFunctionDefinition(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "parameters") val parameters: Map<String, Any>
)

@JsonClass(generateAdapter = true)
data class ApiToolCall(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String = "function",
    @Json(name = "function") val function: ApiFunctionCall
)

@JsonClass(generateAdapter = true)
data class ApiFunctionCall(
    @Json(name = "name") val name: String,
    @Json(name = "arguments") val arguments: String
)

@JsonClass(generateAdapter = true)
data class ChatCompletionResponse(
    @Json(name = "id") val id: String?,
    @Json(name = "choices") val choices: List<ApiChoice>?,
    @Json(name = "usage") val usage: ApiUsage?,
    @Json(name = "error") val error: ApiError? = null
)

@JsonClass(generateAdapter = true)
data class ApiChoice(
    @Json(name = "index") val index: Int?,
    @Json(name = "message") val message: ApiMessage,
    @Json(name = "finish_reason") val finishReason: String?
)

@JsonClass(generateAdapter = true)
data class ApiUsage(
    @Json(name = "prompt_tokens") val promptTokens: Int?,
    @Json(name = "completion_tokens") val completionTokens: Int?,
    @Json(name = "total_tokens") val totalTokens: Int?
)

@JsonClass(generateAdapter = true)
data class ApiError(
    @Json(name = "message") val message: String?,
    @Json(name = "code") val code: Any?,
    @Json(name = "type") val type: String? = null
)
