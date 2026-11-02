package com.example.agent

import android.content.Context
import com.example.data.api.ApiTool
import com.example.domain.model.DeviceTool
import com.example.domain.model.ToolResult
import com.example.platform.AlarmAndCalendarManager
import com.example.platform.AppLaunchManager
import com.example.platform.AppUsageAuditorManager
import com.example.platform.AudioControlManager
import com.example.platform.ClipboardAndMessagingManager
import com.example.platform.ContactsAndPhoneManager
import com.example.platform.DailyBriefingManager
import com.example.platform.DeepDocumentManager
import com.example.platform.DeviceTelemetryManager
import com.example.platform.FlashlightManager
import com.example.platform.MathCalculatorManager
import com.example.platform.NotificationReaderManager
import com.example.platform.RoutinesAndDiagnosticsManager
import com.example.platform.SafFileManager
import com.example.platform.ScheduledAutomationManager
import com.example.platform.SecurityVaultManager
import com.example.platform.SystemSettingsManager
import com.example.platform.WebSearchManager
import com.example.voice.VoiceAssistantManager
import org.json.JSONObject

class ToolRegistry(
    private val flashlightManager: FlashlightManager,
    private val appLaunchManager: AppLaunchManager,
    private val safFileManager: SafFileManager,
    private val audioControlManager: AudioControlManager,
    private val systemSettingsManager: SystemSettingsManager,
    private val deviceTelemetryManager: DeviceTelemetryManager,
    private val deepDocumentManager: DeepDocumentManager,
    private val alarmAndCalendarManager: AlarmAndCalendarManager,
    private val clipboardAndMessagingManager: ClipboardAndMessagingManager,
    private val routinesAndDiagnosticsManager: RoutinesAndDiagnosticsManager,
    private val webSearchManager: WebSearchManager? = null,
    private val mathCalculatorManager: MathCalculatorManager? = null,
    private val contactsAndPhoneManager: ContactsAndPhoneManager? = null,
    private val securityVaultManager: SecurityVaultManager? = null,
    private val dailyBriefingManager: DailyBriefingManager? = null,
    private val appUsageAuditorManager: AppUsageAuditorManager? = null,
    private val notificationReaderManager: NotificationReaderManager? = null,
    private val scheduledAutomationManager: ScheduledAutomationManager? = null,
    private val voiceAssistantManager: VoiceAssistantManager? = null,
    private val exportChatAction: (suspend () -> Result<String>)? = null
) {

    private val tools = mutableMapOf<String, DeviceTool>()

    init {
        registerFlashlightTools()
        registerAppTools()
        registerSafFileTools()
        registerDeepDocumentTools()
        registerAlarmAndCalendarTools()
        registerClipboardAndMessagingTools()
        registerRoutinesAndDiagnosticsTools()
        registerSettingsTools()
        registerAudioTools()
        registerTelemetryTools()
        registerUtilityTools()
        registerWebSearchTools()
        registerMathTools()
        registerPhoneTools()
        registerVaultTools()
        registerDailyBriefingTools()
        registerAppUsageAuditorTools()
        registerNotificationReaderTools()
        registerScheduledAutomationTools()
        registerVoiceCustomizationTools()
    }

    fun getAllTools(): List<DeviceTool> = tools.values.toList()

    fun getApiTools(): List<ApiTool> = tools.values.map { it.toApiTool() }

    fun getTool(name: String): DeviceTool? = tools[name]

    private fun register(tool: DeviceTool) {
        tools[tool.name] = tool
    }

    // --- 1. Flashlight Tools ---
    private fun registerFlashlightTools() {
        register(object : DeviceTool {
            override val name = "turn_flashlight_on"
            override val description = "Turns ON the device camera flashlight / torch."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val res = flashlightManager.turnOn()
                return if (res.isSuccess) {
                    ToolResult(true, "Flashlight turned ON.")
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to turn on flashlight.")
                }
            }
        })

        register(object : DeviceTool {
            override val name = "turn_flashlight_off"
            override val description = "Turns OFF the device camera flashlight / torch."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val res = flashlightManager.turnOff()
                return if (res.isSuccess) {
                    ToolResult(true, "Flashlight turned OFF.")
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to turn off flashlight.")
                }
            }
        })

        register(object : DeviceTool {
            override val name = "get_flashlight_status"
            override val description = "Checks whether the device flashlight is currently ON or OFF."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val status = flashlightManager.getStatus()
                return ToolResult(true, if (status) "The flashlight is currently ON." else "The flashlight is currently OFF.")
            }
        })
    }

    // --- 2. Application Management Tools ---
    private fun registerAppTools() {
        register(object : DeviceTool {
            override val name = "list_installed_apps"
            override val description = "Returns the names and package IDs of launchable apps installed on this device."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "filter" to mapOf(
                        "type" to "string",
                        "description" to "Optional search keyword to filter app names (e.g. 'camera', 'youtube', 'maps')."
                    )
                )
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val filter = arguments.optString("filter", "").ifBlank { null }
                val apps = appLaunchManager.listInstalledApps(filter)
                val appListStr = apps.take(25).joinToString("\n") { "• ${it.name} (${it.packageName})" }
                val output = if (apps.isEmpty()) {
                    "No installed launchable applications matching '$filter' were found."
                } else {
                    "Found ${apps.size} installed application(s):\n$appListStr"
                }
                return ToolResult(true, output)
            }
        })

        register(object : DeviceTool {
            override val name = "open_app"
            override val description = "Opens and launches an installed application by name or package name."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "app_name" to mapOf(
                        "type" to "string",
                        "description" to "The display name or package ID of the application (e.g. 'YouTube', 'WhatsApp', 'Chrome', 'Calculator')."
                    )
                ),
                "required" to listOf("app_name")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val appName = arguments.optString("app_name", "").trim()
                if (appName.isEmpty()) return ToolResult(false, "app_name argument is missing.")
                val res = appLaunchManager.openApp(appName)
                return if (res.isSuccess) {
                    ToolResult(true, res.getOrThrow())
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to open app '$appName'.")
                }
            }
        })

        register(object : DeviceTool {
            override val name = "get_app_info"
            override val description = "Gets version, package, and installation details for an installed app."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "app_name" to mapOf("type" to "string", "description" to "Application name or package ID.")
                ),
                "required" to listOf("app_name")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val appName = arguments.optString("app_name", "").trim()
                if (appName.isEmpty()) return ToolResult(false, "app_name argument is missing.")
                val res = appLaunchManager.getAppInfo(appName)
                return if (res.isSuccess) {
                    val info = res.getOrThrow()
                    ToolResult(
                        true,
                        "App Details:\n• Name: ${info.name}\n• Package: ${info.packageName}\n• Version: ${info.versionName} (${info.versionCode})\n• Installed: ${info.firstInstallTime}\n• Updated: ${info.lastUpdateTime}\n• System App: ${info.isSystemApp}"
                    )
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Could not get info for '$appName'.")
                }
            }
        })

        register(object : DeviceTool {
            override val name = "open_app_settings"
            override val description = "Opens the Android system application settings page for a specific app."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "app_name" to mapOf("type" to "string", "description" to "Application name or package ID.")
                ),
                "required" to listOf("app_name")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val appName = arguments.optString("app_name", "").trim()
                if (appName.isEmpty()) return ToolResult(false, "app_name argument is missing.")
                val res = appLaunchManager.openAppSettings(appName)
                return if (res.isSuccess) {
                    ToolResult(true, res.getOrThrow())
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to open settings.")
                }
            }
        })

        register(object : DeviceTool {
            override val name = "deep_link_app"
            override val description = "Deep links into specific features or queries inside popular apps (e.g. YouTube search, Spotify music, WhatsApp direct chat, Google Maps navigation, Slack, Twitter/X, Gmail, Reddit)."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "app_name" to mapOf("type" to "string", "description" to "Application name (e.g. 'YouTube', 'Spotify', 'WhatsApp', 'Maps', 'Slack', 'Twitter', 'Gmail')."),
                    "action" to mapOf("type" to "string", "description" to "Optional action ('search', 'navigate', 'chat', 'play'). Default is 'search'."),
                    "query" to mapOf("type" to "string", "description" to "Target query, song name, video name, contact number, or address."),
                    "direct_url" to mapOf("type" to "string", "description" to "Optional direct URI scheme or intent link.")
                ),
                "required" to listOf("app_name")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val appName = arguments.optString("app_name", "").trim()
                val action = arguments.optString("action", "search")
                val query = arguments.optString("query", "").ifBlank { null }
                val directUrl = arguments.optString("direct_url", "").ifBlank { null }
                if (appName.isEmpty()) return ToolResult(false, "app_name argument is required.")

                val res = appLaunchManager.deepLinkApp(appName, action, query, directUrl)
                return if (res.isSuccess) {
                    ToolResult(true, res.getOrThrow())
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Deep link failed.")
                }
            }
        })
    }

    // --- 3. Storage Access Framework (SAF) File Tools ---
    private fun registerSafFileTools() {
        register(object : DeviceTool {
            override val name = "list_authorized_folders"
            override val description = "Lists the folders that the user has authorized DEV AI to access via Android Storage Access Framework."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val folders = safFileManager.getAuthorizedFolders()
                return if (folders.isEmpty()) {
                    ToolResult(
                        true,
                        "No authorized folders found. Please tap 'Authorize Folder' in Settings or Quick Actions."
                    )
                } else {
                    val list = folders.joinToString("\n") { "• ${it.second}" }
                    ToolResult(true, "Authorized Folders:\n$list")
                }
            }
        })

        register(object : DeviceTool {
            override val name = "search_files"
            override val description = "Recursively searches for documents and files within user-authorized folders matching a name or extension."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "query" to mapOf("type" to "string", "description" to "Filename keyword or text to search for (e.g. 'notes', 'physics', 'invoice')."),
                    "extension" to mapOf("type" to "string", "description" to "Optional file extension filter without dot (e.g. 'pdf', 'docx', 'jpg', 'txt', 'png', 'mp4')."),
                    "folder_name" to mapOf("type" to "string", "description" to "Optional folder name to limit search.")
                )
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val query = arguments.optString("query", "").ifBlank { null }
                val ext = arguments.optString("extension", "").ifBlank { null }
                val folder = arguments.optString("folder_name", "").ifBlank { null }

                val res = safFileManager.searchFiles(query, ext, folder)
                return if (res.isSuccess) {
                    val files = res.getOrThrow()
                    if (files.isEmpty()) {
                        ToolResult(true, "No files matching query='${query ?: "*"}' and extension='${ext ?: "*"}' were found.")
                    } else {
                        val fileList = files.take(20).joinToString("\n") {
                            "• ${it.name} (${it.formattedSize}, Modified: ${it.lastModified})"
                        }
                        ToolResult(true, "Found ${files.size} matching file(s):\n$fileList")
                    }
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Search failed.")
                }
            }
        })

        register(object : DeviceTool {
            override val name = "list_folder_contents"
            override val description = "Lists files and subdirectories directly inside an authorized folder."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "folder_uri" to mapOf("type" to "string", "description" to "Optional folder URI string. If omitted, uses the first authorized folder.")
                )
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val uriStr = arguments.optString("folder_uri", "").ifBlank { null }
                val res = safFileManager.listFolderContents(uriStr)
                return if (res.isSuccess) {
                    val list = res.getOrThrow()
                    val formatted = list.take(30).joinToString("\n") {
                        val icon = if (it.isDirectory) "[DIR]" else "[FILE]"
                        "$icon ${it.name} (${it.formattedSize})"
                    }
                    ToolResult(true, "Folder Contents:\n$formatted")
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to list folder.")
                }
            }
        })

        register(object : DeviceTool {
            override val name = "get_file_info"
            override val description = "Retrieves size, modification date, and MIME type of a file."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "file_name" to mapOf("type" to "string", "description" to "The name of the file to inspect.")
                ),
                "required" to listOf("file_name")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val name = arguments.optString("file_name", "").trim()
                if (name.isEmpty()) return ToolResult(false, "file_name argument is required.")
                val res = safFileManager.getFileInfo(name)
                return if (res.isSuccess) {
                    val info = res.getOrThrow()
                    ToolResult(
                        true,
                        "File Details for '${info.name}':\n• Size: ${info.formattedSize} (${info.sizeBytes} bytes)\n• MIME Type: ${info.mimeType}\n• Last Modified: ${info.lastModified}\n• Type: ${if (info.isDirectory) "Directory" else "Document"}"
                    )
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "File not found.")
                }
            }
        })

        register(object : DeviceTool {
            override val name = "open_file"
            override val description = "Opens and displays a file using a compatible Android application viewer."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "file_name" to mapOf("type" to "string", "description" to "The filename to open (e.g. 'project_report.pdf', 'photo.jpg').")
                ),
                "required" to listOf("file_name")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val name = arguments.optString("file_name", "").trim()
                if (name.isEmpty()) return ToolResult(false, "file_name argument is required.")
                val res = safFileManager.openFile(name)
                return if (res.isSuccess) {
                    ToolResult(true, res.getOrThrow())
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Could not open file '$name'.")
                }
            }
        })

        register(object : DeviceTool {
            override val name = "create_folder"
            override val description = "Creates a new subdirectory inside an authorized folder."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "folder_name" to mapOf("type" to "string", "description" to "The name of the new folder to create.")
                ),
                "required" to listOf("folder_name")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val name = arguments.optString("folder_name", "").trim()
                if (name.isEmpty()) return ToolResult(false, "folder_name argument is required.")
                val res = safFileManager.createFolder(name)
                return if (res.isSuccess) {
                    ToolResult(true, res.getOrThrow())
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to create folder.")
                }
            }
        })

        register(object : DeviceTool {
            override val name = "create_file"
            override val description = "Creates a new file with text or code content in an authorized folder (e.g. 'notes.txt', 'todo.md', 'code.py')."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "file_name" to mapOf("type" to "string", "description" to "The filename with extension (e.g. 'notes.txt', 'shopping.md')."),
                    "content" to mapOf("type" to "string", "description" to "The full text or code content to write inside the file.")
                ),
                "required" to listOf("file_name", "content")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val fileName = arguments.optString("file_name", "").trim()
                val content = arguments.optString("content", "")
                if (fileName.isEmpty()) return ToolResult(false, "file_name argument is required.")
                val res = safFileManager.createFile(fileName, content)
                return if (res.isSuccess) {
                    ToolResult(true, res.getOrThrow())
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to create file '$fileName'.")
                }
            }
        })

        register(object : DeviceTool {
            override val name = "rename_file"
            override val description = "Renames an existing file in an authorized folder."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "file_name" to mapOf("type" to "string", "description" to "Current filename."),
                    "new_name" to mapOf("type" to "string", "description" to "New filename with extension.")
                ),
                "required" to listOf("file_name", "new_name")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val oldName = arguments.optString("file_name", "").trim()
                val newName = arguments.optString("new_name", "").trim()
                if (oldName.isEmpty() || newName.isEmpty()) return ToolResult(false, "Both file_name and new_name are required.")
                val res = safFileManager.renameFile(oldName, newName)
                return if (res.isSuccess) {
                    ToolResult(true, res.getOrThrow())
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to rename file.")
                }
            }
        })

        register(object : DeviceTool {
            override val name = "delete_file"
            override val description = "Deletes a file. Requires user confirmation boolean to prevent accidental data loss."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "file_name" to mapOf("type" to "string", "description" to "The filename to delete."),
                    "user_confirmed" to mapOf("type" to "boolean", "description" to "Must be true if the user explicitly confirmed deletion.")
                ),
                "required" to listOf("file_name", "user_confirmed")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val name = arguments.optString("file_name", "").trim()
                val confirmed = arguments.optBoolean("user_confirmed", false)
                val res = safFileManager.deleteFile(name, confirmed)
                return if (res.isSuccess) {
                    ToolResult(true, res.getOrThrow())
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Deletion was cancelled or failed.")
                }
            }
        })

        register(object : DeviceTool {
            override val name = "create_file_in_folder"
            override val description = "Creates a new file with text or code inside a specific folder or subdirectory."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "folder_name" to mapOf("type" to "string", "description" to "The name of the target folder to place the file into."),
                    "file_name" to mapOf("type" to "string", "description" to "The filename with extension (e.g. 'summary.txt', 'notes.md')."),
                    "content" to mapOf("type" to "string", "description" to "The text content to write inside the file.")
                ),
                "required" to listOf("folder_name", "file_name", "content")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val folder = arguments.optString("folder_name", "").trim()
                val file = arguments.optString("file_name", "").trim()
                val content = arguments.optString("content", "")
                if (folder.isEmpty() || file.isEmpty()) return ToolResult(false, "folder_name and file_name are required.")

                val res = safFileManager.createFileInFolder(folder, file, content)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to create file in folder.")
            }
        })

        register(object : DeviceTool {
            override val name = "move_file"
            override val description = "Moves a file from its current location to another authorized folder or subdirectory."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "file_name" to mapOf("type" to "string", "description" to "The name of the file to move."),
                    "target_folder_name" to mapOf("type" to "string", "description" to "The destination folder name.")
                ),
                "required" to listOf("file_name", "target_folder_name")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val file = arguments.optString("file_name", "").trim()
                val target = arguments.optString("target_folder_name", "").trim()
                if (file.isEmpty() || target.isEmpty()) return ToolResult(false, "file_name and target_folder_name are required.")

                val res = safFileManager.moveFile(file, target)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to move file.")
            }
        })
    }

    // --- 4. Deep Document Intelligence (Feature 1) ---
    private fun registerDeepDocumentTools() {
        register(object : DeviceTool {
            override val name = "read_file_content"
            override val description = "Reads and retrieves the text content of a document, note, or code file from authorized folders so DEV AI can summarize it or answer questions about it."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "file_name" to mapOf("type" to "string", "description" to "The name of the file to read (e.g. 'notes.txt', 'report.md', 'code.py').")
                ),
                "required" to listOf("file_name")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val name = arguments.optString("file_name", "").trim()
                if (name.isEmpty()) return ToolResult(false, "file_name argument is required.")
                val res = deepDocumentManager.readFileContent(name)
                return if (res.isSuccess) {
                    ToolResult(true, res.getOrThrow())
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Could not read file '$name'.")
                }
            }
        })

        register(object : DeviceTool {
            override val name = "search_file_content"
            override val description = "Performs deep grep search inside document contents in authorized folders for a specific keyword or phrase."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "query" to mapOf("type" to "string", "description" to "Text or phrase to search for inside documents."),
                    "extension" to mapOf("type" to "string", "description" to "Optional file extension to limit search (e.g. 'txt', 'md', 'py').")
                ),
                "required" to listOf("query")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val query = arguments.optString("query", "").trim()
                val ext = arguments.optString("extension", "").ifBlank { null }
                if (query.isEmpty()) return ToolResult(false, "query argument is required.")
                val res = deepDocumentManager.searchFileContent(query, ext)
                return if (res.isSuccess) {
                    ToolResult(true, res.getOrThrow())
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Search failed.")
                }
            }
        })
    }

    // --- 5. Alarms, Timers & Calendar (Feature 3) ---
    private fun registerAlarmAndCalendarTools() {
        register(object : DeviceTool {
            override val name = "set_timer"
            override val description = "Sets a countdown timer on the Android device."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "duration_seconds" to mapOf("type" to "integer", "description" to "Countdown duration in seconds."),
                    "label" to mapOf("type" to "string", "description" to "Optional timer name or label (e.g. 'Pasta', 'Workout').")
                ),
                "required" to listOf("duration_seconds")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val seconds = arguments.optInt("duration_seconds", -1)
                val label = arguments.optString("label", "").ifBlank { null }
                if (seconds <= 0) return ToolResult(false, "duration_seconds must be > 0.")
                val res = alarmAndCalendarManager.setTimer(seconds, label)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to set timer.")
            }
        })

        register(object : DeviceTool {
            override val name = "set_alarm"
            override val description = "Sets an alarm on the Android device clock."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "hour" to mapOf("type" to "integer", "description" to "Hour of the day in 24-hour format (0 to 23)."),
                    "minute" to mapOf("type" to "integer", "description" to "Minute of the hour (0 to 59)."),
                    "message" to mapOf("type" to "string", "description" to "Optional alarm message or label.")
                ),
                "required" to listOf("hour", "minute")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val hour = arguments.optInt("hour", -1)
                val minute = arguments.optInt("minute", -1)
                val msg = arguments.optString("message", "").ifBlank { null }
                val res = alarmAndCalendarManager.setAlarm(hour, minute, msg)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to set alarm.")
            }
        })

        register(object : DeviceTool {
            override val name = "show_alarms"
            override val description = "Opens the device Clock app displaying all configured alarms so the user can inspect or toggle them."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val res = alarmAndCalendarManager.showAlarms()
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to show alarms.")
            }
        })

        register(object : DeviceTool {
            override val name = "modify_or_set_alarm"
            override val description = "Sets or changes the time of an alarm on the Android clock app (24-hour format)."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "hour" to mapOf("type" to "integer", "description" to "Hour in 24-hour format (0-23)."),
                    "minute" to mapOf("type" to "integer", "description" to "Minute (0-59)."),
                    "message" to mapOf("type" to "string", "description" to "Optional alarm label or message (e.g. 'Gym', 'Wake Up')."),
                    "skip_ui" to mapOf("type" to "boolean", "description" to "Whether to skip opening the clock UI.")
                ),
                "required" to listOf("hour", "minute")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val hour = arguments.optInt("hour", -1)
                val minute = arguments.optInt("minute", -1)
                val msg = arguments.optString("message", "").ifBlank { null }
                val skipUi = arguments.optBoolean("skip_ui", false)
                val res = alarmAndCalendarManager.modifyOrSetAlarm(hour, minute, msg, skipUi = skipUi)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to set/modify alarm.")
            }
        })

        register(object : DeviceTool {
            override val name = "dismiss_alarm"
            override val description = "Dismisses an active or ringing alarm."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "label" to mapOf("type" to "string", "description" to "Optional label of the alarm to dismiss.")
                )
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val label = arguments.optString("label", "").ifBlank { null }
                val res = alarmAndCalendarManager.dismissAlarm(label)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to dismiss alarm.")
            }
        })

        register(object : DeviceTool {
            override val name = "schedule_calendar_event_direct"
            override val description = "Schedules a reminder or calendar event directly in Android Calendar WITHOUT opening the calendar app."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "title" to mapOf("type" to "string", "description" to "Event title (e.g. 'Doctor Appointment', 'Sprint Review')."),
                    "begin_time_millis" to mapOf("type" to "integer", "description" to "Start epoch timestamp in milliseconds. If omitted, uses current time + 1 hour."),
                    "duration_minutes" to mapOf("type" to "integer", "description" to "Duration in minutes. Defaults to 60."),
                    "description" to mapOf("type" to "string", "description" to "Optional details or agenda."),
                    "location" to mapOf("type" to "string", "description" to "Optional location."),
                    "reminder_minutes_before" to mapOf("type" to "integer", "description" to "Minutes before event to alert user (default 15).")
                ),
                "required" to listOf("title")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val title = arguments.optString("title", "").trim()
                if (title.isEmpty()) return ToolResult(false, "title argument is required.")

                val begin = if (arguments.has("begin_time_millis")) {
                    arguments.optLong("begin_time_millis")
                } else {
                    System.currentTimeMillis() + 60 * 60 * 1000 // 1 hour from now
                }
                val durationMin = arguments.optInt("duration_minutes", 60)
                val end = begin + (durationMin * 60 * 1000)
                val desc = arguments.optString("description", "").ifBlank { null }
                val loc = arguments.optString("location", "").ifBlank { null }
                val reminderMin = arguments.optInt("reminder_minutes_before", 15)

                val res = alarmAndCalendarManager.scheduleCalendarEventDirectly(
                    title = title,
                    beginTimeMillis = begin,
                    endTimeMillis = end,
                    description = desc,
                    location = loc,
                    reminderMinutesBefore = reminderMin
                )
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to schedule event directly.")
            }
        })

        register(object : DeviceTool {
            override val name = "get_upcoming_calendar_events"
            override val description = "Retrieves upcoming scheduled calendar appointments and reminders directly."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "days_ahead" to mapOf("type" to "integer", "description" to "Number of days ahead to inspect (default 7).")
                )
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val days = arguments.optInt("days_ahead", 7)
                val res = alarmAndCalendarManager.getUpcomingCalendarEvents(days)
                return if (res.isSuccess) {
                    val events = res.getOrThrow()
                    if (events.isEmpty()) {
                        ToolResult(true, "No upcoming calendar events found in the next $days days.")
                    } else {
                        val text = events.joinToString("\n\n") {
                            "📅 **${it.title}**\n• Time: ${it.startTime} - ${it.endTime}${if (!it.location.isNullOrBlank()) "\n• Location: ${it.location}" else ""}${if (!it.description.isNullOrBlank()) "\n• Notes: ${it.description}" else ""}"
                        }
                        ToolResult(true, "Found ${events.size} upcoming event(s):\n\n$text")
                    }
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to query calendar.")
                }
            }
        })

        register(object : DeviceTool {
            override val name = "create_calendar_event"
            override val description = "Opens Google Calendar with a pre-filled event to schedule an appointment or meeting."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "title" to mapOf("type" to "string", "description" to "Title of the event."),
                    "description" to mapOf("type" to "string", "description" to "Optional event description or notes."),
                    "location" to mapOf("type" to "string", "description" to "Optional location or room.")
                ),
                "required" to listOf("title")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val title = arguments.optString("title", "").trim()
                val desc = arguments.optString("description", "").ifBlank { null }
                val loc = arguments.optString("location", "").ifBlank { null }
                if (title.isEmpty()) return ToolResult(false, "title argument is required.")
                val res = alarmAndCalendarManager.createCalendarEvent(title, desc, loc)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to open calendar.")
            }
        })
    }

    // --- 6. Clipboard & Messaging (Feature 4) ---
    private fun registerClipboardAndMessagingTools() {
        register(object : DeviceTool {
            override val name = "get_clipboard_content"
            override val description = "Reads text currently copied to the device clipboard."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val text = clipboardAndMessagingManager.getClipboardText()
                return if (!text.isNullOrBlank()) {
                    ToolResult(true, "Clipboard text:\n\"$text\"")
                } else {
                    ToolResult(true, "Device clipboard is currently empty.")
                }
            }
        })

        register(object : DeviceTool {
            override val name = "set_clipboard_content"
            override val description = "Copies text or notes to the device clipboard."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "text" to mapOf("type" to "string", "description" to "The text to copy to clipboard.")
                ),
                "required" to listOf("text")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val text = arguments.optString("text", "")
                if (text.isEmpty()) return ToolResult(false, "text argument is required.")
                val res = clipboardAndMessagingManager.setClipboardText(text)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to set clipboard.")
            }
        })

        register(object : DeviceTool {
            override val name = "draft_message"
            override val description = "Opens WhatsApp or SMS with a pre-filled message draft to send to someone."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "app_name" to mapOf("type" to "string", "description" to "'whatsapp' or 'sms'."),
                    "text" to mapOf("type" to "string", "description" to "Message text to send."),
                    "recipient" to mapOf("type" to "string", "description" to "Optional phone number or contact identifier.")
                ),
                "required" to listOf("app_name", "text")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val app = arguments.optString("app_name", "sms")
                val text = arguments.optString("text", "")
                val recipient = arguments.optString("recipient", "").ifBlank { null }
                if (text.isEmpty()) return ToolResult(false, "text argument is required.")
                val res = clipboardAndMessagingManager.draftMessage(app, text, recipient)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to draft message.")
            }
        })
    }

    // --- 7. Automated Routines & Diagnostics (Feature 5) ---
    private fun registerRoutinesAndDiagnosticsTools() {
        register(object : DeviceTool {
            override val name = "execute_routine"
            override val description = "Executes an automated smart phone routine: 'bedtime', 'focus' (study), or 'morning'."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "routine_name" to mapOf(
                        "type" to "string",
                        "description" to "Name of the routine: 'bedtime', 'focus', or 'morning'."
                    )
                ),
                "required" to listOf("routine_name")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val routine = arguments.optString("routine_name", "").trim()
                if (routine.isEmpty()) return ToolResult(false, "routine_name argument is required.")
                val res = routinesAndDiagnosticsManager.executeRoutine(routine)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to execute routine.")
            }
        })

        register(object : DeviceTool {
            override val name = "run_device_diagnostics"
            override val description = "Runs a comprehensive health check on phone RAM, storage, battery, OS, and network."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val res = routinesAndDiagnosticsManager.runDiagnostics()
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Diagnostics failed.")
            }
        })

        register(object : DeviceTool {
            override val name = "export_chat_history"
            override val description = "Exports the entire current conversation history to a formatted Markdown file in authorized folders."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val action = exportChatAction
                return if (action != null) {
                    val res = action()
                    if (res.isSuccess) ToolResult(true, res.getOrThrow())
                    else ToolResult(false, res.exceptionOrNull()?.message ?: "Export failed.")
                } else {
                    ToolResult(false, "Export action is not initialized.")
                }
            }
        })
    }

    // --- 8. System Settings Tools ---
    private fun registerSettingsTools() {
        fun makeSettingTool(toolName: String, desc: String, action: () -> Result<String>) {
            register(object : DeviceTool {
                override val name = toolName
                override val description = desc
                override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

                override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                    val res = action()
                    return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                    else ToolResult(false, res.exceptionOrNull()?.message ?: "Unable to open setting.")
                }
            })
        }

        makeSettingTool("open_wifi_settings", "Opens Android Wi-Fi settings.") { systemSettingsManager.openWifiSettings() }
        makeSettingTool("open_bluetooth_settings", "Opens Android Bluetooth settings.") { systemSettingsManager.openBluetoothSettings() }
        makeSettingTool("open_display_settings", "Opens Android Display & Brightness settings.") { systemSettingsManager.openDisplaySettings() }
        makeSettingTool("open_sound_settings", "Opens Android Sound & Vibration settings.") { systemSettingsManager.openSoundSettings() }
        makeSettingTool("open_battery_settings", "Opens Android Battery and Power settings.") { systemSettingsManager.openBatterySettings() }
        makeSettingTool("open_location_settings", "Opens Android Location settings.") { systemSettingsManager.openLocationSettings() }
        makeSettingTool("open_accessibility_settings", "Opens Android Accessibility settings.") { systemSettingsManager.openAccessibilitySettings() }
        makeSettingTool("open_app_notification_settings", "Opens Android Notification settings.") { systemSettingsManager.openNotificationSettings() }
        makeSettingTool("control_power_saver", "Opens Android Battery Saver and Power Optimization controls.") { systemSettingsManager.openPowerSaverSettings() }
        makeSettingTool("open_wifi_panel", "Opens the Android quick Wi-Fi slice/panel toggle.") { systemSettingsManager.openWifiPanel() }
        makeSettingTool("open_bluetooth_panel", "Opens the Android quick Bluetooth slice/panel toggle.") { systemSettingsManager.openBluetoothPanel() }
        makeSettingTool("open_volume_panel", "Opens the Android quick volume slider panel.") { systemSettingsManager.openVolumePanel() }
        makeSettingTool("open_hotspot_settings", "Opens Android Portable Hotspot and Tethering settings.") { systemSettingsManager.openHotspotSettings() }
    }

    // --- 9. Audio Management Tools ---
    private fun registerAudioTools() {
        register(object : DeviceTool {
            override val name = "get_volume_levels"
            override val description = "Retrieves current volume percentage levels, ringer mode (Silent/Vibrate/Normal), and Do Not Disturb status."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val vol = audioControlManager.getVolumeLevels()
                val dndText = if (vol.isDndActive) "ON" else "OFF"
                return ToolResult(
                    true,
                    "Current Audio & Sound Profile:\n• Ringer Mode: ${vol.ringerMode}\n• Do Not Disturb: $dndText\n• Media: ${vol.mediaPercent}%\n• Ring: ${vol.ringPercent}%\n• Notifications: ${vol.notificationPercent}%\n• Alarm: ${vol.alarmPercent}%"
                )
            }
        })

        register(object : DeviceTool {
            override val name = "set_media_volume"
            override val description = "Adjusts the media audio volume (0 to 100 percent)."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "level_percent" to mapOf("type" to "integer", "description" to "Volume level from 0 (mute) to 100 (maximum).")
                ),
                "required" to listOf("level_percent")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val level = arguments.optInt("level_percent", -1)
                if (level < 0) return ToolResult(false, "level_percent argument (0-100) is required.")
                val res = audioControlManager.setMediaVolume(level)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to set volume.")
            }
        })

        register(object : DeviceTool {
            override val name = "set_smart_dnd"
            override val description = "Turns Smart Do Not Disturb (DND) ON or OFF. Priority-only allows alarms and priority callers."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "enable" to mapOf("type" to "boolean", "description" to "True to enable DND, false to disable."),
                    "priority_only" to mapOf("type" to "boolean", "description" to "True for priority alerts only, false for total silence.")
                ),
                "required" to listOf("enable")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val enable = arguments.optBoolean("enable", true)
                val priorityOnly = arguments.optBoolean("priority_only", false)
                val res = audioControlManager.setSmartDnd(enable, priorityOnly)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to toggle DND.")
            }
        })

        register(object : DeviceTool {
            override val name = "set_audio_profile"
            override val description = "Activates a smart audio profile: 'normal', 'meeting', 'focus' (study), 'sleep' (bedtime), 'outdoor', or 'vibrate'."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "profile_name" to mapOf(
                        "type" to "string",
                        "description" to "Profile: 'normal', 'meeting', 'focus', 'sleep', 'outdoor', or 'vibrate'."
                    )
                ),
                "required" to listOf("profile_name")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val profile = arguments.optString("profile_name", "").trim()
                if (profile.isEmpty()) return ToolResult(false, "profile_name argument is required.")
                val res = audioControlManager.applyAudioProfile(profile)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to apply audio profile.")
            }
        })

        register(object : DeviceTool {
            override val name = "set_ringer_mode"
            override val description = "Sets device ringer to 'silent', 'vibrate', or 'normal'."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "mode" to mapOf("type" to "string", "description" to "'silent', 'vibrate', or 'normal'.")
                ),
                "required" to listOf("mode")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val mode = arguments.optString("mode", "").trim()
                if (mode.isEmpty()) return ToolResult(false, "mode argument is required.")
                val res = audioControlManager.setRingerMode(mode)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to set ringer mode.")
            }
        })
    }

    // --- 10. Device Telemetry Tools ---
    private fun registerTelemetryTools() {
        register(object : DeviceTool {
            override val name = "get_battery_status"
            override val description = "Checks current battery percentage, charging state, charger type, and power save mode."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val b = deviceTelemetryManager.getBatteryStatus()
                val chargingText = if (b.isCharging) "Charging (${b.chargingSource})" else "Not charging"
                val powerSaveText = if (b.powerSaveMode) "Power Saver is ON" else "Power Saver is OFF"
                return ToolResult(
                    true,
                    "Battery Status:\n• Level: ${b.percentage}%\n• State: $chargingText\n• Mode: $powerSaveText"
                )
            }
        })

        register(object : DeviceTool {
            override val name = "get_device_storage_information"
            override val description = "Checks device internal storage capacity, free space, and usage percentage."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val s = deviceTelemetryManager.getDeviceStorageInfo()
                return ToolResult(
                    true,
                    "Device Internal Storage:\n• Total: ${s.totalGb} GB\n• Used: ${s.usedGb} GB (${s.usedPercent}%)\n• Available: ${s.freeGb} GB free"
                )
            }
        })

        register(object : DeviceTool {
            override val name = "get_network_status"
            override val description = "Checks active network connectivity (Wi-Fi, Cellular, or Offline)."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val net = deviceTelemetryManager.getNetworkStatus()
                return ToolResult(true, "Network Status: $net")
            }
        })

        register(object : DeviceTool {
            override val name = "get_current_time"
            override val description = "Returns the current local device time."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                return ToolResult(true, "Current local time: ${deviceTelemetryManager.getCurrentTime()}")
            }
        })

        register(object : DeviceTool {
            override val name = "get_current_date"
            override val description = "Returns the current date."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                return ToolResult(true, "Today's date: ${deviceTelemetryManager.getCurrentDate()}")
            }
        })
    }

    // --- 11. Utility Tools ---
    private fun registerUtilityTools() {
        register(object : DeviceTool {
            override val name = "open_url"
            override val description = "Opens a web link or URL in the default Android web browser."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "url" to mapOf("type" to "string", "description" to "The web URL to open (e.g. 'https://github.com').")
                ),
                "required" to listOf("url")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val url = arguments.optString("url", "").trim()
                if (url.isEmpty()) return ToolResult(false, "url argument is required.")
                val res = systemSettingsManager.openUrl(url)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Could not open URL.")
            }
        })

        register(object : DeviceTool {
            override val name = "open_system_setting"
            override val description = "Opens ANY specific Android setting screen by name: 'wifi', 'bluetooth', 'display', 'sound', 'battery', 'storage', 'apps', 'notifications', 'location', 'accessibility', 'security', 'biometrics', 'developer', 'date_time', 'airplane', 'nfc', 'network', 'device_info', 'privacy'."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "setting_name" to mapOf("type" to "string", "description" to "The setting ID or keyword (e.g. 'wifi', 'storage', 'apps', 'battery', 'developer').")
                ),
                "required" to listOf("setting_name")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val name = arguments.optString("setting_name", "")
                val res = systemSettingsManager.openSettingById(name)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Unable to open setting.")
            }
        })
    }

    // --- 12. Live Web Search Tool ---
    private fun registerWebSearchTools() {
        register(object : DeviceTool {
            override val name = "search_web"
            override val description = "Performs a live web search to look up current information, articles, or answers."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "query" to mapOf("type" to "string", "description" to "Search query to look up on the web.")
                ),
                "required" to listOf("query")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val query = arguments.optString("query", "").trim()
                if (query.isEmpty()) return ToolResult(false, "query argument is required.")
                val manager = webSearchManager
                return if (manager != null) {
                    val res = manager.searchWeb(query)
                    if (res.isSuccess) ToolResult(true, res.getOrThrow())
                    else ToolResult(false, res.exceptionOrNull()?.message ?: "Web search failed.")
                } else {
                    ToolResult(false, "WebSearchManager is not initialized.")
                }
            }
        })
    }

    // --- 13. Math Calculation Tool ---
    private fun registerMathTools() {
        register(object : DeviceTool {
            override val name = "calculate_math"
            override val description = "Evaluates high-precision arithmetic, trigonometric, algebraic formulas, or powers locally."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "expression" to mapOf("type" to "string", "description" to "Math expression to evaluate (e.g. 'sqrt(144) + 2^4', '25 * 18').")
                ),
                "required" to listOf("expression")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val expr = arguments.optString("expression", "").trim()
                if (expr.isEmpty()) return ToolResult(false, "expression argument is required.")
                val manager = mathCalculatorManager ?: MathCalculatorManager()
                val res = manager.evaluate(expr)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Calculation error.")
            }
        })
    }

    // --- 14. Contacts & Phone Tools ---
    private fun registerPhoneTools() {
        register(object : DeviceTool {
            override val name = "dial_contact_by_name"
            override val description = "Dials a person by searching their name in your phonebook contacts and opening the dialer."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "contact_name" to mapOf("type" to "string", "description" to "The full or partial name of the contact to call (e.g. 'Mom', 'Alex', 'John').")
                ),
                "required" to listOf("contact_name")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val name = arguments.optString("contact_name", "").trim()
                if (name.isEmpty()) return ToolResult(false, "contact_name argument is required.")
                val manager = contactsAndPhoneManager ?: ContactsAndPhoneManager(context)
                val res = manager.dialContactByName(name)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Could not dial contact '$name'.")
            }
        })

        register(object : DeviceTool {
            override val name = "search_contacts"
            override val description = "Searches device contacts by name or phone number and returns matches."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "query" to mapOf("type" to "string", "description" to "Name or number to search.")
                ),
                "required" to listOf("query")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val query = arguments.optString("query", "").trim()
                val manager = contactsAndPhoneManager ?: ContactsAndPhoneManager(context)
                val res = manager.searchContacts(query)
                return if (res.isSuccess) {
                    val contacts = res.getOrThrow()
                    if (contacts.isEmpty()) {
                        ToolResult(true, "No contacts found matching '$query'.")
                    } else {
                        val text = contacts.joinToString("\n") { "• ${it.name}: ${it.number} [${it.type}]" }
                        ToolResult(true, "Found ${contacts.size} contact(s):\n$text")
                    }
                } else {
                    ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to search contacts.")
                }
            }
        })

        register(object : DeviceTool {
            override val name = "dial_phone_number"
            override val description = "Opens the Android phone dialer with a specified phone number pre-filled."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "phone_number" to mapOf("type" to "string", "description" to "The phone number to dial.")
                ),
                "required" to listOf("phone_number")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val phone = arguments.optString("phone_number", "").trim()
                if (phone.isEmpty()) return ToolResult(false, "phone_number argument is required.")
                val manager = contactsAndPhoneManager ?: ContactsAndPhoneManager(context)
                val res = manager.dialPhoneNumber(phone)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Could not open dialer.")
            }
        })

        register(object : DeviceTool {
            override val name = "open_contacts"
            override val description = "Opens the device contacts list."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val manager = contactsAndPhoneManager ?: ContactsAndPhoneManager(context)
                val res = manager.openContacts()
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Could not open contacts.")
            }
        })
    }

    // --- 15. Security Vault Tools ---
    private fun registerVaultTools() {
        register(object : DeviceTool {
            override val name = "save_vault_note"
            override val description = "Saves a private note, sensitive code, or memory into the DEV AI Security Vault."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "title" to mapOf("type" to "string", "description" to "Title of the note."),
                    "content" to mapOf("type" to "string", "description" to "Content to store privately.")
                ),
                "required" to listOf("content")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val title = arguments.optString("title", "Private Note")
                val content = arguments.optString("content", "")
                if (content.isEmpty()) return ToolResult(false, "content argument is required.")
                val manager = securityVaultManager ?: SecurityVaultManager(context)
                val res = manager.saveNote(title, content)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Could not save vault note.")
            }
        })

        register(object : DeviceTool {
            override val name = "get_vault_notes"
            override val description = "Lists notes stored in the DEV AI Security Vault."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val manager = securityVaultManager ?: SecurityVaultManager(context)
                val notes = manager.getAllNotes()
                return if (notes.isEmpty()) {
                    ToolResult(true, "Security Vault is currently empty.")
                } else {
                    val text = notes.joinToString("\n\n") { "🔒 **${it.title}**\n${it.content}" }
                    ToolResult(true, "DEV AI Vault Notes:\n$text")
                }
            }
        })
    }

    // --- 16. Daily Briefing Tool ---
    private fun registerDailyBriefingTools() {
        register(object : DeviceTool {
            override val name = "get_daily_briefing"
            override val description = "Generates a complete executive daily briefing including battery, storage, RAM, media volume, and developer quote."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val manager = dailyBriefingManager ?: DailyBriefingManager(deviceTelemetryManager, audioControlManager)
                val text = manager.generateBriefing()
                return ToolResult(true, text)
            }
        })
    }

    // --- 17. App Usage & Digital Wellbeing Auditor ---
    private fun registerAppUsageAuditorTools() {
        register(object : DeviceTool {
            override val name = "audit_digital_wellbeing"
            override val description = "Audits device screen time, top used apps, and produces a Digital Wellbeing report with actionable wellness recommendations."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "days" to mapOf("type" to "integer", "description" to "Number of days of usage history to audit (default 1).")
                )
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val days = arguments.optInt("days", 1)
                val auditor = appUsageAuditorManager ?: AppUsageAuditorManager(context)
                val audit = auditor.auditDigitalWellbeing(days)

                if (!audit.isPermissionGranted) {
                    return ToolResult(
                        false,
                        "Usage Access permission is required to audit screen time. Please call 'open_usage_access_settings' to grant permission."
                    )
                }

                val sb = StringBuilder()
                sb.append("📱 **Digital Wellbeing & App Usage Audit** (Past $days day(s)):\n\n")
                sb.append("• **Total Screen Time:** ${audit.formattedTotalScreenTime}\n")
                sb.append("• **Summary:** ${audit.auditSummary}\n\n")
                if (audit.topApps.isNotEmpty()) {
                    sb.append("📊 **Top Used Applications:**\n")
                    for ((i, app) in audit.topApps.withIndex()) {
                        sb.append("${i + 1}. **${app.appName}**: ${app.formattedDuration} (${app.percentageOfTotal}% of screen time)\n")
                    }
                    sb.append("\n")
                }
                sb.append("💡 **Recommendation:** ${audit.recommendation}")
                return ToolResult(true, sb.toString())
            }
        })

        register(object : DeviceTool {
            override val name = "open_usage_access_settings"
            override val description = "Opens Android Usage Access Settings so the user can grant permission for app usage and screen time auditing."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val auditor = appUsageAuditorManager ?: AppUsageAuditorManager(context)
                val res = auditor.openUsageAccessSettings()
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to open usage settings.")
            }
        })
    }

    // --- 18. Notification Reader Tools ---
    private fun registerNotificationReaderTools() {
        register(object : DeviceTool {
            override val name = "check_notifications"
            override val description = "Inspects active and recent notifications from specific apps (e.g. 'Slack', 'WhatsApp', 'Messages', 'Gmail', 'Discord') or filters by message text."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "app_name" to mapOf("type" to "string", "description" to "Optional app name filter (e.g. 'Slack', 'WhatsApp', 'Messages', 'Gmail')."),
                    "keyword" to mapOf("type" to "string", "description" to "Optional keyword filter to search in message text or title.")
                )
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val app = arguments.optString("app_name", "").ifBlank { null }
                val keyword = arguments.optString("keyword", "").ifBlank { null }
                val reader = notificationReaderManager ?: NotificationReaderManager(context)
                val res = reader.checkNotifications(app, keyword)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to read notifications.")
            }
        })

        register(object : DeviceTool {
            override val name = "open_notification_access_settings"
            override val description = "Opens Android Notification Access Settings so user can enable DEV AI Notification Listener."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val reader = notificationReaderManager ?: NotificationReaderManager(context)
                val res = reader.openNotificationListenerSettings()
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to open settings.")
            }
        })
    }

    // --- 19. Scheduled Automation Tools ---
    private fun registerScheduledAutomationTools() {
        register(object : DeviceTool {
            override val name = "schedule_automation_action"
            override val description = "Schedules an automated action, reminder alert, or smart routine after a specific delay in minutes."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "title" to mapOf("type" to "string", "description" to "Descriptive title of the automation."),
                    "action_type" to mapOf(
                        "type" to "string",
                        "description" to "'routine' (bedtime/focus/morning), 'audio_profile' (silent/vibrate/normal), 'flashlight' (turn_off/turn_on), or 'alert'."
                    ),
                    "payload" to mapOf("type" to "string", "description" to "Parameter for the action (e.g. 'bedtime', 'silent', 'turn_off', or reminder message text)."),
                    "delay_minutes" to mapOf("type" to "integer", "description" to "Delay in minutes before executing (e.g. 5, 15, 30, 60).")
                ),
                "required" to listOf("title", "action_type", "payload", "delay_minutes")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val title = arguments.optString("title", "").trim()
                val actionType = arguments.optString("action_type", "alert")
                val payload = arguments.optString("payload", "")
                val delay = arguments.optInt("delay_minutes", 10)
                if (title.isEmpty()) return ToolResult(false, "title argument is required.")
                if (delay <= 0) return ToolResult(false, "delay_minutes must be > 0.")

                val manager = scheduledAutomationManager ?: ScheduledAutomationManager(context)
                val res = manager.scheduleAutomation(title, actionType, payload, delay)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to schedule automation.")
            }
        })

        register(object : DeviceTool {
            override val name = "list_scheduled_automations"
            override val description = "Lists all currently pending scheduled automations."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val manager = scheduledAutomationManager ?: ScheduledAutomationManager(context)
                val list = manager.listScheduledAutomations()
                return if (list.isEmpty()) {
                    ToolResult(true, "There are currently no pending scheduled automations.")
                } else {
                    val formatted = list.joinToString("\n\n") {
                        "⏱️ **${it.title}**\n• Action: ${it.actionType} (${it.payload})\n• Scheduled Time: ${it.formattedTime}\n• ID: `${it.id}`"
                    }
                    ToolResult(true, "Pending Scheduled Automations (${list.size}):\n\n$formatted")
                }
            }
        })

        register(object : DeviceTool {
            override val name = "cancel_scheduled_automation"
            override val description = "Cancels a scheduled automation by its ID."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "automation_id" to mapOf("type" to "string", "description" to "The ID of the scheduled automation to cancel.")
                ),
                "required" to listOf("automation_id")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val id = arguments.optString("automation_id", "").trim()
                if (id.isEmpty()) return ToolResult(false, "automation_id argument is required.")
                val manager = scheduledAutomationManager ?: ScheduledAutomationManager(context)
                val res = manager.cancelAutomation(id)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to cancel automation.")
            }
        })
    }

    // --- 20. Voice Customization Tools ---
    private fun registerVoiceCustomizationTools() {
        register(object : DeviceTool {
            override val name = "list_available_voices"
            override val description = "Lists available Text-To-Speech (TTS) voices installed on the device."
            override val parameterSchema = mapOf("type" to "object", "properties" to emptyMap<String, Any>())

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val manager = voiceAssistantManager
                if (manager == null) return ToolResult(false, "Voice assistant manager is not initialized.")
                val voices = manager.getAvailableVoices()
                return if (voices.isEmpty()) {
                    ToolResult(true, "No alternate TTS voices detected on this device. Using default Android system voice.")
                } else {
                    val text = voices.take(20).joinToString("\n") { "• ${it.displayName} [ID: ${it.name}]" }
                    ToolResult(true, "Found ${voices.size} TTS Voice(s):\n$text")
                }
            }
        })

        register(object : DeviceTool {
            override val name = "set_custom_voice"
            override val description = "Sets the assistant speaking voice by voice name or ID."
            override val parameterSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "voice_name" to mapOf("type" to "string", "description" to "The name or ID of the desired voice.")
                ),
                "required" to listOf("voice_name")
            )

            override suspend fun execute(context: Context, arguments: JSONObject): ToolResult {
                val voiceName = arguments.optString("voice_name", "").trim()
                if (voiceName.isEmpty()) return ToolResult(false, "voice_name argument is required.")
                val manager = voiceAssistantManager
                if (manager == null) return ToolResult(false, "Voice assistant manager is not initialized.")
                val res = manager.setCustomVoice(voiceName)
                return if (res.isSuccess) ToolResult(true, res.getOrThrow())
                else ToolResult(false, res.exceptionOrNull()?.message ?: "Failed to set voice.")
            }
        })
    }
}
