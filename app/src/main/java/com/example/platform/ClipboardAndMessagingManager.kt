package com.example.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri

class ClipboardAndMessagingManager(private val context: Context) {

    private val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    fun getClipboardText(): String? {
        val clip = clipboardManager?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0)?.text?.toString()
            if (!text.isNullOrBlank()) return text
        }
        return null
    }

    fun setClipboardText(text: String): Result<String> {
        return try {
            val clip = ClipData.newPlainText("DEV AI", text)
            clipboardManager?.setPrimaryClip(clip)
            Result.success("Copied to clipboard: \"${text.take(60)}...\"")
        } catch (e: Exception) {
            Result.failure(Exception("Could not copy to clipboard: ${e.localizedMessage}"))
        }
    }

    fun draftMessage(app: String, text: String, recipient: String? = null): Result<String> {
        val cleanApp = app.lowercase()
        return try {
            if (cleanApp.contains("whatsapp")) {
                val cleanPhone = recipient?.replace(Regex("[^0-9+]"), "") ?: ""
                val url = if (cleanPhone.isNotEmpty()) {
                    "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(text)}"
                } else {
                    "https://api.whatsapp.com/send?text=${Uri.encode(text)}"
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Result.success("Opened WhatsApp with pre-filled message draft.")
            } else {
                // SMS
                val uri = if (!recipient.isNullOrBlank()) Uri.parse("smsto:$recipient") else Uri.parse("smsto:")
                val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                    putExtra("sms_body", text)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Result.success("Opened SMS messaging with pre-filled draft.")
            }
        } catch (e: Exception) {
            Result.failure(Exception("Unable to draft message: ${e.localizedMessage}"))
        }
    }
}
