package com.example.agent

import com.example.data.api.ApiMessage
import com.example.data.api.ChatCompletionRequest
import com.example.data.api.OpenRouterClient
import com.example.data.preferences.DevAiPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class OrchestratorEvent {
    data class ToolStarted(val toolCallId: String, val toolName: String, val argsJson: String) : OrchestratorEvent()
    data class ToolCompleted(val report: ToolExecutionReport) : OrchestratorEvent()
    data class AgentReasoning(val round: Int) : OrchestratorEvent()
}

data class AgentExecutionResult(
    val replyText: String,
    val toolReports: List<ToolExecutionReport>,
    val finalMessages: List<ApiMessage>
)

class AgentOrchestrator(
    private val openRouterClient: OpenRouterClient,
    private val toolRegistry: ToolRegistry,
    private val toolExecutor: ToolExecutor,
    private val preferences: DevAiPreferences
) {

    private val _events = MutableSharedFlow<OrchestratorEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<OrchestratorEvent> = _events.asSharedFlow()

    companion object {
        const val MAX_TOOL_ROUNDS = 6
    }

    suspend fun processUserTurn(
        userPrompt: String,
        history: List<ApiMessage>
    ): Result<AgentExecutionResult> {
        val apiKey = preferences.openRouterApiKey
        if (apiKey.isBlank()) {
            return Result.failure(
                IllegalStateException("OpenRouter API Key is not set. Please go to Settings to add your key.")
            )
        }

        val modelId = preferences.modelId.ifBlank { "nvidia/nemotron-3-ultra-550b-a55b" }
        val tools = toolRegistry.getApiTools()

        // Build message sequence starting with system prompt
        val messages = mutableListOf<ApiMessage>()
        messages.add(ApiMessage(role = "system", content = SystemPrompt.DEFAULT_PROMPT))

        // Append recent history (exclude any old system prompts)
        for (m in history) {
            if (m.role != "system") {
                messages.add(m)
            }
        }

        // Add current user prompt
        messages.add(ApiMessage(role = "user", content = userPrompt))

        val executedReports = mutableListOf<ToolExecutionReport>()
        var round = 0
        var finalReply = ""

        while (round < MAX_TOOL_ROUNDS) {
            round++
            _events.emit(OrchestratorEvent.AgentReasoning(round))

            val request = ChatCompletionRequest(
                model = modelId,
                messages = messages.toList(),
                tools = tools,
                temperature = 0.2,
                maxTokens = 1500
            )

            val responseResult = openRouterClient.getChatCompletion(apiKey, request)
            if (responseResult.isFailure) {
                val err = responseResult.exceptionOrNull()
                return Result.failure(err ?: Exception("OpenRouter API request failed."))
            }

            val response = responseResult.getOrThrow()
            val choice = response.choices?.firstOrNull()
                ?: return Result.failure(Exception("OpenRouter returned an empty response choices list."))

            val assistantMsg = choice.message
            val toolCalls = assistantMsg.toolCalls

            if (toolCalls.isNullOrEmpty()) {
                // Assistant returned final text answer
                finalReply = assistantMsg.content?.trim() ?: "Task completed."
                messages.add(ApiMessage(role = "assistant", content = finalReply))
                break
            } else {
                // Assistant wants to call one or more tools
                messages.add(assistantMsg)

                for (call in toolCalls) {
                    _events.emit(OrchestratorEvent.ToolStarted(call.id, call.function.name, call.function.arguments))
                    val report = toolExecutor.executeTool(call)
                    executedReports.add(report)
                    _events.emit(OrchestratorEvent.ToolCompleted(report))

                    // Feed tool result back into conversation
                    messages.add(
                        ApiMessage(
                            role = "tool",
                            name = call.function.name,
                            toolCallId = call.id,
                            content = report.resultText
                        )
                    )
                }
            }
        }

        if (finalReply.isEmpty() && round >= MAX_TOOL_ROUNDS) {
            finalReply = "Completed tool actions. (Reached sequential reasoning limit of $MAX_TOOL_ROUNDS rounds)."
        }

        return Result.success(
            AgentExecutionResult(
                replyText = finalReply,
                toolReports = executedReports,
                finalMessages = messages
            )
        )
    }
}
