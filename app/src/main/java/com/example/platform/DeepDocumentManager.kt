package com.example.platform

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.example.data.preferences.DevAiPreferences
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

class DeepDocumentManager(
    private val context: Context,
    private val safFileManager: SafFileManager
) {

    private val contentResolver: ContentResolver = context.contentResolver
    private val preferences = DevAiPreferences(context)

    fun readFileContent(fileNameOrUri: String, maxChars: Int = 8000): Result<String> {
        val infoRes = safFileManager.getFileInfo(fileNameOrUri)
        val info = infoRes.getOrElse { return Result.failure(it) }

        if (info.isDirectory) {
            return Result.failure(Exception("'${info.name}' is a directory, not a document file."))
        }

        return try {
            val uri = Uri.parse(info.uriString)
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    val buffer = StringBuilder()
                    var line = reader.readLine()
                    var totalChars = 0

                    while (line != null && totalChars < maxChars) {
                        buffer.append(line).append("\n")
                        totalChars += line.length + 1
                        line = reader.readLine()
                    }

                    val truncatedNotice = if (line != null) "\n[... Document truncated at $maxChars characters ...]" else ""
                    Result.success("Content of '${info.name}':\n```\n${buffer.toString().trimEnd()}$truncatedNotice\n```")
                }
            } ?: Result.failure(Exception("Could not open input stream for '${info.name}'."))
        } catch (e: Exception) {
            Result.failure(Exception("Error reading '${info.name}': ${e.localizedMessage}"))
        }
    }

    fun searchFileContent(query: String, extension: String? = null, maxMatches: Int = 5): Result<String> {
        if (query.isBlank()) return Result.failure(Exception("Query cannot be empty."))

        val filesRes = safFileManager.searchFiles(query = null, extension = extension, maxResults = 30)
        val files = filesRes.getOrElse { return Result.failure(it) }

        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        val textExtensions = setOf("txt", "md", "json", "csv", "py", "kt", "xml", "html", "js", "ts", "log", "yaml", "yml")

        val matchingFiles = mutableListOf<String>()

        for (file in files) {
            if (file.isDirectory) continue
            val ext = file.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
            if (extension.isNullOrBlank() && ext !in textExtensions) continue

            try {
                val uri = Uri.parse(file.uriString)
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                        var lineNum = 0
                        var line = reader.readLine()
                        while (line != null && matchingFiles.size < maxMatches) {
                            lineNum++
                            if (line.lowercase(Locale.ROOT).contains(cleanQuery)) {
                                matchingFiles.add("• ${file.name} (line $lineNum): \"${line.trim().take(80)}\"")
                                break // 1 match per file is sufficient for listing
                            }
                            line = reader.readLine()
                        }
                    }
                }
            } catch (_: Exception) {}

            if (matchingFiles.size >= maxMatches) break
        }

        return if (matchingFiles.isEmpty()) {
            Result.success("No documents in authorized folders contain the text '$query'.")
        } else {
            Result.success("Found matching text inside ${matchingFiles.size} document(s):\n${matchingFiles.joinToString("\n")}")
        }
    }
}
