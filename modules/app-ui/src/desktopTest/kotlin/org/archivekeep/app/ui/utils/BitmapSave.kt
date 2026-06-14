package org.archivekeep.app.ui.utils

import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.outputStream

fun BufferedImage.save(file: Path) {
    file.outputStream().use { out ->
        ImageIO.write(this, "PNG", out)
    }
}
