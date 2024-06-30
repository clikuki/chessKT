import kotlin.experimental.or

class Board {
    val grid = ByteArray(64) { Piece.NONE }

    val pawnBB = 0L
    val bishopBB = 0L
    val knightBB = 0L
    val rookBB = 0L
    val queenBB = 0L
    val kingBB = 0L
    val whiteBB = 0L
    val blackBB = 0L

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

    fun getPawnBB(color: Byte) = if (color == Piece.WHITE) whitePawnBB else blackPawnBB

    fun getBishopBB(color: Byte) = if (color == Piece.WHITE) whiteBishopBB else blackBishopBB

    fun getKnightBB(color: Byte) = if (color == Piece.WHITE) whiteKnightBB else blackKnightBB

    fun getRookBB(color: Byte) = if (color == Piece.WHITE) whiteRookBB else blackRookBB

    fun getQueenBB(color: Byte) = if (color == Piece.WHITE) whiteQueenBB else blackQueenBB

    fun getKingBB(color: Byte) = if (color == Piece.WHITE) whiteKingBB else blackKingBB

    fun get(pos: Int) = grid[pos]

    fun set(
        x: Int,
        y: Int,
        piece: Byte,
    ) {
        grid[y * 8 + x] = piece
    }

    companion object {
        fun from(fen: String): Board {
            val grid = Board()
            var x = 0
            var y = 7
            for (char in fen) {
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
