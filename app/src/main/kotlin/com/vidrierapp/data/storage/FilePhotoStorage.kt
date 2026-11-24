package com.vidrierapp.data.storage

import android.content.Context
import androidx.core.content.FileProvider
import com.vidrierapp.domain.storage.PhotoSlot
import com.vidrierapp.domain.storage.PhotoStorage
import java.io.File
import java.util.UUID

class FilePhotoStorage(private val context: Context) : PhotoStorage {

    private val directory: File
        get() = File(context.filesDir, PHOTOS_DIRECTORY).also { it.mkdirs() }

    override fun createPhoto(): PhotoSlot {
        val file = File(directory, "${UUID.randomUUID()}.jpg")
        file.createNewFile()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file)
        return PhotoSlot(path = file.absolutePath, captureUri = uri.toString())
    }

    override fun exists(path: String): Boolean {
        val file = File(path)
        return file.isInside(directory) && file.length() > 0
    }

    override fun delete(path: String) {
        val file = File(path)
        if (file.isInside(directory)) file.delete()
    }

    private fun File.isInside(parent: File): Boolean =
        canonicalFile.parentFile == parent.canonicalFile

    companion object {
        const val PHOTOS_DIRECTORY = "visit_photos"
        const val AUTHORITY_SUFFIX = ".fileprovider"
    }
}
