import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.shape.Rectangle
import kotlin.experimental.or

fun main() =
    application {
        configure {
            title = "ChessRNDR"
            width = 1088
            height = 612
        }
        program {
            val pieceSpriteSheet = loadImage("data/images/1280px-Chess_Pieces.png")
            val spriteSize = pieceSpriteSheet.width / 6.0
            val pieceLoc =
                buildMap {
                    val pieceTypes = listOf(Piece.KING, Piece.QUEEN, Piece.BISHOP, Piece.KNIGHT, Piece.ROOK, Piece.PAWN)
                    for (x in 0..5) {
                        for (y in 0..1) {
                            val rect = Rectangle(x * spriteSize, y * spriteSize, spriteSize)
                            val piece = pieceTypes[x] or (if (y == 0) Piece.WHITE else Piece.BLACK)
                            set(piece, rect)
                        }
                    }
                }

            val tileSize = height / 10.0
            val boardOffset = (height - tileSize * 8) / 2
            val boardLight = ColorRGBa.fromHex(0xf2e1c3)
            val boardDark = ColorRGBa.fromHex(0xc3a082)
            val windowBG = ColorRGBa.fromHex(0x3a3a3a)

            val board = Board.from("RNBQKBNR/PPPPPPPP/8/8/8/8/pppppppp/rnbqkbnr w KQkq - 0 1")

            extend {
                drawer.clear(windowBG)

                drawer.stroke = null
                for (x in 0..7) {
                    for (y in 0..7) {
                        drawer.fill = if ((x + y) % 2 == 0) boardLight else boardDark
                        drawer.rectangle(x * tileSize + boardOffset, y * tileSize + boardOffset, tileSize)

                        val piece = board.get(y * 8 + x)
                        if (piece != Piece.NONE) {
                            drawer.image(
                                pieceSpriteSheet,
                                pieceLoc[piece]!!,
                                Rectangle(
                                    x * tileSize + boardOffset,
                                    y * tileSize + boardOffset,
                                    tileSize,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
