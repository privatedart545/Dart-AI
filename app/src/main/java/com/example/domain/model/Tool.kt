package com.example.domain.model

import android.content.Context
import com.example.data.api.ApiFunctionDefinition
import com.example.data.api.ApiTool
import org.json.JSONObject

data class ToolResult(
    val success: Boolean,
    val output: String,
    val requiresUserAction: Boolean = false,
    val userActionPrompt: String? = null,
    val metadata: Map<String, Any>? = null
) {
    fun toSerializedJson(): String {
        val json = JSONObject()
        json.put("success", success)
        json.put("output", output)
        if (requiresUserAction && userActionPrompt != null) {
            json.put("requires_user_action", true)
            json.put("action_prompt", userActionPrompt)
        }
        return json.toString()
    }
}

interface DeviceTool {
    val name: String
    val description: String
    val parameterSchema: Map<String, Any>

    suspend fun execute(context: Context, arguments: JSONObject): ToolResult

    fun toApiTool(): ApiTool {
        return ApiTool(
            type = "function",
            function = ApiFunctionDefinition(
                name = name,
                description = description,
                parameters = parameterSchema
            )
        )
    }
}
