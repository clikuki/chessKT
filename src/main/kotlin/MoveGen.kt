fun lsb(bb: ULong) = bb.takeLowestOneBit().let { it to it.countTrailingZeroBits() }

private fun generateOrthogonalMoves(
    board: Board,
    moves: MutableList<Move>,
) {
    var sliders = board.getQueenBB() or board.getRookBB()
    if (sliders == 0UL) return

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
            moves.add(Move(from, to, type = Move.QUIET))

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
    if (sliders == 0UL) return

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
            moves.add(Move(from, to, type = Move.QUIET))

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

private fun generatePromotions(
    moves: MutableList<Move>,
    from: Int,
    to: Int,
    isCapture: Boolean,
) {
    moves.add(Move(from, to, type = if (isCapture) Move.N_PROMO_CAPTURE else Move.N_PROMOTE))
    moves.add(Move(from, to, type = if (isCapture) Move.B_PROMO_CAPTURE else Move.B_PROMOTE))
    moves.add(Move(from, to, type = if (isCapture) Move.R_PROMO_CAPTURE else Move.R_PROMOTE))
    moves.add(Move(from, to, type = if (isCapture) Move.Q_PROMO_CAPTURE else Move.Q_PROMOTE))
}

private fun generatePawnMoves(
    board: Board,
    moves: MutableList<Move>,
) {
    val isWhite = board.side == Piece.WHITE
    val forwardOffset = if (isWhite) -8 else 8
    val shifter = if (isWhite) Shift::nort else Shift::sout

    val promotionRank = if (isWhite) RANK_8 else RANK_1
    val nonPromoRanks = promotionRank.inv()
    val dblPushRank = if (isWhite) RANK_2 else RANK_7

    val pawns = board.getPawnBB()
    val capturable = board.getOpponentBB()
    val emptySqrs = board.occupancyBB.inv()

    var normalPawns = shifter(pawns and nonPromoRanks, 1) and emptySqrs
    var promoPawns = shifter(pawns and promotionRank, 1) and emptySqrs
    var dblPushPawns = shifter(pawns and dblPushRank, 2) and (emptySqrs and shifter(emptySqrs, 1))

    val left = if (isWhite) Shift::noWe else Shift::soWe
    val right = if (isWhite) Shift::noEa else Shift::soEa
    var normalCaptureLeft = left(pawns and nonPromoRanks) and capturable
    var normalCaptureRight = right(pawns and nonPromoRanks) and capturable
    var promoCaptureLeft = left(pawns and promotionRank) and capturable
    var promoCaptureRight = right(pawns and promotionRank) and capturable

//    Normal push
    if (normalPawns != 0UL) {
        var (lsb, index) = lsb(normalPawns)
        while (lsb != 0UL) {
            moves.add(Move(from = index - forwardOffset, to = index, type = Move.QUIET))

            normalPawns = normalPawns xor lsb
            with(lsb(normalPawns)) {
                lsb = first
                index = second
            }
        }
    }

//    Double push
    if (dblPushPawns != 0UL) {
        var (lsb, index) = lsb(dblPushPawns)
        while (lsb != 0UL) {
            moves.add(
                Move(
                    from = index - forwardOffset - forwardOffset,
                    to = index,
                    type = Move.DBL_PUSH,
                ),
            )

            dblPushPawns = dblPushPawns xor lsb
            with(lsb(dblPushPawns)) {
                lsb = first
                index = second
            }
        }
    }

//    Normal capture
    if (normalCaptureLeft != 0UL) {
        var (lsb, index) = lsb(normalCaptureLeft)
        while (lsb != 0UL) {
            moves.add(Move(from = index - forwardOffset + 1, to = index, type = Move.CAPTURE))

            normalCaptureLeft = normalCaptureLeft xor lsb
            with(lsb(normalCaptureLeft)) {
                lsb = first
                index = second
            }
        }
    }
    if (normalCaptureRight != 0UL) {
        var (lsb, index) = lsb(normalCaptureRight)
        while (lsb != 0UL) {
            moves.add(Move(from = index - forwardOffset - 1, to = index, type = Move.CAPTURE))

            normalCaptureRight = normalCaptureRight xor lsb
            with(lsb(normalCaptureRight)) {
                lsb = first
                index = second
            }
        }
    }

//    Promo push
    if (promoPawns != 0UL) {
        var (lsb, index) = lsb(promoPawns)
        while (lsb != 0UL) {
            generatePromotions(
                moves = moves,
                from = index - forwardOffset,
                to = index,
                isCapture = false,
            )

            promoPawns = promoPawns xor lsb
            with(lsb(promoPawns)) {
                lsb = first
                index = second
            }
        }
    }

//    Promo capture
    if (promoCaptureLeft != 0UL) {
        var (lsb, index) = lsb(promoCaptureLeft)
        while (lsb != 0UL) {
            generatePromotions(
                moves = moves,
                from = index - forwardOffset + 1,
                to = index,
                isCapture = true,
            )

            promoCaptureLeft = promoCaptureLeft xor lsb
            with(lsb(promoCaptureLeft)) {
                lsb = first
                index = second
            }
        }
    }
    if (promoCaptureRight != 0UL) {
        var (lsb, index) = lsb(promoCaptureRight)
        while (lsb != 0UL) {
            generatePromotions(
                moves = moves,
                from = index - forwardOffset - 1,
                to = index,
                isCapture = true,
            )

            promoCaptureRight = promoCaptureRight xor lsb
            with(lsb(promoCaptureRight)) {
                lsb = first
                index = second
            }
        }
    }

//    TODO: Support en passant
}

private fun generateKnightMoves(
    board: Board,
    moves: MutableList<Move>,
) {
    var knights = board.getKnightBB()
    val nonblockers = board.getColorBB().inv()
    val opp = board.getOpponentBB()
    if (knights != 0UL) {
        var (lsb1, from) = lsb(knights)
        while (lsb1 != 0UL) {
            var attacks = knightAttacks[from]!! and nonblockers
            var (lsb2, to) = lsb(attacks)
            while (lsb2 != 0UL) {
                val type = if (lsb2 and opp != 0UL) Move.CAPTURE else Move.QUIET
                moves.add(Move(from, to, type))

                attacks = attacks xor lsb2
                with(lsb(attacks)) {
                    lsb2 = first
                    to = second
                }
            }

            knights = knights xor lsb1
            with(lsb(knights)) {
                lsb1 = first
                from = second
            }
        }
    }
}

object MoveGen {
    fun pseudoLegal(board: Board): List<Move> {
        val moves = mutableListOf<Move>()
        generateOrthogonalMoves(board, moves)
        generateDiagonalMoves(board, moves)
        generatePawnMoves(board, moves)
        generateKnightMoves(board, moves)
        return moves
    }
}

private fun main() {
//    TODO: Perft tests
}
