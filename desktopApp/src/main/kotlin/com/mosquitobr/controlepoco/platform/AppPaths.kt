package com.mosquitobr.controlepoco.platform

import java.io.File
import java.util.Locale

enum class DesktopOs {
    WINDOWS,
    LINUX,
    MACOS,
    OTHER
}

/**
 * Resolve diretórios padrão de configuração, dados e exportação para
 * Windows 11 (%APPDATA%) e Linux/WSL ($XDG_CONFIG_HOME / $XDG_DATA_HOME).
 */
object AppPaths {
    private const val WIN_DIR_NAME = "ControleDePoco2"
    private const val UNIX_DIR_NAME = "controle-de-poco-2"

    /** Permite sobrescrever o diretório raiz em testes automatizados. */
    @Volatile
    var overrideRootDir: File? = null

    val currentOs: DesktopOs by lazy {
        val name = System.getProperty("os.name", "").lowercase(Locale.ROOT)
        when {
            name.contains("win") -> DesktopOs.WINDOWS
            name.contains("linux") -> DesktopOs.LINUX
            name.contains("mac") || name.contains("darwin") -> DesktopOs.MACOS
            else -> DesktopOs.OTHER
        }
    }

    val isWsl: Boolean by lazy {
        if (currentOs != DesktopOs.LINUX) return@lazy false
        if (!System.getenv("WSL_DISTRO_NAME").isNullOrBlank()) return@lazy true
        if (!System.getenv("WSL_INTEROP").isNullOrBlank()) return@lazy true
        runCatching {
            val version = File("/proc/version")
            version.exists() && version.readText().lowercase(Locale.ROOT).contains("microsoft")
        }.getOrDefault(false)
    }

    val platformLabel: String by lazy {
        val arch = System.getProperty("os.arch", "x64")
        val javaVer = System.getProperty("java.version", "21")
        when (currentOs) {
            DesktopOs.WINDOWS -> "Windows Desktop ($arch · JVM $javaVer)"
            DesktopOs.LINUX -> {
                val distro = System.getenv("WSL_DISTRO_NAME")
                if (isWsl) {
                    val suffix = if (!distro.isNullOrBlank()) " · $distro" else ""
                    "Linux via WSL2/WSLg ($arch$suffix · JVM $javaVer)"
                } else {
                    "Linux Desktop ($arch · JVM $javaVer)"
                }
            }
            DesktopOs.MACOS -> "macOS Desktop ($arch · JVM $javaVer)"
            DesktopOs.OTHER -> "JVM Desktop ($arch · JVM $javaVer)"
        }
    }

    fun configDir(): File {
        overrideRootDir?.let { return File(it, "config").apply { mkdirs() } }
        val base = when (currentOs) {
            DesktopOs.WINDOWS -> {
                val appData = System.getenv("APPDATA")
                if (!appData.isNullOrBlank()) File(appData, WIN_DIR_NAME)
                else File(System.getProperty("user.home"), "AppData/Roaming/$WIN_DIR_NAME")
            }
            DesktopOs.LINUX -> {
                val xdg = System.getenv("XDG_CONFIG_HOME")
                if (!xdg.isNullOrBlank()) File(xdg, UNIX_DIR_NAME)
                else File(System.getProperty("user.home"), ".config/$UNIX_DIR_NAME")
            }
            DesktopOs.MACOS -> {
                File(System.getProperty("user.home"), "Library/Application Support/$WIN_DIR_NAME")
            }
            DesktopOs.OTHER -> {
                File(System.getProperty("user.home"), ".$UNIX_DIR_NAME")
            }
        }
        return base.apply { mkdirs() }
    }

    fun dataDir(): File {
        overrideRootDir?.let { return File(it, "data").apply { mkdirs() } }
        val base = when (currentOs) {
            DesktopOs.WINDOWS -> {
                val localAppData = System.getenv("LOCALAPPDATA") ?: System.getenv("APPDATA")
                if (!localAppData.isNullOrBlank()) File(localAppData, WIN_DIR_NAME)
                else File(System.getProperty("user.home"), "AppData/Local/$WIN_DIR_NAME")
            }
            DesktopOs.LINUX -> {
                val xdg = System.getenv("XDG_DATA_HOME")
                if (!xdg.isNullOrBlank()) File(xdg, UNIX_DIR_NAME)
                else File(System.getProperty("user.home"), ".local/share/$UNIX_DIR_NAME")
            }
            DesktopOs.MACOS -> {
                File(System.getProperty("user.home"), "Library/Application Support/$WIN_DIR_NAME")
            }
            DesktopOs.OTHER -> {
                File(System.getProperty("user.home"), ".$UNIX_DIR_NAME")
            }
        }
        return base.apply { mkdirs() }
    }

    fun defaultExportDir(): File {
        val home = File(System.getProperty("user.home"))
        val downloads = File(home, "Downloads")
        if (downloads.exists() && downloads.isDirectory) return downloads
        val documents = File(home, "Documents")
        if (documents.exists() && documents.isDirectory) return documents
        return dataDir()
    }
}
