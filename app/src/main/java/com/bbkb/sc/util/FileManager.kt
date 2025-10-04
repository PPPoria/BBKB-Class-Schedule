package com.bbkb.sc.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.bbkb.sc.SCApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object FileManager {

    suspend fun saveInnerImageToGallery(
        absolutePath: String
    ): Uri? {
        return withContext(Dispatchers.IO) {
            val srcFile = File(absolutePath)
            val resolver = SCApp.app.contentResolver
            val album = Environment.DIRECTORY_PICTURES
            val relativePath = "$album/BBKB"          // 可自己改名
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, srcFile.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            }
            val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val uri = resolver.insert(collection, values) ?: return@withContext null
            resolver.openOutputStream(uri).use { out ->
                srcFile.inputStream().copyTo(out!!)
            }
            return@withContext uri
        }
    }

    suspend fun deleteInnerImageFromGallery(
        absolutePath: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val file = File(absolutePath)
            return@withContext file.delete()
        }
    }
}