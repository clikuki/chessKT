import java.util.Stack
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

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
    private val grid = ByteArray(64) { Piece.NONE }
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

    val occupancyBB get() = bitboards[Piece.WHITE]!! or bitboards[Piece.BLACK]!!

    fun getColorBB(color: Byte = side) = bitboards[color]!!

    fun getOpponentBB(color: Byte = side) = bitboards[Piece.invClr(color)]!!

    fun getPawnBB(color: Byte = side) = bitboards[color]!! and bitboards[Piece.PAWN]!!

    fun getBishopBB(color: Byte = side) = bitboards[color]!! and bitboards[Piece.BISHOP]!!

    fun getKnightBB(color: Byte = side) = bitboards[color]!! and bitboards[Piece.KNIGHT]!!

    fun getRookBB(color: Byte = side) = bitboards[color]!! and bitboards[Piece.ROOK]!!

    fun getQueenBB(color: Byte = side) = bitboards[color]!! and bitboards[Piece.QUEEN]!!

    fun getKingBB(color: Byte = side) = bitboards[color]!! and bitboards[Piece.KING]!!

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
        bitboards[clr] = bitboards[clr]!! or mask
        bitboards[piece and Piece.TYPE] = bitboards[piece and Piece.TYPE]!! or mask
    }

    fun makeMove(move: Move) {
        val isPromo = move.type and 8 == 8.b
        val isCapture = move.type and 4 == 4.b
        val isCastling = move.type and 14 == 2.b
        val isEnpassant = move.type == Move.EP_CAPTURE

//        1. Store unmove data
        unmoveStack.push(
            Unmove(
                captured = grid[if (isEnpassant) enpassantTarget else move.to],
                prevHalfMoves = halfMoveClock,
                prevEnpassant = enpassantTarget,
                prevCastling = castlingRights,
            ),
        )

        val movedPiece = grid[move.from]
        val pieceType = movedPiece and Piece.TYPE
        val promoPiece =
            when (move.type and 0b0011) {
                0.b -> Piece.KNIGHT
                1.b -> Piece.BISHOP
                2.b -> Piece.ROOK
                3.b -> Piece.QUEEN
                else -> Piece.NONE
            }

        val fromMask = (1UL shl move.from)
        val toMask = (1UL shl move.to)

//        2. Remove captured pc from its bitboard
        if (isCapture) {
            val (capClr, capType) = Piece.split(grid[if (isEnpassant) enpassantTarget else move.to])
            val mask = if (isEnpassant) (1UL shl enpassantTarget) else toMask
            bitboards[capType] = bitboards[capType]!! xor mask
            bitboards[capClr] = bitboards[capClr]!! xor mask

//            Remove castling right for captured rooks
            if (castlingRights != 0.b && capType == Piece.ROOK) {
                val x = move.to % 8
                val y = move.to / 8
//                Only rooks on the corners
                if ((x == 0 || x == 7) && ((capClr == Piece.BLACK && y == 0) || (capClr == Piece.WHITE && y == 7))) {
//                    CASTLING = COLOR + SIDE
                    var shiftBy = if (capClr == Piece.WHITE) 2 else 0
                    if (x == 0) shiftBy++
                    castlingRights = castlingRights and (1 shl shiftBy).toByte().inv()
                }
            }
        }

//        3. If castling, move rook in its bitboard and grid
        if (isCastling) {
//            GRID
            val isKingside = move.type and 1 == 0.b
            val rookFromIndex = move.to + (if (isKingside) 1 else -2)
            val rookToIndex = move.from + (move.to - move.from) / 2
            grid[rookToIndex] = grid[rookFromIndex]
            grid[rookFromIndex] = Piece.NONE

//            BB
            val rookFromMask = (1UL shl rookFromIndex)
            val rookToMask = (1UL shl rookToIndex)
            bitboards[Piece.ROOK] = bitboards[Piece.ROOK]!! xor rookFromMask or rookToMask
            bitboards[side] = bitboards[side]!! xor rookFromMask or rookToMask
        }

//        4. If promoting, add respective piece to its bitboard
//        5. Update moved piece in its bitboard
        bitboards[side] = bitboards[side]!! xor fromMask or toMask
        bitboards[pieceType] = bitboards[pieceType]!! xor fromMask
        if (isPromo) {
            bitboards[promoPiece] = bitboards[promoPiece]!! or toMask
        } else {
            bitboards[pieceType] = bitboards[pieceType]!! or toMask
        }

//        6. Update grid representation
        grid[move.from] = Piece.NONE
        grid[move.to] = if (isPromo) promoPiece or side else movedPiece
        if (move.type == Move.EP_CAPTURE) {
            grid[enpassantTarget] = Piece.NONE
        }

//        6. Update remaining board states

//        Update castling rights for king/rook moves
        if (castlingRights != 0.b) {
            if (pieceType == Piece.KING) {
                castlingRights = castlingRights and if (side == Piece.WHITE) 0b0011 else 0b1100
            } else if (pieceType == Piece.ROOK) {
                val x = move.from % 8
                val y = move.from / 8
                if ((x == 0 || x == 7) && ((side == Piece.BLACK && y == 0) || (side == Piece.WHITE && y == 7))) {
                    var shiftBy = if (side == Piece.WHITE) 2 else 0
                    if (x == 0) shiftBy += 1
                    castlingRights = castlingRights and (1 shl shiftBy).toByte().inv()
                }
            }
        }

        if (isCapture || pieceType == Piece.PAWN) {
            halfMoveClock = 0
        } else {
            ++halfMoveClock
        }

        if (side == Piece.WHITE) {
            side = Piece.BLACK
        } else {
            side = Piece.WHITE
            ++fullMoveCounter
        }

        enpassantTarget = if (move.type == Move.DBL_PUSH) move.to else -1
    }

    fun unmakeMove(move: Move) {
        val unmove = unmoveStack.pop() ?: return

//        1. Reset board states
        enpassantTarget = unmove.prevEnpassant
        castlingRights = unmove.prevCastling
        halfMoveClock = unmove.prevHalfMoves
        if (side == Piece.WHITE) {
            side = Piece.BLACK
        } else {
            side = Piece.WHITE
            fullMoveCounter--
        }

        val isPromo = move.type and 8 == 8.b
        val isCapture = move.type and 4 == 4.b
        val isCastling = move.type and 14 == 2.b
        val isEnpassant = move.type == Move.EP_CAPTURE

        val promoPiece = grid[move.to] and Piece.TYPE
        val movedPiece = if (isPromo) side or Piece.PAWN else grid[move.to]
        val pieceType = movedPiece and Piece.TYPE

        val fromMask = (1UL shl move.from)
        val toMask = (1UL shl move.to)

//        2. Return moved piece to original bitboard index
        bitboards[side] = bitboards[side]!! or fromMask xor toMask
        bitboards[pieceType] = bitboards[pieceType]!! or fromMask
        if (isPromo) {
            bitboards[promoPiece] = bitboards[promoPiece]!! xor toMask
        } else {
            bitboards[pieceType] = bitboards[pieceType]!! xor toMask
        }

//        3. If capturing, return captured piece
        if (isCapture) {
            val (capClr, capType) = Piece.split(unmove.captured)
            val mask = if (isEnpassant) (1UL shl enpassantTarget) else toMask
            bitboards[capType] = bitboards[capType]!! or mask
            bitboards[capClr] = bitboards[capClr]!! or mask
        }

//        4. If castling, reset rooks
        if (isCastling) {
//            GRID
            val isKingside = move.type and 1 == 0.b
            val rookFromIndex = move.to + (if (isKingside) 1 else -2)
            val rookToIndex = move.from + (move.to - move.from) / 2
            grid[rookFromIndex] = grid[rookToIndex]
            grid[rookToIndex] = Piece.NONE

//            BB
            val rookFromMask = (1UL shl rookFromIndex)
            val rookToMask = (1UL shl rookToIndex)
            bitboards[Piece.ROOK] = bitboards[Piece.ROOK]!! or rookFromMask xor rookToMask
            bitboards[side] = bitboards[side]!! or rookFromMask xor rookToMask
        }

//       5. Update grid representation
        grid[move.from] = movedPiece
        if (isEnpassant) {
            grid[enpassantTarget] = unmove.captured
            grid[move.to] = Piece.NONE
        } else {
            grid[move.to] = unmove.captured
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
                        'c' -> 2
                        'd' -> 3
                        'e' -> 4
                        'f' -> 5
                        'g' -> 6
                        'h' -> 7
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

        private val fileChars =
            mapOf(
                0 to 'a',
                1 to 'b',
                2 to 'c',
                3 to 'd',
                4 to 'e',
                5 to 'f',
                6 to 'g',
                7 to 'h',
            )

        fun indexToSqr(i: Int) = fileChars[i % 8]!! + (8 - (i / 8)).toString()
    }
}
