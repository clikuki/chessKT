import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.extra.noise.fastFloor
import org.openrndr.shape.Rectangle

fun main() =
    application {
        configure {
            title = "ChessRNDR"
            width = 1024
            height = 576
        }
        program {
            val pieceSpriteSheet = loadImage("data/images/1280px-Chess_Pieces.png")
            val spriteSize = pieceSpriteSheet.width / 6.0
            val pieceLoc =
                buildList {
                    for (x in 0..5) {
                        for (y in 0..1) {
                            add(Rectangle(x * spriteSize, y * spriteSize, spriteSize, spriteSize))
                        }
                    }
                }

            extend {
                drawer.clear(ColorRGBa.GRAY)

                val pieceSpriteIndex = seconds.fastFloor() % pieceLoc.size
                drawer.image(pieceSpriteSheet, pieceLoc[pieceSpriteIndex], Rectangle(0.0, 0.0, spriteSize))
            }
        }
    }
