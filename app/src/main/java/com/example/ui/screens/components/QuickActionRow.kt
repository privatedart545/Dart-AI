package com.example.ui.screens.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class QuickAction(
    val label: String,
    val prompt: String,
    val icon: ImageVector,
    val tag: String
)

@Composable
fun QuickActionRow(
    onActionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        QuickAction("Check Slack", "Is there any message from Slack?", Icons.Default.Notifications, "quick_action_slack"),
        QuickAction("Screen Time", "Audit my digital wellbeing and screen time today", Icons.Default.Schedule, "quick_action_screen_time"),
        QuickAction("Show Alarms", "Show all my alarms", Icons.Default.Alarm, "quick_action_show_alarms"),
        QuickAction("Calendar Reminder", "Schedule a reminder for Project Sync tomorrow at 10 AM", Icons.Default.CalendarMonth, "quick_action_calendar"),
        QuickAction("Smart DND", "Apply Focus audio profile with Smart Do Not Disturb", Icons.Default.VolumeUp, "quick_action_dnd"),
        QuickAction("Call Contact", "Search and call John from contacts", Icons.Default.Phone, "quick_action_call_contact"),
        QuickAction("Briefing", "Give me my daily executive briefing", Icons.Default.WbSunny, "quick_action_briefing"),
        QuickAction("Move File", "Move file notes.txt to folder Archive", Icons.Default.DriveFileMove, "quick_action_move_file"),
        QuickAction("Flashlight", "Turn on flashlight", Icons.Default.FlashlightOn, "quick_action_flashlight"),
        QuickAction("Calculate", "Calculate sqrt(144) + (25 * 4)", Icons.Default.Calculate, "quick_action_calc"),
        QuickAction("Settings", "Open Android Settings Hub", Icons.Default.Settings, "quick_action_settings"),
        QuickAction("Battery", "Check my battery status and power saver", Icons.Default.BatteryChargingFull, "quick_action_battery")
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { action ->
            AssistChip(
                onClick = { onActionClick(action.prompt) },
                label = {
                    Text(
                        text = action.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.label,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                ),
                border = AssistChipDefaults.assistChipBorder(
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    borderWidth = 1.dp,
                    enabled = true
                ),
                modifier = Modifier.testTag(action.tag)
            )
        }
    }
}
