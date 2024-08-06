import kotlin.time.Duration
import kotlin.time.measureTimedValue

inline val ULong.cnt get() = countOneBits()

class Engine(
    private val board: Board,
    private val moveGen: MoveGen,
) {
    private fun getAttackedSqrs(side: Byte): ULong {
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

//    1/(1+10**(-pawnAdvantage/4))
//    Maybe score capture moves differently to silent moves?
    private fun evaluate(): Int {
        val wAtks = getAttackedSqrs(Piece.WHITE)
        val bAtks = getAttackedSqrs(Piece.BLACK)
        val mobility = (wAtks and bAtks.inv()).cnt - (bAtks and wAtks.inv()).cnt

        val material =
            (board.wPawnBB.countOneBits() - board.bPawnBB.countOneBits()) * 100 +
                (board.wBishopBB.countOneBits() - board.bBishopBB.countOneBits()) * 350 +
                (board.wKnightBB.countOneBits() - board.bKnightBB.countOneBits()) * 350 +
                (board.wRookBB.countOneBits() - board.bRookBB.countOneBits()) * 525 +
                (board.wQueenBB.countOneBits() - board.bQueenBB.countOneBits()) * 1000 +
                (board.bKingBB.countOneBits() - board.bKingBB.countOneBits()) * 10000

        val toMove = if (board.side == Piece.WHITE) 1 else -1
        return (material + mobility) * toMove
    }

    var evalTime = Duration.ZERO

    private fun negaMax(depth: Int): Int {
        if (depth <= 0) {
//            return evaluate()
            return measureTimedValue { evaluate() }.let {
                evalTime += it.duration
                it.value
            }
        }

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
//            mvUnmakeTime += measureTime { board.unmakeMove(mv) }.toDouble(DurationUnit.MILLISECONDS)

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
