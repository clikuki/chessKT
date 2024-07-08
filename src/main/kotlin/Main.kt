import org.openrndr.application
import org.openrndr.color.ColorHSLa
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadImage
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.ActionParameter
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.math.Matrix55
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
//            val board = Board.from("RNBQKBNR/PPPPPPPP/8/8/8/8/pppppppp/rnbqkbnr w KQkq - 0 1")
            val board = Board.from("8/3pP3/8/8/3Qq3/8/3Pp3/8 w KQkq - 0 1")
//            val board = Board.from("8/8/8/8/3Qq3/8/8/8 w KQkq - 0 1")
            val moveStack = ArrayDeque<Move>()
            var validMoves = MoveGen.pseudoLegal(board)

            val gui = GUI()
            val settings =
                object {
                    @BooleanParameter("Display Bitboards")
                    var displayBitboards = true

                    @Suppress("unused")
                    @ActionParameter("Undo Move")
                    fun undoMove() {
                        if (moveStack.isEmpty()) return
                        board.unmakeMove(moveStack.removeLast())
                    }
                }
            gui.add(settings, "Settings")

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
            val lightTile = ColorRGBa.fromHex("#f2e1c3")
            val darkTile = ColorRGBa.fromHex("#c3a082")
            val tileHighlight = ColorRGBa.fromHex("#08ff006c")
            val windowBG = ColorRGBa.fromHex("#3a3a3a")
            val translucencyMatrix =
                Matrix55(
                    1.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    1.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    1.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.5,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    1.0,
                )

            var mousePos: Vector2? = null
            var pcMoveIndex: Int? = null

//            Helper functions
            fun isWithinBoard(vec: Vector2) =
                vec.x >= boardOffset &&
                    vec.x < boardOffset + tileSize * 8 &&
                    vec.y >= boardOffset &&
                    vec.y < boardOffset + tileSize * 8

            mouse.moved.listen { mousePos = it.position }
            mouse.buttonDown.listen { e ->
                if (pcMoveIndex == null && isWithinBoard(e.position)) {
                    pcMoveIndex = ((e.position - boardOffset) / tileSize).toInt().let { it.y * 8 + it.x }
                }
            }
            mouse.buttonUp.listen { e ->
                if (pcMoveIndex != null && isWithinBoard(e.position)) {
                    val toIndex = ((e.position - boardOffset) / tileSize).toInt().let { it.y * 8 + it.x }
                    if (pcMoveIndex!! != toIndex) {
                        mvLoop@for (move in validMoves) {
//                            TODO: Update for special move types, ie. castling
                            if (move.from != pcMoveIndex!! || move.to != toIndex) continue
                            board.makeMove(move)
                            moveStack.add(move)
                            validMoves = MoveGen.pseudoLegal(board)
                            break@mvLoop
                        }
                    }
                }

                pcMoveIndex = null
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
                            val mask = 1UL shl index
                            for ((bb, clr) in bitboardTrackers) {
                                if (bb() and mask != 0UL) {
                                    drawer.fill = clr
                                    drawer.rectangle(x * tileSize + boardOffset, y * tileSize + boardOffset, tileSize)
                                }
                            }
                        }

//                        Highlight move start/end tile
                        if (
                            pcMoveIndex is Int &&
                            (pcMoveIndex!! == index || validMoves.any { it.from == pcMoveIndex!! && it.to == index })
                        ) {
                            drawer.fill = tileHighlight
                            drawer.rectangle(x * tileSize + boardOffset, y * tileSize + boardOffset, tileSize)
                        }
                        if (piece == Piece.NONE) continue

                        drawer.isolated {
                            if (pcMoveIndex is Int && pcMoveIndex == index) {
                                drawer.drawStyle.colorMatrix = translucencyMatrix
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
                }

//                Draw moving piece separately
                if (pcMoveIndex != null) {
                    val movedPiece = board.get(pcMoveIndex!!)
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
