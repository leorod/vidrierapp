package com.vidrierapp.domain.storage

data class PhotoSlot(val path: String, val captureUri: String)

interface PhotoStorage {
    fun createPhoto(): PhotoSlot
    fun exists(path: String): Boolean
    fun delete(path: String)
}
