package bin.mg.main.utils.saf

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import bin.mg.main.model.FileItem

object SafHelper {

    fun listChildren(context: Context, treeUri: Uri): List<FileItem> {
        val items = mutableListOf<FileItem>()
        try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idCol)
                    val name = cursor.getString(nameCol) ?: continue
                    val mime = cursor.getString(mimeCol) ?: ""
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val modified = if (modifiedCol >= 0) cursor.getLong(modifiedCol) else 0L

                    val isDir = DocumentsContract.Document.MIME_TYPE_DIR == mime
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                    val childPath = childUri.toString()

                    val item = FileItem(name, childPath, size, isDir, modified)
                    items.add(item)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return items
    }

    fun readFileContent(context: Context, documentUri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(documentUri)?.use { stream ->
                stream.bufferedReader().readText()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun writeFileContent(context: Context, documentUri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(documentUri)?.use { stream ->
                stream.write(content.toByteArray())
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun findDocumentUri(context: Context, treeUri: Uri, path: String): Uri? {
        try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val segments = path.substringAfterLast("/", "").split("/").filter { it.isNotEmpty() }

            var currentDocId = docId
            for (segment in segments) {
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, currentDocId)
                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                )

                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)

                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idCol)
                        val name = cursor.getString(nameCol)
                        if (name == segment) {
                            currentDocId = id
                            return@use
                        }
                    }
                }
            }

            return DocumentsContract.buildDocumentUriUsingTree(treeUri, currentDocId)
        } catch (e: Exception) {
            return null
        }
    }
}
