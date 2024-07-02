import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor

data class Move(
    val from: Int,
    val to: Int,
    val isEnpassant: Boolean,
    val kingSideCastling: Boolean,
    val queenSideCastling: Boolean,
)

//    TODO: Add Castling flags
//    TODO: Add enpassant target sqr
class Board {
    val grid = ByteArray(64) { Piece.NONE }
    var side: Byte = Piece.WHITE
    var enpassantTarget: Int? = null

    val bitboards =
        mutableMapOf(
            Piece.PAWN to 0L,
            Piece.BISHOP to 0L,
            Piece.KNIGHT to 0L,
            Piece.ROOK to 0L,
            Piece.QUEEN to 0L,
            Piece.KING to 0L,
            Piece.WHITE to 0L,
            Piece.BLACK to 0L,
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
        val mask = 1L shl i
        if (occupancyBB and mask != 0L) {
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
//        Update grid
        val fullpiece = grid[move.from]
        val fullCaptured = grid[move.to]
        grid[move.from] = Piece.NONE
        grid[move.to] = fullpiece

//        Update bitboards
        val piece = fullpiece and Piece.TYPE
        val clr = fullpiece and Piece.COLOR
        val fromMask = (1L shl move.from)
        val toMask = (1L shl move.to)
        bitboards[piece] = bitboards[piece]!! or toMask xor fromMask
        bitboards[clr] = bitboards[clr]!! or toMask xor fromMask

        if (move.isEnpassant) {
//            TODO: Support en passant
        } else if (move.kingSideCastling || move.queenSideCastling) {
//            TODO: Support castling
        } else if (fullCaptured != Piece.NONE) {
            val capturedPiece = fullCaptured and Piece.TYPE
            val capturedClr = fullCaptured and Piece.COLOR
            bitboards[capturedPiece] = bitboards[piece]!! xor toMask
            bitboards[capturedClr] = bitboards[capturedClr]!! xor toMask
        }
    }

    fun unmakeMove() {
//        TODO: Implement unmake move
    }

    companion object {
        fun from(fen: String): Board {
            val fenParts = fen.split(' ')
            val grid = Board()

            var x = 0
            var y = 7
            for (char in fenParts[0]) {
                if (char == '/') {
                    if (--y < 0) break
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
                    grid.set(x++, y, color or type)
                }
            }
            return grid
        }
    }
}
