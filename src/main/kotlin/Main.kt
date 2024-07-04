import org.openrndr.application
import org.openrndr.color.ColorHSLa
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
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
            val gui = GUI()
            val settings =
                object {
                    @BooleanParameter("Display Bitboards")
                    var displayBitboards = true
                }
            gui.add(settings, "Settings")

            val board = Board.from("RNBQKBNR/PPPPPPPP/8/8/8/8/pppppppp/rnbqkbnr w KQkq - 0 1")
            val pieceSpriteSheet = loadImage("data/images/1280px-Chess_Pieces.png")
            val font = loadFont("data/fonts/default.otf", 32.0)
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
            val lightTile = ColorRGBa.fromHex("#f2e1c3")
            val darkTile = ColorRGBa.fromHex("#c3a082")
            val tileHighlight = ColorRGBa.fromHex("#08ff006c")
            val windowBG = ColorRGBa.fromHex("#3a3a3a")

            var mousePos: Vector2? = null
            var pcMovePos: IntVector2? = null

//            Helper functions
            fun isWithinBoard(vec: Vector2) =
                vec.x >= boardOffset &&
                    vec.x < boardOffset + tileSize * 8 &&
                    vec.y >= boardOffset &&
                    vec.y < boardOffset + tileSize * 8

            mouse.moved.listen { mousePos = it.position }
            mouse.buttonDown.listen { e ->
                if (pcMovePos == null && isWithinBoard(e.position)) {
                    val piecePos = ((e.position - boardOffset) / tileSize).toInt()
                    pcMovePos = piecePos
                }
            }
            mouse.buttonUp.listen { e ->
                if (pcMovePos != null && isWithinBoard(e.position)) {
                    val fromIndex = pcMovePos!!.let { it.y * 8 + it.x }
                    val toIndex = ((e.position - boardOffset) / tileSize).toInt().let { it.y * 8 + it.x }
                    if (fromIndex != toIndex) {
                        board.makeMove(
                            Move(
                                from = fromIndex,
                                to = toIndex,
                                isEnpassant = false,
                                kingSideCastling = false,
                                queenSideCastling = false,
                            ),
                        )
                    }
                }

                pcMovePos = null
            }

            val bitboardTrackers =
                listOf(
                    board::pawnBB,
                    board::bishopBB,
                    board::knightBB,
                    board::rookBB,
                    board::queenBB,
                    board::kingBB,
                ).let {
                    it.mapIndexed { i, bb ->
                        bb to ColorHSLa(360.0 * i / it.size, 1.0, .5, .4).toRGBa()
                    }
                }

            extend(gui)
            extend {
                drawer.clear(windowBG)

//                Loop through each board tile
                drawer.stroke = null
                for (x in 0..7) {
                    for (y in 0..7) {
                        drawer.fill = if ((x + y) % 2 == 0) lightTile else darkTile
                        drawer.rectangle(x * tileSize + boardOffset, y * tileSize + boardOffset, tileSize)

                        val index = y * 8 + x
                        val piece = board.get(index)

//                        Draw bit clr from bitboard
                        if (settings.displayBitboards) {
                            val mask = 1L shl index
                            for ((bb, clr) in bitboardTrackers) {
                                if (bb() and mask != 0L) {
                                    drawer.fill = clr
                                    drawer.rectangle(x * tileSize + boardOffset, y * tileSize + boardOffset, tileSize)
                                }
                            }
                        }

                        if (piece == Piece.NONE) continue
//                        Highlight move start tile
                        if (x == pcMovePos?.x && y == pcMovePos?.y) {
                            drawer.fill = tileHighlight
                            drawer.rectangle(x * tileSize + boardOffset, y * tileSize + boardOffset, tileSize)
                            continue
                        }

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

//                Draw moving piece separately
                if (pcMovePos != null) {
                    val movedPiece = board.get(pcMovePos!!.y * 8 + pcMovePos!!.x)
                    if (movedPiece != Piece.NONE) {
                        drawer.image(
                            pieceSpriteSheet,
                            pieceLoc[movedPiece]!!,
                            Rectangle(mousePos!! - tileSize / 2, tileSize),
                        )
                    }
                }
            }
        }
    }
