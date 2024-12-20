import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadImage
import org.openrndr.extra.color.presets.LIME_GREEN
import org.openrndr.math.Matrix55
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.time.measureTime

val lightTile = ColorRGBa.fromHex("#f2e1c3")
val darkTile = ColorRGBa.fromHex("#c3a082")
val tileHighlight = ColorRGBa.fromHex("#08ff006c")
val checkHighlight = ColorRGBa.fromHex("#ff2c2c80")
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
//            val board = Board.from("8/PPPPPPPP/8/8/8/8/R7/K6k w - - 0 1")
            val moveStack = ArrayDeque<Move>()
            val moveGen = MoveGen(board)
            moveGen.generateMoves()
            val engine = Engine(board, moveGen)

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
            val promoOptions = listOf(Piece.QUEEN, Piece.ROOK, Piece.BISHOP, Piece.KNIGHT)
            val promoOptionsX = width - 50 - tileSize * 9

            var mousePos: Vector2? = null
            var pieceMoveFromSqr: Int? = null
            var promoteToPc: Byte = Piece.QUEEN

//            Helper functions
            fun isWithinBoard(vec: Vector2) = vec.x >= 0 && vec.x < tileSize * 8 && vec.y >= 0 && vec.y < tileSize * 8

            fun getFixedBoardVec(vec: Vector2) =
                Vector2(
                    vec.x - (width - (8 * tileSize) - edgeOffset),
                    vec.y - edgeOffset,
                )

            fun correctMove(mv: Move, toSqr: Int): Boolean {
                if(mv.from != pieceMoveFromSqr) return false
                if(mv.to != toSqr) return false

                if(mv.type and 0b1000.b == 0.b) return true
                return mv.type and 0b0011 == 0b0011.b and when(promoteToPc) {
                    Piece.KNIGHT -> Move.N_PROMO_CAPTURE
                    Piece.BISHOP -> Move.B_PROMO_CAPTURE
                    Piece.ROOK -> Move.R_PROMO_CAPTURE
                    Piece.QUEEN -> Move.Q_PROMO_CAPTURE
                    else -> throw Exception("Invalid variable: promoteToPc")
                }
            }

            mouse.moved.listen { mousePos = it.position }
            mouse.buttonDown.listen { e ->
                val offsetPos = getFixedBoardVec(e.position)

                if (pieceMoveFromSqr == null && isWithinBoard(offsetPos)) {
                    pieceMoveFromSqr = (offsetPos / tileSize).toInt().let { it.y * 8 + it.x }
                }
            }
            mouse.buttonUp.listen { e ->
                val offsetPos = getFixedBoardVec(e.position)

//                Piece drop-off
                if (pieceMoveFromSqr != null && isWithinBoard(offsetPos)) {
                    val toSqr = (offsetPos / tileSize).toInt().let { it.y * 8 + it.x }

                    mvLoop@for (move in moveGen.moves) {
                        if (!correctMove(move, toSqr)) continue
//                        println(move)

//                        Player
                        board.makeMove(move)
                        moveStack.add(move)

//                        Engine
                        measureTime {
                            engine.search(4).let {
                                if (it != Move.NULLMOVE) {
                                    board.makeMove(it)
                                    moveStack.add(it)
                                }
                            }
                        }.let {
                            println("Evaluation: ${engine.evalTime }")
                            println("Overall: $it")
                        }

                        moveGen.generateMoves()

                        break@mvLoop
                    }
                }

//                Pawn promotion options
                if(
                    e.position.x >= promoOptionsX &&
                    e.position.x <= promoOptionsX + tileSize &&
                    e.position.y >= edgeOffset &&
                    e.position.y <= promoOptions.size * tileSize + edgeOffset
                ) {
                    for((i, pc) in promoOptions.withIndex()) {
                        val bottomEdge = tileSize * (i+1) + edgeOffset
                        if(e.position.y <= bottomEdge) {
                            promoteToPc = pc
                            break
                        }
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

//                Pawn promotion selection
                promoOptions.forEachIndexed { i, pc ->
                    if(promoteToPc == pc) {
                        drawer.fill = ColorRGBa.LIME_GREEN
                        drawer.stroke = ColorRGBa.BLACK
                        drawer.strokeWeight = 2.0
                    } else {
                        drawer.fill = ColorRGBa.fromHex("0x00000022")
                        drawer.stroke = null
                        drawer.strokeWeight = 0.0
                    }

                    val screenY = tileSize * i + edgeOffset
                    if(promoteToPc == pc) drawer.rectangle(promoOptionsX, screenY, tileSize)
                    drawer.image(
                        pieceSpriteSheet,
                        pieceLoc[pc or Piece.WHITE]!!,
                        Rectangle(promoOptionsX, screenY, tileSize),
                    )
                    if(promoteToPc != pc) drawer.rectangle(promoOptionsX, screenY, tileSize)
                }
                drawer.fill = null
                drawer.stroke = ColorRGBa.BLACK
                drawer.strokeWeight = 2.0
                drawer.rectangle(promoOptionsX, edgeOffset, tileSize, promoOptions.size * tileSize)

//                Loop through each board tile
                drawer.stroke = null
                for (x in 0..7) {
                    for (y in 0..7) {
                        val index = y * 8 + x
                        val piece = board.get(index)
                        val screenX = width - ((8 - x) * tileSize) - edgeOffset
                        val screenY = y * tileSize + edgeOffset

                        drawer.fill = if ((x + y) % 2 == 0) lightTile else darkTile
                        drawer.rectangle(screenX, screenY, tileSize)

// //                        Draw bit clr from bitboard
//                        if (options.displayBitboards) {
//                            val mask = 1UL shl index
//                            for ((bb, clr) in bitboardTrackers) {
//                                if (bb() and mask != 0UL) {
//                                    drawer.fill = clr
//                                    drawer.rectangle(screenX, screenY, tileSize)
//                                }
//                            }
//                        }

//                        Highlight checked kings
                        if (moveGen.data.inCheck && piece == board.side or Piece.KING) {
                            drawer.fill = checkHighlight
                            drawer.rectangle(screenX, screenY, tileSize)
                        }

//                        Highlight move start/end tile
                        if (
                            pieceMoveFromSqr is Int &&
                            (pieceMoveFromSqr!! == index || moveGen.moves.any { it.from == pieceMoveFromSqr!! && it.to == index })
                        ) {
                            drawer.fill = tileHighlight
                            drawer.rectangle(screenX, screenY, tileSize)
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
                                Rectangle(screenX, screenY, tileSize),
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
