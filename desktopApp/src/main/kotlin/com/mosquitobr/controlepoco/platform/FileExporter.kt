package com.mosquitobr.controlepoco.platform

import java.awt.FileDialog
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Exportação de arquivos JSON via diálogo nativo do sistema operacional
 * (Windows 11 e Linux/WSLg), com fallback seguro para ambientes headless.
 */
object FileExporter {

    fun saveJson(suggestedName: String, content: String): File? {
        val target = chooseSaveFile(suggestedName) ?: return null
        val finalFile = if (target.name.lowercase().endsWith(".json")) {
            target
        } else {
            File(target.parentFile, "${target.name}.json")
        }
        finalFile.parentFile?.mkdirs()
        finalFile.writeText(content, Charsets.UTF_8)
        return finalFile
    }

    private fun chooseSaveFile(suggestedName: String): File? {
        if (GraphicsEnvironment.isHeadless()) {
            return File(AppPaths.defaultExportDir(), suggestedName)
        }
        var chosen: File? = null
        val runnable = Runnable {
            chosen = when (AppPaths.currentOs) {
                DesktopOs.WINDOWS, DesktopOs.MACOS -> showNativeAwtDialog(suggestedName)
                DesktopOs.LINUX, DesktopOs.OTHER -> showSwingDialog(suggestedName)
            }
        }
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run()
        } else {
            SwingUtilities.invokeAndWait(runnable)
        }
        return chosen
    }

    private fun showNativeAwtDialog(suggestedName: String): File? {
        val dialog = FileDialog(null as Frame?, "Exportar JSON", FileDialog.SAVE).apply {
            directory = AppPaths.defaultExportDir().absolutePath
            file = suggestedName
            setFilenameFilter { _, name -> name.lowercase().endsWith(".json") }
            isVisible = true
        }
        val dir = dialog.directory
        val file = dialog.file
        dialog.dispose()
        return if (!dir.isNullOrBlank() && !file.isNullOrBlank()) File(dir, file) else null
    }

    private fun showSwingDialog(suggestedName: String): File? {
        val chooser = JFileChooser(AppPaths.defaultExportDir()).apply {
            dialogTitle = "Exportar JSON"
            selectedFile = File(AppPaths.defaultExportDir(), suggestedName)
            fileFilter = FileNameExtensionFilter("JSON (*.json)", "json")
        }
        val result = chooser.showSaveDialog(null)
        return if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    }
}
