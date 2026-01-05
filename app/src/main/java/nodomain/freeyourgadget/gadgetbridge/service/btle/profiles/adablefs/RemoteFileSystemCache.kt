package nodomain.freeyourgadget.gadgetbridge.util

import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.adablefs.AdaFsEntry

/**
 * Type-safe cache for remote filesystem.
 * Directories are maps, files are AdaFsEntry objects.
 */
class RemoteFileSystemCache {

    private val root = mutableMapOf<String, Any>()

    fun updateEntry(path: String, entry: AdaFsEntry) {
        val parts = path.trim('/').split("/")
        var current: MutableMap<String, Any> = root
        for ((index, part) in parts.withIndex()) {
            if (index == parts.lastIndex) {
                current[part] = entry
            } else {
                current = (current[part] as? MutableMap<String, Any>) ?: mutableMapOf<String, Any>().also {
                    current[part] = it
                }
            }
        }
    }

    fun directoryExists(path: String): Boolean {
        val parts = path.trim('/').split("/")
        var current: Map<String, Any> = root
        for (part in parts) {
            val next = current[part] ?: return false
            current = next as? Map<String, Any> ?: return false
        }
        return true
    }

    fun getEntry(path: String): AdaFsEntry? {
        val parts = path.trim('/').split("/")
        var current: Any = root
        for (part in parts) {
            current = (current as? Map<*, *>)?.get(part) ?: return null
        }
        return current as? AdaFsEntry
    }

    fun removeEntry(path: String) {
        val parts = path.trim('/').split("/")
        var current: MutableMap<String, Any> = root
        for ((index, part) in parts.withIndex()) {
            if (index == parts.lastIndex) {
                current.remove(part)
            } else {
                current = (current[part] as? MutableMap<String, Any>) ?: return
            }
        }
    }

    fun listDirectory(path: String): List<AdaFsEntry> {
        val parts = path.trim('/').split("/")
        var current: Any = root
        for (part in parts) {
            current = (current as? Map<String, Any>)?.get(part) ?: return emptyList()
        }
        val dir = current as? Map<String, Any> ?: return emptyList()
        return dir.values.mapNotNull { it as? AdaFsEntry }
    }
}
