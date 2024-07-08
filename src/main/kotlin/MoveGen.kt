fun lsb(bb: ULong) = bb.takeLowestOneBit().let { it to it.countTrailingZeroBits() }

private fun generateOrthogonalMoves(
    board: Board,
    list: MutableList<Move>,
) {
    var sliders = board.getQueenBB() or board.getRookBB()
    val enemy = board.getOpponentBB()
    val friendly = board.getColorBB()
    var (lsb1, from) = lsb(sliders)
    while (lsb1 != 0UL) {
        val north = Occl.nort(lsb1, (friendly or Shift.nort(enemy)).inv())
        val south = Occl.sout(lsb1, (friendly or Shift.sout(enemy)).inv())
        val west = Occl.west(lsb1, (friendly or Shift.west(enemy)).inv())
        val east = Occl.east(lsb1, (friendly or Shift.east(enemy)).inv())
        var rays = (north or south or west or east) xor lsb1

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
    val enemy = board.getOpponentBB()
    val friendly = board.getColorBB()
    var (lsb1, from) = lsb(sliders)
    while (lsb1 != 0UL) {
        val noWe = Occl.noWe(lsb1, (friendly or Shift.noWe(enemy)).inv())
        val noEa = Occl.noEa(lsb1, (friendly or Shift.noEa(enemy)).inv())
        val soWe = Occl.soWe(lsb1, (friendly or Shift.soWe(enemy)).inv())
        val soEa = Occl.soEa(lsb1, (friendly or Shift.soEa(enemy)).inv())
        var rays = (noWe or noEa or soWe or soEa) xor lsb1
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
