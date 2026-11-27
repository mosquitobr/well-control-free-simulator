package com.mosquitobr.controlepoco.platform

import java.io.File
import java.nio.charset.Charset
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Writes local persistence files atomically via a temporary sibling file
 * followed by atomic move, preventing file truncation on abrupt shutdown.
 */
object AtomicFileWriter {
    fun writeTextAtomically(target: File, content: String, charset: Charset = Charsets.UTF_8) {
        val parent = target.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        val tmp = File(parent ?: File("."), "${target.name}.tmp")
        tmp.writeText(content, charset)
        try {
            Files.move(
                tmp.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (ex: Exception) {
            tmp.delete()
            throw ex
        }
    }
}
