fun lsb(bb: ULong) = bb.takeLowestOneBit().let { it to it.countTrailingZeroBits() }

private fun generateOrthogonalMoves(
    board: Board,
    moves: MutableList<Move>,
) {
    var sliders = board.getQueenBB() or board.getRookBB()
    val enemy = board.getOpponentBB()
    val friendly = board.getColorBB()
    var (lsb1, from) = lsb(sliders)
    while (lsb1 != 0UL) {
        val north = Occl.nort(lsb1, (friendly or Shift.nort(enemy, 1)).inv())
        val south = Occl.sout(lsb1, (friendly or Shift.sout(enemy, 1)).inv())
        val west = Occl.west(lsb1, (friendly or Shift.west(enemy)).inv())
        val east = Occl.east(lsb1, (friendly or Shift.east(enemy)).inv())
        var rays = (north or south or west or east) xor lsb1

        var (lsb2, to) = lsb(rays)
        while (rays != 0UL) {
            moves.add(Move(from, to, type = 0))

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
    moves: MutableList<Move>,
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
            moves.add(Move(from, to, type = 0))

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

private fun generatePawnMoves(
    board: Board,
    moves: MutableList<Move>,
) {
    val isWhite = board.side == Piece.WHITE
    val forwardOffset = if (isWhite) -8 else 8
    val shifter = if (isWhite) Shift::nort else Shift::sout
    val promotionRank = if (isWhite) RANK_8 else RANK_1
    val dblPushRank = if (isWhite) RANK_2 else RANK_7

    val pawns = board.getPawnBB()
    val emptySqrs = board.occupancyBB.inv()
    var normalPawns = shifter(pawns and promotionRank.inv(), 1) and emptySqrs
    var promoPawns = shifter(pawns and promotionRank, 1) and emptySqrs
    var dblPushPawns = shifter(pawns and dblPushRank, 2) and (emptySqrs and shifter(emptySqrs, 1))

//    Normal push
    var (normLsb, normIndex) = lsb(normalPawns)
    while (normLsb != 0UL) {
        moves.add(Move(from = normIndex - forwardOffset, to = normIndex, type = 0))

        normalPawns = normalPawns xor normLsb
        with(lsb(normalPawns)) {
            normLsb = first
            normIndex = second
        }
    }

//    Double push
    var (dblLsb, dblIndex) = lsb(dblPushPawns)
    while (dblLsb != 0UL) {
        moves.add(
            Move(
                from = dblIndex - forwardOffset - forwardOffset,
                to = dblIndex,
                type = Move.types["dbl p push"]!!,
            ),
        )

        dblPushPawns = dblPushPawns xor dblLsb
        with(lsb(dblPushPawns)) {
            dblLsb = first
            dblIndex = second
        }
    }

//    Promo push
    var (promoLsb, promoIndex) = lsb(promoPawns)
    while (promoLsb != 0UL) {
        moves.add(
            Move(
                from = promoIndex - forwardOffset,
                to = promoIndex,
                type = Move.types["n promote"]!!,
            ),
        )
        moves.add(
            Move(
                from = promoIndex - forwardOffset,
                to = promoIndex,
                type = Move.types["b promote"]!!,
            ),
        )
        moves.add(
            Move(
                from = promoIndex - forwardOffset,
                to = promoIndex,
                type = Move.types["r promote"]!!,
            ),
        )
        moves.add(
            Move(
                from = promoIndex - forwardOffset,
                to = promoIndex,
                type = Move.types["q promote"]!!,
            ),
        )

        promoPawns = promoPawns xor promoLsb
        with(lsb(promoPawns)) {
            promoLsb = first
            promoIndex = second
        }
    }
}

object MoveGen {
    fun pseudoLegal(board: Board): List<Move> {
        val moves = mutableListOf<Move>()
        generateOrthogonalMoves(board, moves)
        generateDiagonalMoves(board, moves)
        generatePawnMoves(board, moves)
        return moves
    }
}

private fun main() {
//    TODO: Perft tests
}
