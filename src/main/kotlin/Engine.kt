class Engine(
    private val board: Board,
    private val moveGen: MoveGen,
) {
    private fun attacked(side: Byte): ULong {
        val orthoSliders = (board.getQueenBB(side) or board.getRookBB(side))
        val diagonalSliders = (board.getQueenBB(side) or board.getBishopBB(side))
        val empty = board.occupancyBB.inv()

        var attackedSqrs = Rays.nort(orthoSliders, empty)
        attackedSqrs = attackedSqrs or Rays.sout(orthoSliders, empty)
        attackedSqrs = attackedSqrs or Rays.west(orthoSliders, empty)
        attackedSqrs = attackedSqrs or Rays.east(orthoSliders, empty)
        attackedSqrs = attackedSqrs or Rays.noWe(diagonalSliders, empty)
        attackedSqrs = attackedSqrs or Rays.noEa(diagonalSliders, empty)
        attackedSqrs = attackedSqrs or Rays.soWe(diagonalSliders, empty)
        attackedSqrs = attackedSqrs or Rays.soEa(diagonalSliders, empty)

        attackedSqrs = attackedSqrs or knightFill(board.getKnightBB(side))
        attackedSqrs = attackedSqrs or kingAttacks[board.getKingBB(side).countTrailingZeroBits()]!!

        val pawns = board.getPawnBB(side)
        attackedSqrs = attackedSqrs or (if (side == Piece.WHITE) Shift::soWe else Shift::noWe)(pawns)
        return attackedSqrs or (if (side == Piece.WHITE) Shift::soEa else Shift::noEa)(pawns)
    }

    private fun evaluate(): Int {
//        TODO: Write a proper evaluation function
        val oppClr = if (board.side == Piece.WHITE) Piece.BLACK else Piece.WHITE
        val ownSqrs = attacked(board.side)
        val oppSqrs = attacked(oppClr)
//        Maybe score capture moves differently to silent moves?
        val mobility = (ownSqrs and oppSqrs.inv()).countOneBits() - (oppSqrs and ownSqrs.inv()).countOneBits()

//        println("Mobility: $mobility")
//        println("Pawns: ${board.getPawnBB().countOneBits() - board.getPawnBB(oppClr).countOneBits()}")
//        println("Knights: ${(board.getKnightBB().countOneBits() - board.getKnightBB(oppClr).countOneBits()) * 3}")
//        println("Bishops: ${(board.getBishopBB().countOneBits() - board.getBishopBB(oppClr).countOneBits()) * 3}")
//        println("Rooks: ${(board.getRookBB().countOneBits() - board.getRookBB(oppClr).countOneBits()) * 5}")
//        println("Queens: ${(board.getQueenBB().countOneBits() - board.getQueenBB(oppClr).countOneBits()) * 9}")
//        println("Kings: ${(board.getKingBB().countOneBits() - board.getKingBB(oppClr).countOneBits()) * 200}")
//        println("============\n")

        return (board.getPawnBB().countOneBits() - board.getPawnBB(oppClr).countOneBits()) +
            (board.getKnightBB().countOneBits() - board.getKnightBB(oppClr).countOneBits()) * 3 +
            (board.getBishopBB().countOneBits() - board.getBishopBB(oppClr).countOneBits()) * 3 +
            (board.getRookBB().countOneBits() - board.getRookBB(oppClr).countOneBits()) * 5 +
            (board.getQueenBB().countOneBits() - board.getQueenBB(oppClr).countOneBits()) * 9 +
            (board.getKingBB().countOneBits() - board.getKingBB(oppClr).countOneBits()) * 200 +
            mobility
    }

    private fun negaMax(depth: Int): Int {
        if (depth <= 0) return evaluate()

        var max = Int.MIN_VALUE
        val mvs = moveGen.generateMoves(mutableListOf())

        for (mv in mvs) {
            board.makeMove(mv)
            val score = -negaMax(depth - 1)
            if (score > max) max = score
            board.unmakeMove(mv)
        }

        return max
    }

    fun search(depth: Int): Move {
        var max = Int.MIN_VALUE
        var bestmv = Move.NULLMOVE
        val mvs = moveGen.generateMoves(mutableListOf())

        for (mv in mvs) {
            board.makeMove(mv)
            val score = -negaMax(depth - 1)
            if (score > max) {
                max = score
                bestmv = mv
            }
            board.unmakeMove(mv)

//            val promoTo =
//                when (mv.type and 0b1011) {
//                    0b1000.b -> "n"
//                    0b1001.b -> "b"
//                    0b1010.b -> "r"
//                    0b1011.b -> "q"
//                    else -> ""
//                }
//            println("${Board.indexToSqr(mv.from)}${Board.indexToSqr(mv.to)}$promoTo: $score")
        }

        return bestmv
    }
}
