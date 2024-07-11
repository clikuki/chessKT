import java.util.Stack
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor

inline val Int.b get() = toByte()

data class Move(
    val from: Int,
    val to: Int,
    val type: Byte,
) {
    companion object {
        const val QUIET: Byte = 0b0000
        const val DBL_PUSH: Byte = 0b0001
        const val K_CASTLE: Byte = 0b0010
        const val Q_CASTLE: Byte = 0b0011
        const val CAPTURE: Byte = 0b0100
        const val EP_CAPTURE: Byte = 0b0101
        const val N_PROMOTE: Byte = 0b1000
        const val B_PROMOTE: Byte = 0b1001
        const val R_PROMOTE: Byte = 0b1010
        const val Q_PROMOTE: Byte = 0b1011
        const val N_PROMO_CAPTURE: Byte = 0b1100
        const val B_PROMO_CAPTURE: Byte = 0b1101
        const val R_PROMO_CAPTURE: Byte = 0b1110
        const val Q_PROMO_CAPTURE: Byte = 0b1111
    }
}

data class Unmove(
    val captured: Byte,
    val prevEnpassant: Int,
    val prevCastling: Byte,
    val prevHalfMoves: Int,
)

class Board {
    val grid = ByteArray(64) { Piece.NONE }
    var side: Byte = Piece.WHITE
    var enpassantTarget = -1
    var halfMoveClock = 0
    var fullMoveCounter = 1
    var castlingRights: Byte = 0
    private val unmoveStack = Stack<Unmove>()

    val bitboards =
        mutableMapOf(
            Piece.PAWN to 0UL,
            Piece.BISHOP to 0UL,
            Piece.KNIGHT to 0UL,
            Piece.ROOK to 0UL,
            Piece.QUEEN to 0UL,
            Piece.KING to 0UL,
            Piece.WHITE to 0UL,
            Piece.BLACK to 0UL,
        )
    var pawnBB
        get() = bitboards[Piece.PAWN]!!
        set(v) {
            bitboards[Piece.PAWN] = v
        }
    var bishopBB
        get() = bitboards[Piece.BISHOP]!!
        set(v) {
            bitboards[Piece.BISHOP] = v
        }
    var knightBB
        get() = bitboards[Piece.KNIGHT]!!
        set(v) {
            bitboards[Piece.KNIGHT] = v
        }
    var rookBB
        get() = bitboards[Piece.ROOK]!!
        set(v) {
            bitboards[Piece.ROOK] = v
        }
    var queenBB
        get() = bitboards[Piece.QUEEN]!!
        set(v) {
            bitboards[Piece.QUEEN] = v
        }
    var kingBB
        get() = bitboards[Piece.KING]!!
        set(v) {
            bitboards[Piece.KING] = v
        }
    var whiteBB
        get() = bitboards[Piece.WHITE]!!
        set(v) {
            bitboards[Piece.WHITE] = v
        }
    var blackBB
        get() = bitboards[Piece.BLACK]!!
        set(v) {
            bitboards[Piece.BLACK] = v
        }

    val occupancyBB get() = whiteBB or blackBB
    val whitePawnBB get() = whiteBB and pawnBB
    val whiteBishopBB get() = whiteBB and bishopBB
    val whiteKnightBB get() = whiteBB and knightBB
    val whiteRookBB get() = whiteBB and rookBB
    val whiteQueenBB get() = whiteBB and queenBB
    val whiteKingBB get() = whiteBB and kingBB
    val blackPawnBB get() = blackBB and pawnBB
    val blackBishopBB get() = blackBB and bishopBB
    val blackKnightBB get() = blackBB and knightBB
    val blackRookBB get() = blackBB and rookBB
    val blackQueenBB get() = blackBB and queenBB
    val blackKingBB get() = blackBB and kingBB

    fun getColorBB(color: Byte = side) = if (color == Piece.WHITE) whiteBB else blackBB

    fun getOpponentBB(color: Byte = side) = if (color == Piece.WHITE) blackBB else whiteBB

    fun getPawnBB(color: Byte = side) = if (color == Piece.WHITE) whitePawnBB else blackPawnBB

    fun getBishopBB(color: Byte = side) = if (color == Piece.WHITE) whiteBishopBB else blackBishopBB

    fun getKnightBB(color: Byte = side) = if (color == Piece.WHITE) whiteKnightBB else blackKnightBB

    fun getRookBB(color: Byte = side) = if (color == Piece.WHITE) whiteRookBB else blackRookBB

    fun getQueenBB(color: Byte = side) = if (color == Piece.WHITE) whiteQueenBB else blackQueenBB

    fun getKingBB(color: Byte = side) = if (color == Piece.WHITE) whiteKingBB else blackKingBB

    fun get(pos: Int) = grid[pos]

    fun set(
        x: Int,
        y: Int,
        piece: Byte,
    ) {
//        Update grid
        val i = y * 8 + x
        grid[i] = piece

//        Fix possible bitboard colliding
        val clr = piece and Piece.COLOR
        val mask = 1UL shl i
        if (occupancyBB and mask != 0UL) {
            for ((type, bb) in bitboards) {
                bitboards[type] = (bb xor mask) and bb
            }
        }

//        Set piece bitboards
        if (clr == Piece.WHITE) {
            whiteBB = whiteBB or mask
        } else {
            blackBB = blackBB or mask
        }
        when (piece xor clr) {
            Piece.PAWN -> pawnBB = pawnBB or mask
            Piece.BISHOP -> bishopBB = bishopBB or mask
            Piece.KNIGHT -> knightBB = knightBB or mask
            Piece.ROOK -> rookBB = rookBB or mask
            Piece.QUEEN -> queenBB = queenBB or mask
            Piece.KING -> kingBB = kingBB or mask
        }
    }

    fun makeMove(move: Move) {
        val fullpiece = grid[move.from]
        val fullCaptured = if (move.type == Move.EP_CAPTURE) grid[enpassantTarget] else grid[move.to]
        unmoveStack.push(
            Unmove(
                captured = fullCaptured,
                prevHalfMoves = halfMoveClock,
                prevEnpassant = enpassantTarget,
                prevCastling = castlingRights,
            ),
        )

//        Update grid
        grid[move.from] = Piece.NONE
        grid[move.to] = fullpiece
        if (move.type == Move.EP_CAPTURE) {
            grid[enpassantTarget] = Piece.NONE
        }

//        Remove captured from BB
        val ownPiece = fullpiece and Piece.TYPE
        val ownClr = fullpiece and Piece.COLOR
        val oppPiece = fullCaptured and Piece.TYPE
        val oppClr = fullCaptured and Piece.COLOR
        val fromMask = (1UL shl move.from)
        val toMask = (1UL shl move.to)
        if (move.type xor 2 < 2) {
//            Update rooks grid and bitboards
            val rookFromIndex = move.to + (if (move.type xor 2 == (0).b) 1 else -2)
            val rookToIndex = move.from + (move.to - move.from) / 2
            grid[rookToIndex] = grid[rookFromIndex]
            grid[rookFromIndex] = Piece.NONE

            val rookFromMask = (1UL shl rookFromIndex)
            val rookToMask = (1UL shl rookToIndex)
            rookBB = rookBB xor rookFromMask or rookToMask
            bitboards[ownClr] = bitboards[ownClr]!! xor rookFromMask or rookToMask

//            Update castling rights
            var shiftBy = if (ownClr == Piece.WHITE) 2 else 0
            if (move.type and (1).b != (0).b) shiftBy += 1
            castlingRights = castlingRights xor (1 shl shiftBy).toByte()
        } else if (move.type == Move.EP_CAPTURE) {
//            Remove ep piece
            val epMask = 1UL shl enpassantTarget
            bitboards[oppPiece] = bitboards[oppPiece]!! xor epMask
            bitboards[oppClr] = bitboards[oppClr]!! xor epMask
        } else if (fullCaptured != Piece.NONE) {
//            Remove captured piece
            bitboards[oppPiece] = bitboards[oppPiece]!! xor toMask
            bitboards[oppClr] = bitboards[oppClr]!! xor toMask
        }

//        Update castling rights for king/rook moves
        if (ownPiece == Piece.KING) {
            castlingRights = castlingRights and if (ownClr == Piece.WHITE) 0b0011 else 0b1100
        } else if (ownPiece == Piece.ROOK) {
            val xFrom = move.from % 8
            if (xFrom !in 1..6) {
                var shiftBy = if (ownClr == Piece.BLACK) 0 else 2
                if (xFrom == 0) shiftBy += 1
                castlingRights = castlingRights xor (1 shl shiftBy).toByte()
            }
        }

//        Update castling rights for captured rooks
        if (fullCaptured and Piece.ROOK != (0).b) {
            val xTo = move.to % 8
            val yTo = move.to / 8
            if (xTo !in 1..6 && yTo !in 1..6) {
                var shiftBy = if (yTo == 0) 0 else 2
                if (xTo == 0) shiftBy += 1
                castlingRights = castlingRights xor (1 shl shiftBy).toByte()
            }
        }

//        Update pc position in BB
        bitboards[ownPiece] = bitboards[ownPiece]!! or toMask xor fromMask
        bitboards[ownClr] = bitboards[ownClr]!! or toMask xor fromMask

        if (ownPiece == Piece.PAWN || fullCaptured != Piece.NONE) {
            halfMoveClock = 0
        } else {
            halfMoveClock++
        }

        if (side == Piece.WHITE) {
            side = Piece.BLACK
        } else {
            side = Piece.WHITE
            fullMoveCounter++
        }

        enpassantTarget =
            if (move.type == Move.DBL_PUSH) {
                move.to
            } else {
                -1
            }
    }

    fun unmakeMove(move: Move) {
        val unmove = unmoveStack.pop() ?: return
        val fullpiece = grid[move.to]
        val fullCaptured = unmove.captured

//        Reset irreversible board states
        enpassantTarget = unmove.prevEnpassant
        castlingRights = unmove.prevCastling
        halfMoveClock = unmove.prevHalfMoves

//        Update grid
        grid[move.from] = fullpiece
        if (move.type == Move.EP_CAPTURE) {
            grid[unmove.prevEnpassant] = fullCaptured
            grid[move.to] = Piece.NONE
        } else {
            grid[move.to] = fullCaptured
        }

//        Update pc position in BB
        val ownPiece = fullpiece and Piece.TYPE
        val ownClr = fullpiece and Piece.COLOR
        val oppPiece = fullCaptured and Piece.TYPE
        val oppClr = fullCaptured and Piece.COLOR
        val fromMask = (1UL shl move.from)
        val toMask = (1UL shl move.to)
        bitboards[ownPiece] = bitboards[ownPiece]!! or fromMask xor toMask
        bitboards[ownClr] = bitboards[ownClr]!! or fromMask xor toMask

//        Re-add captured from BB
        if (move.type xor 2 < 2) {
//            Update rooks for castling
            val rookFromIndex = move.to + (if (move.type xor 2 == (0).b) 1 else -2)
            val rookToIndex = move.from + (move.to - move.from) / 2
            grid[rookFromIndex] = grid[rookToIndex]
            grid[rookToIndex] = Piece.NONE

            val rookFromMask = (1UL shl rookFromIndex)
            val rookToMask = (1UL shl rookToIndex)
            rookBB = rookBB xor rookToMask or rookFromMask
            bitboards[ownClr] = bitboards[ownClr]!! xor rookToMask or rookFromMask
        } else if (move.type == Move.EP_CAPTURE) {
//            Return ep piece
            val epMask = 1UL shl enpassantTarget
            bitboards[oppPiece] = bitboards[oppPiece]!! or epMask
            bitboards[oppClr] = bitboards[oppClr]!! or epMask
        } else if (fullCaptured != Piece.NONE) {
            bitboards[oppPiece] = bitboards[oppPiece]!! or toMask
            bitboards[oppClr] = bitboards[oppClr]!! or toMask
        }

        if (side == Piece.WHITE) {
            side = Piece.BLACK
        } else {
            side = Piece.WHITE
            fullMoveCounter++
        }
    }

    companion object {
        fun from(fen: String): Board {
            val fenParts = fen.split(' ')
            val board = Board()

//            Piece placement
            var x = 0
            var y = 0
            for (char in fenParts[0]) {
                if (char == '/') {
                    if (++y > 7) break
                    x = 0
                } else if (char.isDigit()) {
                    x += char.digitToInt()
                } else {
                    val color = if (char.isUpperCase()) Piece.WHITE else Piece.BLACK
                    val type =
                        when (char.lowercaseChar()) {
                            'p' -> Piece.PAWN
                            'b' -> Piece.BISHOP
                            'n' -> Piece.KNIGHT
                            'r' -> Piece.ROOK
                            'q' -> Piece.QUEEN
                            'k' -> Piece.KING
                            else -> throw Exception("Invalid FEN piece character : $char")
                        }
                    board.set(x++, y, color or type)
                }
            }

//            Side to move
            when (fenParts[1]) {
                "w" -> board.side = Piece.WHITE
                "b" -> board.side = Piece.BLACK
                else -> throw Error("INVALID FEN - SIDE")
            }

//            Castling
            board.castlingRights = 0
            if (fenParts[2] != "-") {
                for (char in fenParts[2]) {
                    board.castlingRights = board.castlingRights or
                        when (char) {
                            'k' -> 1
                            'q' -> 2
                            'K' -> 4
                            'Q' -> 8
                            else -> throw Error("INVALID FEN - CASTLING")
                        }
                }
            }

//            EP target sqr
            fenParts[3].let {
                if (it == "-") {
                    board.enpassantTarget = -1
                    return@let
                }

                val rank = it[1].digitToInt().let { r -> if (r == 3) 4 else 3 }
                val file =
                    when (it[0]) {
                        'a' -> 0
                        'b' -> 1
                        'c' -> 3
                        'd' -> 4
                        'e' -> 5
                        'f' -> 6
                        'g' -> 7
                        'h' -> 8
                        else -> throw Error("INVALID FEN - EP SQR")
                    }

                board.enpassantTarget = rank * 8 + file
            }

//            Halfmove clock
            board.halfMoveClock = fenParts[4].toInt()

//            Fullmove clock
            board.fullMoveCounter = fenParts[5].toInt()

            return board
        }
    }
}
