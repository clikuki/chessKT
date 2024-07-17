import kotlin.experimental.and

object Piece {
    const val NONE: Byte = 0b00_000
    const val PAWN: Byte = 0b00_001
    const val BISHOP: Byte = 0b00_010
    const val KNIGHT: Byte = 0b00_011
    const val ROOK: Byte = 0b00_100
    const val QUEEN: Byte = 0b00_101
    const val KING: Byte = 0b00_110
    const val WHITE: Byte = 0b01_000
    const val BLACK: Byte = 0b10_000

    const val COLOR: Byte = 0b11_000
    const val TYPE: Byte = 0b00_111

    fun split(pc: Byte) = (pc and COLOR) to (pc and TYPE)

    fun invClr(clr: Byte) = if (clr == WHITE) BLACK else WHITE
}
