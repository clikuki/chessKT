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

fun main() =
    application {
        configure {
            title = "ChessRNDR"
            width = 1088
            height = 612
        }
        program {
//            val board = Board.from("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
//            val board = Board.from("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/P2P2PP/r2Q1R1K w kq - 0 2")
            val board = Board.from("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1")
            val moveStack = ArrayDeque<Move>()
            val moveGen = MoveGen(board)

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

            val gui = GUI()
            val options =
                object {
                    @BooleanParameter("Display Bitboards")
                    var displayBitboards = true

                    @Suppress("unused")
                    @ActionParameter("Undo Move")
                    fun undoMove() {
                        if (moveStack.isEmpty()) return
                        board.unmakeMove(moveStack.removeLast())
                        moveGen.generateMoves()
                    }
                }
            gui.add(options, "Options")

            val tileSize = height / 10.0
            val boardOffset = (height - tileSize * 8) / 2
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
                    mvLoop@for (move in moveGen.moves) {
                        if (move.from != pcMoveIndex!! || move.to != toIndex) continue
                        println(move)
                        board.makeMove(move)
                        moveStack.add(move)
                        moveGen.generateMoves()
                        break@mvLoop
                    }
                }

                pcMoveIndex = null
            }

            val bitboardTrackers =
                listOf(
//                    { board.bitboards[Piece.PAWN]!! },
//                    { board.bitboards[Piece.KNIGHT]!! },
//                    { board.bitboards[Piece.BISHOP]!! },
//                    { board.bitboards[Piece.ROOK]!! },
//                    { board.bitboards[Piece.QUEEN]!! },
//                    { board.bitboards[Piece.KING]!! },
//                    { board.bitboards[Piece.WHITE]!! },
//                    { board.bitboards[Piece.BLACK]!! },
//                    moveGen.data::attackedSqrs,
//                    moveGen.data::checkers,
                    moveGen.data::orthoPins,
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
                        if (options.displayBitboards) {
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
                            (pcMoveIndex!! == index || moveGen.moves.any { it.from == pcMoveIndex!! && it.to == index })
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
