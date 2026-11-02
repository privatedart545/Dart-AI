package com.example.platform

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import java.util.Locale

data class ContactEntry(
    val name: String,
    val number: String,
    val type: String
)

class ContactsAndPhoneManager(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    private fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun searchContacts(query: String): Result<List<ContactEntry>> {
        if (!hasContactsPermission()) {
            return Result.failure(Exception("Contacts read permission is required. Please grant Contacts permission in Settings."))
        }

        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE
        )

        val results = mutableListOf<ContactEntry>()
        var cursor: Cursor? = null

        try {
            cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            while (cursor != null && cursor.moveToNext() && results.size < 20) {
                val name = cursor.getString(0) ?: "Unknown"
                val number = cursor.getString(1) ?: ""
                val typeInt = cursor.getInt(2)
                val typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                    context.resources,
                    typeInt,
                    "Phone"
                ).toString()

                if (cleanQuery.isBlank() || name.lowercase(Locale.ROOT).contains(cleanQuery) || number.contains(cleanQuery)) {
                    results.add(ContactEntry(name, number, typeLabel))
                }
            }
            return Result.success(results.distinctBy { "${it.name}_${it.number}" })
        } catch (e: Exception) {
            return Result.failure(Exception("Error searching contacts: ${e.localizedMessage}"))
        } finally {
            cursor?.close()
        }
    }

    fun dialContactByName(name: String): Result<String> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.failure(Exception("Contact name cannot be empty."))

        val searchResult = searchContacts(trimmed)
        if (searchResult.isFailure) {
            // If permission missing or failed, check if input looks like a phone number
            if (trimmed.any { it.isDigit() }) {
                return dialPhoneNumber(trimmed)
            }
            // Fallback: open contacts search
            val fallback = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            return Result.success("Contacts permission required for automatic dialing. Opened Contacts list for '$trimmed'.")
        }

        val contacts = searchResult.getOrThrow()
        if (contacts.isEmpty()) {
            // Try dialing directly if it happens to be a numeric string
            if (trimmed.any { it.isDigit() }) {
                return dialPhoneNumber(trimmed)
            }
            return Result.failure(Exception("Could not find any contact matching '$trimmed' in device phonebook."))
        }

        // Exact match preferred, otherwise first match
        val bestMatch = contacts.firstOrNull { it.name.equals(trimmed, ignoreCase = true) } ?: contacts.first()
        val cleanNumber = bestMatch.number.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }

        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val info = if (contacts.size > 1) {
                " (Found ${contacts.size} matches: ${contacts.take(3).joinToString { "${it.name} [${it.type}]" }})"
            } else ""
            Result.success("Initiating dial for ${bestMatch.name} ($cleanNumber)$info.")
        } catch (e: Exception) {
            Result.failure(Exception("Could not open dialer: ${e.localizedMessage}"))
        }
    }

    fun dialPhoneNumber(phoneNumber: String): Result<String> {
        return try {
            val cleanNumber = phoneNumber.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
            if (cleanNumber.isBlank()) {
                return Result.failure(IllegalArgumentException("Invalid phone number provided."))
            }
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened phone dialer with number: $cleanNumber")
        } catch (e: Exception) {
            Result.failure(Exception("Could not launch phone dialer: ${e.localizedMessage}"))
        }
    }

    fun openContacts(): Result<String> {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened device contacts.")
        } catch (e: Exception) {
            Result.failure(Exception("Could not open contacts: ${e.localizedMessage}"))
        }
    }
}
