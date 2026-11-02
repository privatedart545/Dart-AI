package com.example.platform

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import com.example.data.preferences.DevAiPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SafFileInfo(
    val name: String,
    val uriString: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val formattedSize: String,
    val mimeType: String?,
    val lastModified: String
)

class SafFileManager(private val context: Context) {

    private val preferences = DevAiPreferences(context)
    private val contentResolver: ContentResolver = context.contentResolver

    fun getAuthorizedFolders(): List<Pair<String, String>> {
        val uris = preferences.getAuthorizedFolders()
        val results = mutableListOf<Pair<String, String>>()
        for (u in uris) {
            try {
                val uri = Uri.parse(u)
                val docId = DocumentsContract.getTreeDocumentId(uri)
                val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, docId)
                val name = queryDisplayName(docUri) ?: "Folder (${uri.lastPathSegment ?: "Selected"})"
                results.add(Pair(u, name))
            } catch (_: Exception) {
                results.add(Pair(u, "Authorized Folder"))
            }
        }
        return results
    }

    fun addAuthorizedFolder(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {}
        preferences.addAuthorizedFolder(uri.toString())
    }

    fun removeAuthorizedFolder(uri: Uri) {
        try {
            contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {}
        preferences.removeAuthorizedFolder(uri.toString())
    }

    private fun queryDisplayName(uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                if (idx != -1) cursor.getString(idx) else null
            } else null
        } catch (_: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

    fun searchFiles(
        query: String?,
        extension: String? = null,
        targetFolderName: String? = null,
        maxResults: Int = 25
    ): Result<List<SafFileInfo>> {
        val authorizedUris = preferences.getAuthorizedFolders()
        if (authorizedUris.isEmpty()) {
            return Result.failure(
                Exception("No folders have been authorized yet. Please tap 'Authorize Folder' in Settings or Quick Actions so DEV AI can safely search your documents.")
            )
        }

        val results = mutableListOf<SafFileInfo>()
        val cleanQuery = query?.trim()?.lowercase(Locale.ROOT)
        val cleanExt = extension?.trim()?.removePrefix(".")?.lowercase(Locale.ROOT)

        for (uriString in authorizedUris) {
            try {
                val treeUri = Uri.parse(uriString)
                val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)

                if (!targetFolderName.isNullOrBlank()) {
                    val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
                    val rootName = queryDisplayName(rootDocUri)?.lowercase(Locale.ROOT) ?: ""
                    if (!rootName.contains(targetFolderName.lowercase(Locale.ROOT))) {
                        continue
                    }
                }

                traverseAndSearch(treeUri, rootDocId, cleanQuery, cleanExt, results, maxResults, currentDepth = 0)
                if (results.size >= maxResults) break
            } catch (_: Exception) {
                // Continue to next tree
            }
        }

        return Result.success(results)
    }

    private fun traverseAndSearch(
        treeUri: Uri,
        parentId: String,
        query: String?,
        extension: String?,
        results: MutableList<SafFileInfo>,
        maxResults: Int,
        currentDepth: Int
    ) {
        if (currentDepth > 6 || results.size >= maxResults) return

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(childrenUri, projection, null, null, null)
            while (cursor != null && cursor.moveToNext() && results.size < maxResults) {
                val docId = cursor.getString(0)
                val displayName = cursor.getString(1) ?: "Unnamed"
                val mimeType = cursor.getString(2) ?: "*/*"
                val sizeBytes = if (!cursor.isNull(3)) cursor.getLong(3) else 0L
                val lastModified = if (!cursor.isNull(4)) cursor.getLong(4) else 0L

                val isDir = mimeType == DocumentsContract.Document.MIME_TYPE_DIR

                if (isDir) {
                    traverseAndSearch(treeUri, docId, query, extension, results, maxResults, currentDepth + 1)
                } else {
                    val lowerName = displayName.lowercase(Locale.ROOT)
                    val matchesQuery = query.isNullOrBlank() || lowerName.contains(query)
                    val matchesExt = extension.isNullOrBlank() || lowerName.endsWith(".$extension")

                    if (matchesQuery && matchesExt) {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        results.add(
                            SafFileInfo(
                                name = displayName,
                                uriString = docUri.toString(),
                                isDirectory = false,
                                sizeBytes = sizeBytes,
                                formattedSize = formatBytes(sizeBytes),
                                mimeType = mimeType,
                                lastModified = formatTimestamp(lastModified)
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            cursor?.close()
        }
    }

    fun listFolderContents(folderUriString: String? = null): Result<List<SafFileInfo>> {
        val uriStr = folderUriString ?: preferences.getAuthorizedFolders().firstOrNull()
            ?: return Result.failure(Exception("No authorized folder found. Please select a folder first."))

        return try {
            val treeUri = Uri.parse(uriStr)
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )

            val items = mutableListOf<SafFileInfo>()
            contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val docId = cursor.getString(0)
                    val name = cursor.getString(1) ?: "Unnamed"
                    val mime = cursor.getString(2) ?: "*/*"
                    val size = if (!cursor.isNull(3)) cursor.getLong(3) else 0L
                    val lastMod = if (!cursor.isNull(4)) cursor.getLong(4) else 0L
                    val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)

                    items.add(
                        SafFileInfo(
                            name = name,
                            uriString = docUri.toString(),
                            isDirectory = isDir,
                            sizeBytes = size,
                            formattedSize = formatBytes(size),
                            mimeType = mime,
                            lastModified = formatTimestamp(lastMod)
                        )
                    )
                }
            }
            Result.success(items.sortedWith(compareBy<SafFileInfo> { !it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) }))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to list folder contents: ${e.localizedMessage}"))
        }
    }

    fun getFileInfo(fileNameOrUri: String): Result<SafFileInfo> {
        val trimmed = fileNameOrUri.trim()
        if (trimmed.startsWith("content://")) {
            val uri = Uri.parse(trimmed)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )
            return try {
                contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val name = cursor.getString(0) ?: "File"
                        val mime = cursor.getString(1) ?: "*/*"
                        val size = if (!cursor.isNull(2)) cursor.getLong(2) else 0L
                        val lastMod = if (!cursor.isNull(3)) cursor.getLong(3) else 0L
                        val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                        Result.success(
                            SafFileInfo(
                                name = name,
                                uriString = uri.toString(),
                                isDirectory = isDir,
                                sizeBytes = size,
                                formattedSize = formatBytes(size),
                                mimeType = mime,
                                lastModified = formatTimestamp(lastMod)
                            )
                        )
                    } else {
                        Result.failure(Exception("File document not found for URI: $trimmed"))
                    }
                } ?: Result.failure(Exception("Could not query document URI."))
            } catch (e: Exception) {
                Result.failure(Exception("Query error: ${e.localizedMessage}"))
            }
        }

        // Search in authorized folders
        val searchRes = searchFiles(query = trimmed, maxResults = 5)
        val matches = searchRes.getOrNull() ?: emptyList()
        val exact = matches.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
            ?: matches.firstOrNull()

        return if (exact != null) {
            Result.success(exact)
        } else {
            Result.failure(Exception("Could not find file '$fileNameOrUri' in any authorized folder."))
        }
    }

    fun openFile(fileNameOrUri: String): Result<String> {
        val infoRes = getFileInfo(fileNameOrUri)
        val info = infoRes.getOrElse { return Result.failure(it) }

        return try {
            val uri = Uri.parse(info.uriString)
            val mime = info.mimeType ?: resolveMimeType(info.name)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Result.success("Opening '${info.name}' with compatible viewer application.")
            } else {
                val chooser = Intent.createChooser(intent, "Open with DEV AI").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                Result.success("Opened app chooser to view '${info.name}'.")
            }
        } catch (e: Exception) {
            Result.failure(Exception("Unable to open file '${info.name}': ${e.localizedMessage}"))
        }
    }

    fun createFolder(folderName: String, parentUriString: String? = null): Result<String> {
        val uriStr = parentUriString ?: preferences.getAuthorizedFolders().firstOrNull()
            ?: return Result.failure(Exception("No authorized folder to create folder in."))

        return try {
            val treeUri = Uri.parse(uriStr)
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)

            val newDocUri = DocumentsContract.createDocument(
                contentResolver,
                parentDocUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                folderName
            )

            if (newDocUri != null) {
                Result.success("Successfully created folder '$folderName'.")
            } else {
                Result.failure(Exception("Failed to create folder '$folderName'."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error creating folder: ${e.localizedMessage}"))
        }
    }

    fun createFile(fileName: String, content: String, parentUriString: String? = null): Result<String> {
        val uriStr = parentUriString ?: preferences.getAuthorizedFolders().firstOrNull()
            ?: return Result.failure(Exception("No authorized folder available. Please authorize a folder in Settings first."))

        return try {
            val treeUri = Uri.parse(uriStr)
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)

            val mime = resolveMimeType(fileName).ifBlank { "text/plain" }
            val newDocUri = DocumentsContract.createDocument(
                contentResolver,
                parentDocUri,
                mime,
                fileName
            ) ?: return Result.failure(Exception("Storage provider refused file creation for '$fileName'."))

            contentResolver.openOutputStream(newDocUri)?.use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
            } ?: return Result.failure(Exception("Failed to write content to '$fileName'."))

            val bytesCount = content.toByteArray(Charsets.UTF_8).size
            Result.success("Successfully created file '$fileName' with $bytesCount bytes.")
        } catch (e: Exception) {
            Result.failure(Exception("Error creating file '$fileName': ${e.localizedMessage}"))
        }
    }

    fun findFolder(folderNameOrUri: String): Result<SafFileInfo> {
        val trimmed = folderNameOrUri.trim()
        if (trimmed.startsWith("content://")) {
            val infoRes = getFileInfo(trimmed)
            return if (infoRes.isSuccess && infoRes.getOrThrow().isDirectory) {
                infoRes
            } else if (infoRes.isSuccess) {
                Result.failure(Exception("URI is a file, not a directory."))
            } else infoRes
        }

        // Check authorized roots
        for (auth in getAuthorizedFolders()) {
            if (auth.second.equals(trimmed, ignoreCase = true) || auth.first.contains(trimmed, ignoreCase = true)) {
                val treeUri = Uri.parse(auth.first)
                val docId = DocumentsContract.getTreeDocumentId(treeUri)
                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                return Result.success(
                    SafFileInfo(
                        name = auth.second,
                        uriString = docUri.toString(),
                        isDirectory = true,
                        sizeBytes = 0L,
                        formattedSize = "Folder",
                        mimeType = DocumentsContract.Document.MIME_TYPE_DIR,
                        lastModified = "Authorized Root"
                    )
                )
            }
        }

        // Search subdirectories
        val list = listFolderContents(null).getOrNull() ?: emptyList()
        val match = list.firstOrNull { it.isDirectory && (it.name.equals(trimmed, ignoreCase = true) || it.name.contains(trimmed, ignoreCase = true)) }
        return if (match != null) {
            Result.success(match)
        } else {
            Result.failure(Exception("Folder '$folderNameOrUri' not found in authorized locations."))
        }
    }

    fun createFileInFolder(folderNameOrUri: String, fileName: String, content: String): Result<String> {
        val folderRes = findFolder(folderNameOrUri)
        val targetFolder = folderRes.getOrElse { return Result.failure(it) }

        return try {
            val parentDocUri = Uri.parse(targetFolder.uriString)
            val mime = resolveMimeType(fileName).ifBlank { "text/plain" }
            val newDocUri = DocumentsContract.createDocument(
                contentResolver,
                parentDocUri,
                mime,
                fileName
            ) ?: return Result.failure(Exception("Storage provider denied creating file in '${targetFolder.name}'."))

            contentResolver.openOutputStream(newDocUri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            } ?: return Result.failure(Exception("Failed to write content into '$fileName'."))

            val bytesCount = content.toByteArray(Charsets.UTF_8).size
            Result.success("Successfully created file '$fileName' inside folder '${targetFolder.name}' ($bytesCount bytes).")
        } catch (e: Exception) {
            Result.failure(Exception("Could not create file in folder: ${e.localizedMessage}"))
        }
    }

    fun moveFile(sourceFileNameOrUri: String, destinationFolderNameOrUri: String): Result<String> {
        val fileRes = getFileInfo(sourceFileNameOrUri)
        val sourceFile = fileRes.getOrElse { return Result.failure(it) }

        val destFolderRes = findFolder(destinationFolderNameOrUri)
        val destFolder = destFolderRes.getOrElse { return Result.failure(it) }

        val sourceUri = Uri.parse(sourceFile.uriString)
        val destUri = Uri.parse(destFolder.uriString)

        return try {
            // Attempt standard DocumentsContract.moveDocument (Android 24+)
            var moved = false
            try {
                // Find source parent document URI
                val authUris = preferences.getAuthorizedFolders()
                var sourceParentUri: Uri? = null
                for (u in authUris) {
                    val treeUri = Uri.parse(u)
                    val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
                    val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
                    sourceParentUri = rootDocUri
                    break
                }

                if (sourceParentUri != null) {
                    val newUri = DocumentsContract.moveDocument(contentResolver, sourceUri, sourceParentUri, destUri)
                    if (newUri != null) {
                        moved = true
                    }
                }
            } catch (_: Exception) {
                moved = false
            }

            // Fallback: Copy stream to new folder and delete old file
            if (!moved) {
                val mime = sourceFile.mimeType ?: resolveMimeType(sourceFile.name)
                val newDocUri = DocumentsContract.createDocument(
                    contentResolver,
                    destUri,
                    mime,
                    sourceFile.name
                ) ?: return Result.failure(Exception("Destination folder refused creating target document."))

                val input = contentResolver.openInputStream(sourceUri)
                    ?: return Result.failure(Exception("Could not read source file data."))
                val output = contentResolver.openOutputStream(newDocUri)
                    ?: return Result.failure(Exception("Could not write destination file data."))

                input.use { inStream ->
                    output.use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }

                // Delete source after copying
                DocumentsContract.deleteDocument(contentResolver, sourceUri)
            }

            Result.success("Successfully moved '${sourceFile.name}' into folder '${destFolder.name}'.")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to move file: ${e.localizedMessage}"))
        }
    }

    fun renameFile(fileNameOrUri: String, newName: String): Result<String> {
        val infoRes = getFileInfo(fileNameOrUri)
        val info = infoRes.getOrElse { return Result.failure(it) }

        return try {
            val uri = Uri.parse(info.uriString)
            val renamedUri = DocumentsContract.renameDocument(contentResolver, uri, newName)
            if (renamedUri != null) {
                Result.success("Successfully renamed '${info.name}' to '$newName'.")
            } else {
                Result.failure(Exception("Failed to rename file '${info.name}' to '$newName'."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Rename error: ${e.localizedMessage}"))
        }
    }

    fun deleteFile(fileNameOrUri: String, userConfirmed: Boolean): Result<String> {
        if (!userConfirmed) {
            return Result.failure(
                Exception("Destructive operation: File deletion requires explicit confirmation from the user. Please confirm by replying 'Confirm delete $fileNameOrUri'.")
            )
        }
        val infoRes = getFileInfo(fileNameOrUri)
        val info = infoRes.getOrElse { return Result.failure(it) }

        return try {
            val uri = Uri.parse(info.uriString)
            val deleted = DocumentsContract.deleteDocument(contentResolver, uri)
            if (deleted) {
                Result.success("File '${info.name}' was successfully deleted.")
            } else {
                Result.failure(Exception("Android storage system denied deletion of '${info.name}'."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Deletion error: ${e.localizedMessage}"))
        }
    }

    private fun resolveMimeType(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0) return "Unknown"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    }
}
