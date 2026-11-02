package com.example.platform

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VaultNote(
    val id: String,
    val title: String,
    val content: String,
    val timestamp: Long
)

class SecurityVaultManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dart_ai_security_vault", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NOTES_JSON = "vault_notes_json"
    }

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    fun getAllNotes(): List<VaultNote> {
        val raw = prefs.getString(KEY_NOTES_JSON, "[]") ?: "[]"
        val list = mutableListOf<VaultNote>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    VaultNote(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        content = obj.getString("content"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.timestamp }
    }

    fun saveNote(title: String, content: String): Result<String> {
        return try {
            val notes = getAllNotes().toMutableList()
            val newNote = VaultNote(
                id = "vault_${System.currentTimeMillis()}",
                title = title.ifBlank { "Encrypted Note" },
                content = content,
                timestamp = System.currentTimeMillis()
            )
            notes.add(0, newNote)
            saveList(notes)
            Result.success("Saved note '${newNote.title}' into Dart AI Security Vault.")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save vault note: ${e.localizedMessage}"))
        }
    }

    fun deleteNote(id: String): Result<String> {
        val notes = getAllNotes().filter { it.id != id }
        saveList(notes)
        return Result.success("Deleted note from Security Vault.")
    }

    private fun saveList(notes: List<VaultNote>) {
        val arr = JSONArray()
        for (n in notes) {
            val obj = JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("content", n.content)
                put("timestamp", n.timestamp)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_NOTES_JSON, arr.toString()).apply()
    }
}
