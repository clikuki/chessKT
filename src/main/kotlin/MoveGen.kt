fun lsb(bb: ULong) = bb.takeLowestOneBit().let { it to it.countTrailingZeroBits() }

private const val NOT_A_FILE = 0xfefefefefefefefeUL
private const val NOT_H_FILE = 0x7f7f7f7f7f7f7f7fUL

private fun southFill(a: ULong): ULong {
    var gen = a or (a shl 8)
    gen = gen or (gen shl 16)
    return gen or (gen shl 32)
}

private fun northFill(a: ULong): ULong {
    var gen = a or (a shr 8)
    gen = gen or (gen shr 16)
    return gen or (gen shr 32)
}

private fun eastFill(a: ULong): ULong {
    val pr0 = NOT_A_FILE
    val pr1 = pr0 and (pr0 shl 1)
    val pr2 = pr1 and (pr1 shl 2)
    var gen = a or (pr0 and (a shl 1))
    gen = gen or (pr1 and (gen shl 2))
    return gen or (pr2 and (gen shl 4))
}

private fun soEaFill(a: ULong): ULong {
    val pr0 = NOT_A_FILE
    val pr1 = pr0 and (pr0 shl 9)
    val pr2 = pr1 and (pr1 shl 18)

    var gen = a or (pr0 and (a shl 9))
    gen = gen or (pr1 and (gen shl 18))
    return gen or (pr2 and (gen shl 36))
}

private fun noEaFill(a: ULong): ULong {
    val pr0 = NOT_A_FILE
    val pr1 = pr0 and (pr0 shr 7)
    val pr2 = pr1 and (pr1 shr 14)
    var gen = a or (pr0 and (a shr 7))
    gen = gen or (pr1 and (gen shr 14))
    return gen or (pr2 and (gen shr 28))
}

private fun westFill(a: ULong): ULong {
    val pr0 = NOT_H_FILE
    val pr1 = pr0 and (pr0 shr 1)
    val pr2 = pr1 and (pr1 shr 2)
    var gen = a or (pr0 and (a shr 1))
    gen = gen or (pr1 and (gen shr 2))
    return gen or (pr2 and (gen shr 4))
}

private fun noWeFill(a: ULong): ULong {
    val pr0 = NOT_H_FILE
    val pr1 = pr0 and (pr0 shr 9)
    val pr2 = pr1 and (pr1 shr 18)
    var gen = a or (pr0 and (a shr 9))
    gen = gen or (pr1 and (gen shr 18))
    return gen or (pr2 and (gen shr 36))
}

private fun soWeFill(a: ULong): ULong {
    val pr0 = NOT_H_FILE
    val pr1 = pr0 and (pr0 shl 7)
    val pr2 = pr1 and (pr1 shl 14)
    var gen = a or (pr0 and (a shl 7))
    gen = gen or (pr1 and (gen shl 14))
    return gen or (pr2 and (gen shl 21))
}

private fun generateOrthogonalMoves(
    board: Board,
    list: MutableList<Move>,
) {
    var sliders = board.getQueenBB() or board.getRookBB()
    var (lsb1, from) = lsb(sliders)
    while (lsb1 != 0UL) {
        var rays = (northFill(lsb1) or southFill(lsb1) or westFill(lsb1) or eastFill(lsb1)) xor lsb1
        var (lsb2, to) = lsb(rays)
        while (rays != 0UL) {
            list.add(Move(from, to, type = 0))

            rays = rays xor lsb2
            with(lsb(rays)) {
                lsb2 = first
                to = second
            }
        }

        sliders = sliders xor lsb1
        with(lsb(sliders)) {
            lsb1 = first
            from = second
        }
    }
}

private fun generateDiagonalMoves(
    board: Board,
    list: MutableList<Move>,
) {
    var sliders = board.getQueenBB() or board.getBishopBB()
    var (lsb1, from) = lsb(sliders)
    while (lsb1 != 0UL) {
        var rays = (noWeFill(lsb1) or noEaFill(lsb1) or soWeFill(lsb1) or soEaFill(lsb1)) xor lsb1
        var (lsb2, to) = lsb(rays)
        while (rays != 0UL) {
            list.add(Move(from, to, type = 0))

            rays = rays xor lsb2
            with(lsb(rays)) {
                lsb2 = first
                to = second
            }
        }

        sliders = sliders xor lsb1
        with(lsb(sliders)) {
            lsb1 = first
            from = second
        }
    }
}

object MoveGen {
    fun pseudoLegal(board: Board): List<Move> {
        val moves = mutableListOf<Move>()
        generateOrthogonalMoves(board, moves)
        generateDiagonalMoves(board, moves)
        return moves
    }
}

private fun main() {
//    TODO: Perft tests
}
