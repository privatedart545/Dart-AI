package com.example.platform

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class CalendarEventItem(
    val id: Long,
    val title: String,
    val description: String?,
    val startTime: String,
    val endTime: String,
    val location: String?
)

class AlarmAndCalendarManager(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    // --- ALARM ENHANCEMENTS ---

    fun showAlarms(): Result<String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened Clock application showing all configured alarms.")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to open alarms: ${e.localizedMessage}"))
        }
    }

    fun modifyOrSetAlarm(
        hour: Int,
        minute: Int,
        message: String? = null,
        days: ArrayList<Int>? = null,
        skipUi: Boolean = false
    ): Result<String> {
        if (hour !in 0..23 || minute !in 0..59) {
            return Result.failure(Exception("Invalid time: $hour:$minute. Hour must be 0-23, minute 0-59."))
        }

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message ?: "DEV AI Alarm")
                putExtra(AlarmClock.EXTRA_SKIP_UI, skipUi)
                if (days != null && days.isNotEmpty()) {
                    putIntegerArrayListExtra(AlarmClock.EXTRA_DAYS, days)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(String.format(Locale.getDefault(), "Alarm configured/updated for %02d:%02d (%s).", hour, minute, message ?: "DEV AI"))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to modify or set alarm: ${e.localizedMessage}"))
        }
    }

    fun dismissAlarm(label: String? = null): Result<String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_DISMISS_ALARM).apply {
                if (!label.isNullOrBlank()) {
                    putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_LABEL)
                    putExtra(AlarmClock.EXTRA_MESSAGE, label)
                }
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Dismissed active or ringing alarm${if (!label.isNullOrBlank()) " matching '$label'" else ""}.")
        } catch (e: Exception) {
            Result.failure(Exception("Could not dismiss alarm: ${e.localizedMessage}"))
        }
    }

    fun setTimer(durationSeconds: Int, label: String?): Result<String> {
        if (durationSeconds <= 0) return Result.failure(Exception("Duration must be greater than 0 seconds."))

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, durationSeconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label ?: "DEV AI Timer")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Timer set for $durationSeconds seconds ($label).")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to set timer: ${e.localizedMessage}"))
        }
    }

    fun setAlarm(hour: Int, minute: Int, message: String?): Result<String> {
        return modifyOrSetAlarm(hour, minute, message, skipUi = false)
    }

    // --- DIRECT CALENDAR & REMINDER SCHEDULING ---

    private fun hasCalendarPermissions(): Boolean {
        val write = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        return write && read
    }

    private fun getPrimaryCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )
            var fallbackId: Long? = null
            while (cursor != null && cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val isPrimary = if (cursor.getInt(1) == 1) true else false
                if (isPrimary) return id
                if (fallbackId == null) fallbackId = id
            }
            fallbackId
        } catch (_: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

    fun scheduleCalendarEventDirectly(
        title: String,
        beginTimeMillis: Long,
        endTimeMillis: Long? = null,
        description: String? = null,
        location: String? = null,
        reminderMinutesBefore: Int = 15
    ): Result<String> {
        if (!hasCalendarPermissions()) {
            // Graceful fallback: Open intent so user can save immediately without failing
            val intentResult = createCalendarEvent(title, description, location, beginTimeMillis, endTimeMillis)
            return Result.success("Calendar permission not granted for background write. $intentResult (Grant Calendar permission in Android Settings for instant direct scheduling).")
        }

        return try {
            val calId = getPrimaryCalendarId() ?: 1L
            val endMillis = endTimeMillis ?: (beginTimeMillis + 60 * 60 * 1000) // Default 1 hour

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, beginTimeMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description ?: "Scheduled by DEV AI")
                put(CalendarContract.Events.CALENDAR_ID, calId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                if (!location.isNullOrBlank()) {
                    put(CalendarContract.Events.EVENT_LOCATION, location)
                }
            }

            val uri: Uri? = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            if (uri == null) {
                return Result.failure(Exception("Failed to insert event into device calendar."))
            }

            val eventId = ContentUris.parseId(uri)

            // Add reminder notification if requested
            if (reminderMinutesBefore >= 0) {
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.MINUTES, reminderMinutesBefore)
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }

            val sdf = SimpleDateFormat("EEEE, MMM d, yyyy 'at' hh:mm a", Locale.getDefault())
            val dateFormatted = sdf.format(Date(beginTimeMillis))
            Result.success("✅ Scheduled reminder/event '$title' directly for $dateFormatted (Reminder set $reminderMinutesBefore min prior) without opening the Calendar app.")
        } catch (e: Exception) {
            Result.failure(Exception("Error scheduling calendar event directly: ${e.localizedMessage}"))
        }
    }

    fun getUpcomingCalendarEvents(daysAhead: Int = 7): Result<List<CalendarEventItem>> {
        if (!hasCalendarPermissions()) {
            return Result.failure(Exception("Calendar read permission required. Please grant Calendar permission in App Info."))
        }

        val startMillis = System.currentTimeMillis()
        val endMillis = startMillis + (daysAhead.toLong() * 24 * 60 * 60 * 1000)

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION
        )

        val items = mutableListOf<CalendarEventItem>()
        val sdf = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault())

        return try {
            contentResolver.query(builder.build(), projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
                while (cursor.moveToNext() && items.size < 20) {
                    val id = cursor.getLong(0)
                    val title = cursor.getString(1) ?: "Untitled Event"
                    val desc = cursor.getString(2)
                    val begin = cursor.getLong(3)
                    val end = cursor.getLong(4)
                    val loc = cursor.getString(5)

                    items.add(
                        CalendarEventItem(
                            id = id,
                            title = title,
                            description = desc,
                            startTime = sdf.format(Date(begin)),
                            endTime = sdf.format(Date(end)),
                            location = loc
                        )
                    )
                }
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to read calendar events: ${e.localizedMessage}"))
        }
    }

    fun createCalendarEvent(
        title: String,
        description: String? = null,
        location: String? = null,
        beginTimeMillis: Long? = null,
        endTimeMillis: Long? = null
    ): Result<String> {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                if (!description.isNullOrBlank()) putExtra(CalendarContract.Events.DESCRIPTION, description)
                if (!location.isNullOrBlank()) putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                if (beginTimeMillis != null) putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTimeMillis)
                if (endTimeMillis != null) putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTimeMillis)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened Calendar with pre-filled event: '$title'.")
        } catch (e: Exception) {
            Result.failure(Exception("Could not open Calendar: ${e.localizedMessage}"))
        }
    }
}
