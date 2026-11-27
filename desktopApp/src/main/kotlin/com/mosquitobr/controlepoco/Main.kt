package com.mosquitobr.controlepoco

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.mosquitobr.controlepoco.ui.AppRoot
import com.mosquitobr.controlepoco.ui.ControlePocoTheme
import java.awt.Dimension
import javax.imageio.ImageIO

fun main() = application {
    val windowState = rememberWindowState(
        placement = WindowPlacement.Floating,
        position = WindowPosition.Aligned(androidx.compose.ui.Alignment.Center),
        size = DpSize(1360.dp, 860.dp)
    )

    val appIcon = runCatching {
        object {}.javaClass.getResourceAsStream("/icons/app_icon.png")?.use { stream ->
            BitmapPainter(ImageIO.read(stream).toComposeImageBitmap())
        }
    }.getOrNull()

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Controle de Poço 2 — v${BuildConfig.VERSION_NAME}",
        icon = appIcon
    ) {
        window.minimumSize = Dimension(960, 640)
        ControlePocoTheme {
            AppRoot()
        }
    }
}
