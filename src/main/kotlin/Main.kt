import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadImage
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
            width = 800
            height = 600
        }
        program {
            val board = Board.from("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
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

            val tileSize = height / 9.0
            val edgeOffset = (height - tileSize * 8) / 2
            var mousePos: Vector2? = null
            var pieceMoveFromSqr: Int? = null

//            Helper functions
            fun isWithinBoard(vec: Vector2) = vec.x >= 0 && vec.x < tileSize * 8 && vec.y >= 0 && vec.y < tileSize * 8

            fun getFixedBoardVec(vec: Vector2) =
                Vector2(
                    vec.x - (width - (8 * tileSize) - edgeOffset),
                    vec.y - edgeOffset,
                )

            mouse.moved.listen { mousePos = it.position }
            mouse.buttonDown.listen { e ->
                val offsetPos = getFixedBoardVec(e.position)

                if (pieceMoveFromSqr == null && isWithinBoard(offsetPos)) {
                    pieceMoveFromSqr = (offsetPos / tileSize).toInt().let { it.y * 8 + it.x }
                }
            }
            mouse.buttonUp.listen { e ->
                val offsetPos = getFixedBoardVec(e.position)

                if (pieceMoveFromSqr != null && isWithinBoard(offsetPos)) {
                    val toSqr = (offsetPos / tileSize).toInt().let { it.y * 8 + it.x }

                    mvLoop@for (move in moveGen.moves) {
                        if (move.from != pieceMoveFromSqr || move.to != toSqr) continue

                        println(move)
                        board.makeMove(move)
                        moveStack.add(move)
                        moveGen.generateMoves()

                        break@mvLoop
                    }
                }

                pieceMoveFromSqr = null
            }

//            val bitboardTrackers =
//                listOf(
//                    { board.bitboards[Piece.PAWN]!! },
//                    { board.bitboards[Piece.KNIGHT]!! },
//                    { board.bitboards[Piece.BISHOP]!! },
//                    { board.bitboards[Piece.ROOK]!! },
//                    { board.bitboards[Piece.QUEEN]!! },
//                    { board.bitboards[Piece.KING]!! },
//                    { board.bitboards[Piece.WHITE]!! },
//                    { board.bitboards[Piece.BLACK]!! },
//                ).let {
//                    it.mapIndexed { i, bb ->
//                        bb to ColorHSLa(360.0 * i / it.size, 1.0, .5, .4).toRGBa()
//                    }
//                }

//            TODO: Add UI to show side-to-move, clocks, etc
            extend {
                drawer.clear(windowBG)

//                Loop through each board tile
                drawer.stroke = null
                for (x in 0..7) {
                    for (y in 0..7) {
                        val index = y * 8 + x
                        val piece = board.get(index)
                        val screenx = width - ((8 - x) * tileSize) - edgeOffset
                        val screeny = y * tileSize + edgeOffset

                        drawer.fill = if ((x + y) % 2 == 0) lightTile else darkTile
                        drawer.rectangle(screenx, screeny, tileSize)

// //                        Draw bit clr from bitboard
//                        if (options.displayBitboards) {
//                            val mask = 1UL shl index
//                            for ((bb, clr) in bitboardTrackers) {
//                                if (bb() and mask != 0UL) {
//                                    drawer.fill = clr
//                                    drawer.rectangle(screenx, screeny, tileSize)
//                                }
//                            }
//                        }

//                        Highlight move start/end tile
                        if (
                            pieceMoveFromSqr is Int &&
                            (pieceMoveFromSqr!! == index || moveGen.moves.any { it.from == pieceMoveFromSqr!! && it.to == index })
                        ) {
                            drawer.fill = tileHighlight
                            drawer.rectangle(screenx, screeny, tileSize)
                        }
                        if (piece == Piece.NONE) continue

//                        Draw piece
                        drawer.isolated {
                            if (pieceMoveFromSqr is Int && pieceMoveFromSqr == index) {
                                drawer.drawStyle.colorMatrix = translucencyMatrix
                            }

                            drawer.image(
                                pieceSpriteSheet,
                                pieceLoc[piece]!!,
                                Rectangle(screenx, screeny, tileSize),
                            )
                        }
                    }
                }

//                Draw moving piece separately
                if (pieceMoveFromSqr != null) {
                    val movedPiece = board.get(pieceMoveFromSqr!!)
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
