package com.neldasi.dafscanner.extras

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

suspend fun saveImageToInternal(context: Context, uri: Uri, fullCode: String): String? = withContext(Dispatchers.IO) {
    return@withContext try {
        val file = File(context.filesDir, "img_$fullCode.jpg")
        // We overwrite the existing file if it exists
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun deleteImageForCode(context: Context, fullCode: String): Unit = withContext(Dispatchers.IO) {
    File(context.filesDir, "img_$fullCode.jpg").delete()
}
