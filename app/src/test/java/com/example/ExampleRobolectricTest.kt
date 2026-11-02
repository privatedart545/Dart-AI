package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.agent.SystemPrompt
import com.example.agent.ToolRegistry
import com.example.platform.AlarmAndCalendarManager
import com.example.platform.AppLaunchManager
import com.example.platform.AudioControlManager
import com.example.platform.ClipboardAndMessagingManager
import com.example.platform.ContactsAndPhoneManager
import com.example.platform.DailyBriefingManager
import com.example.platform.DeepDocumentManager
import com.example.platform.DeviceTelemetryManager
import com.example.platform.FlashlightManager
import com.example.platform.MathCalculatorManager
import com.example.platform.RoutinesAndDiagnosticsManager
import com.example.platform.SafFileManager
import com.example.platform.SecurityVaultManager
import com.example.platform.SystemSettingsManager
import com.example.platform.WebSearchManager
import com.example.platform.AppLockManager
import com.example.platform.AppUsageAuditorManager
import com.example.platform.DisplayAndBrightnessManager
import com.example.platform.NotificationReaderManager
import com.example.platform.ScheduledAutomationManager
import com.example.platform.WakeWordManager
import com.example.voice.VoiceAssistantManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `verify app name resource`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DEV AI", appName)
    }

    @Test
    fun `verify system prompt branding and instructions`() {
        assertTrue(SystemPrompt.DEFAULT_PROMPT.contains("DEV AI"))
        assertTrue(SystemPrompt.DEFAULT_PROMPT.contains("DEV L KAKKOTH"))
        assertTrue(SystemPrompt.DEFAULT_PROMPT.contains("Never claim an action was completed unless confirmed by tool execution"))
    }

    @Test
    fun `verify tool registry contains required device tools`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val flashlightManager = FlashlightManager(context)
        val appLaunchManager = AppLaunchManager(context)
        val safFileManager = SafFileManager(context)
        val audioControlManager = AudioControlManager(context)
        val systemSettingsManager = SystemSettingsManager(context)
        val deviceTelemetryManager = DeviceTelemetryManager(context)
        val deepDocumentManager = DeepDocumentManager(context, safFileManager)
        val alarmAndCalendarManager = AlarmAndCalendarManager(context)
        val clipboardAndMessagingManager = ClipboardAndMessagingManager(context)
        val routinesAndDiagnosticsManager = RoutinesAndDiagnosticsManager(
            context,
            flashlightManager,
            audioControlManager,
            deviceTelemetryManager,
            safFileManager
        )
        val webSearchManager = WebSearchManager()
        val mathCalculatorManager = MathCalculatorManager()
        val contactsAndPhoneManager = ContactsAndPhoneManager(context)
        val securityVaultManager = SecurityVaultManager(context)
        val dailyBriefingManager = DailyBriefingManager(deviceTelemetryManager, audioControlManager)
        val appUsageAuditorManager = AppUsageAuditorManager(context)
        val notificationReaderManager = NotificationReaderManager(context)
        val scheduledAutomationManager = ScheduledAutomationManager(context)
        val voiceAssistantManager = VoiceAssistantManager(context)
        val preferences = com.example.data.preferences.DevAiPreferences(context)
        val displayAndBrightnessManager = DisplayAndBrightnessManager(context)
        val appLockManager = AppLockManager(context, preferences)
        val wakeWordManager = WakeWordManager(context, preferences)

        val registry = ToolRegistry(
            flashlightManager = flashlightManager,
            appLaunchManager = appLaunchManager,
            safFileManager = safFileManager,
            audioControlManager = audioControlManager,
            systemSettingsManager = systemSettingsManager,
            deviceTelemetryManager = deviceTelemetryManager,
            deepDocumentManager = deepDocumentManager,
            alarmAndCalendarManager = alarmAndCalendarManager,
            clipboardAndMessagingManager = clipboardAndMessagingManager,
            routinesAndDiagnosticsManager = routinesAndDiagnosticsManager,
            webSearchManager = webSearchManager,
            mathCalculatorManager = mathCalculatorManager,
            contactsAndPhoneManager = contactsAndPhoneManager,
            securityVaultManager = securityVaultManager,
            dailyBriefingManager = dailyBriefingManager,
            appUsageAuditorManager = appUsageAuditorManager,
            notificationReaderManager = notificationReaderManager,
            scheduledAutomationManager = scheduledAutomationManager,
            voiceAssistantManager = voiceAssistantManager,
            displayAndBrightnessManager = displayAndBrightnessManager,
            appLockManager = appLockManager,
            wakeWordManager = wakeWordManager,
            exportChatAction = { Result.success("Exported") }
        )

        assertNotNull(registry.getTool("turn_flashlight_on"))
        assertNotNull(registry.getTool("turn_flashlight_off"))
        assertNotNull(registry.getTool("open_app"))
        assertNotNull(registry.getTool("deep_link_app"))
        assertNotNull(registry.getTool("list_installed_apps"))
        assertNotNull(registry.getTool("search_files"))
        assertNotNull(registry.getTool("create_file"))
        assertNotNull(registry.getTool("create_file_in_folder"))
        assertNotNull(registry.getTool("move_file"))
        assertNotNull(registry.getTool("read_file_content"))
        assertNotNull(registry.getTool("search_file_content"))
        assertNotNull(registry.getTool("set_timer"))
        assertNotNull(registry.getTool("set_alarm"))
        assertNotNull(registry.getTool("show_alarms"))
        assertNotNull(registry.getTool("modify_or_set_alarm"))
        assertNotNull(registry.getTool("dismiss_alarm"))
        assertNotNull(registry.getTool("schedule_calendar_event_direct"))
        assertNotNull(registry.getTool("get_upcoming_calendar_events"))
        assertNotNull(registry.getTool("dial_contact_by_name"))
        assertNotNull(registry.getTool("search_contacts"))
        assertNotNull(registry.getTool("audit_digital_wellbeing"))
        assertNotNull(registry.getTool("check_notifications"))
        assertNotNull(registry.getTool("schedule_automation_action"))
        assertNotNull(registry.getTool("set_smart_dnd"))
        assertNotNull(registry.getTool("set_audio_profile"))
        assertNotNull(registry.getTool("control_power_saver"))
        assertNotNull(registry.getTool("open_wifi_panel"))
        assertNotNull(registry.getTool("open_bluetooth_panel"))
        assertNotNull(registry.getTool("open_volume_panel"))
        assertNotNull(registry.getTool("list_available_voices"))
        assertNotNull(registry.getTool("set_custom_voice"))
        assertNotNull(registry.getTool("get_display_status"))
        assertNotNull(registry.getTool("set_screen_brightness"))
        assertNotNull(registry.getTool("set_auto_brightness"))
        assertNotNull(registry.getTool("set_screen_timeout"))
        assertNotNull(registry.getTool("open_display_settings"))
        assertNotNull(registry.getTool("lock_app"))
        assertNotNull(registry.getTool("unlock_app"))
        assertNotNull(registry.getTool("list_locked_apps"))
        assertNotNull(registry.getTool("toggle_app_lock"))
        assertNotNull(registry.getTool("set_wake_phrase"))
        assertNotNull(registry.getTool("toggle_wake_word"))
        assertNotNull(registry.getTool("get_wake_word_status"))

        val apiTools = registry.getApiTools()
        assertTrue(apiTools.size >= 50)
    }

    @Test
    fun `verify display and brightness manager defaults`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = DisplayAndBrightnessManager(context)
        val status = manager.getDisplayStatus()
        assertTrue(status.brightnessPercent in 0..100)
        assertTrue(status.screenTimeoutSeconds >= 0)
    }

    @Test
    fun `verify app lock and custom wake word preferences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = com.example.data.preferences.DevAiPreferences(context)
        preferences.wakeWordPhrase = "Hey Dev"
        assertEquals("Hey Dev", preferences.wakeWordPhrase)

        preferences.appLockEnabled = true
        preferences.addLockedPackage("com.whatsapp")
        assertTrue(preferences.isPackageLocked("com.whatsapp"))
        preferences.removeLockedPackage("com.whatsapp")
        assertTrue(!preferences.isPackageLocked("com.whatsapp"))
    }
}

