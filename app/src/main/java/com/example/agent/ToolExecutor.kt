package com.example.agent

import android.content.Context
import com.example.data.api.ApiToolCall
import com.example.domain.model.ToolResult
import org.json.JSONObject

data class ToolExecutionReport(
    val toolCallId: String,
    val toolName: String,
    val argumentsJson: String,
    val isSuccess: Boolean,
    val resultText: String
)

class ToolExecutor(
    private val toolRegistry: ToolRegistry,
    private val context: Context
) {

    suspend fun executeTool(toolCall: ApiToolCall): ToolExecutionReport {
        val functionName = toolCall.function.name
        val rawArgs = toolCall.function.arguments

        val tool = toolRegistry.getTool(functionName)
        if (tool == null) {
            return ToolExecutionReport(
                toolCallId = toolCall.id,
                toolName = functionName,
                argumentsJson = rawArgs,
                isSuccess = false,
                resultText = "Error: Tool '$functionName' is not recognized by Dart AI Tool Registry."
            )
        }

        val jsonArgs = try {
            if (rawArgs.isBlank()) JSONObject() else JSONObject(rawArgs)
        } catch (e: Exception) {
            return ToolExecutionReport(
                toolCallId = toolCall.id,
                toolName = functionName,
                argumentsJson = rawArgs,
                isSuccess = false,
                resultText = "Error: Failed to parse tool arguments as valid JSON: ${e.localizedMessage}"
            )
        }

        return try {
            val result: ToolResult = tool.execute(context, jsonArgs)
            ToolExecutionReport(
                toolCallId = toolCall.id,
                toolName = functionName,
                argumentsJson = rawArgs,
                isSuccess = result.success,
                resultText = result.output
            )
        } catch (e: Exception) {
            ToolExecutionReport(
                toolCallId = toolCall.id,
                toolName = functionName,
                argumentsJson = rawArgs,
                isSuccess = false,
                resultText = "Execution failed with exception: ${e.localizedMessage ?: e.javaClass.simpleName}"
            )
        }
    }
}
