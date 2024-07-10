import kotlin.experimental.and

fun lsb(bb: ULong) = bb.takeLowestOneBit().let { it to it.countTrailingZeroBits() }

private fun generateOrthogonalMoves(
    moves: MutableList<Move>,
    data: MoveGenData,
) {
    var sliders = data.board.getQueenBB() or data.board.getRookBB()
    if (sliders == 0UL) return

    var (lsb1, from) = lsb(sliders)
    while (lsb1 != 0UL) {
        val north = Occl.nort(lsb1, (data.ownPieces or Shift.nort(data.oppPieces, 1)).inv())
        val south = Occl.sout(lsb1, (data.ownPieces or Shift.sout(data.oppPieces, 1)).inv())
        val west = Occl.west(lsb1, (data.ownPieces or Shift.west(data.oppPieces)).inv())
        val east = Occl.east(lsb1, (data.ownPieces or Shift.east(data.oppPieces)).inv())
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
    moves: MutableList<Move>,
    data: MoveGenData,
) {
    var sliders = data.board.getQueenBB() or data.board.getBishopBB()
    if (sliders == 0UL) return

    var (lsb1, from) = lsb(sliders)
    while (lsb1 != 0UL) {
        val noWe = Occl.noWe(lsb1, (data.ownPieces or Shift.noWe(data.oppPieces)).inv())
        val noEa = Occl.noEa(lsb1, (data.ownPieces or Shift.noEa(data.oppPieces)).inv())
        val soWe = Occl.soWe(lsb1, (data.ownPieces or Shift.soWe(data.oppPieces)).inv())
        val soEa = Occl.soEa(lsb1, (data.ownPieces or Shift.soEa(data.oppPieces)).inv())
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
    moves: MutableList<Move>,
    data: MoveGenData,
) {
    val forwardOffset = if (data.isWhite) -8 else 8
    val shifter = if (data.isWhite) Shift::nort else Shift::sout

    val promotionRank = if (data.isWhite) RANK_8 else RANK_1
    val nonPromoRanks = promotionRank.inv()
    val dblPushRank = if (data.isWhite) RANK_2 else RANK_7

    val pawns = data.board.getPawnBB()
    var normalPawns = shifter(pawns and nonPromoRanks, 1) and data.emptySqrs
    var promoPawns = shifter(pawns and promotionRank, 1) and data.emptySqrs
    var dblPushPawns = shifter(pawns and dblPushRank, 2) and (data.emptySqrs and shifter(data.emptySqrs, 1))

    val left = if (data.isWhite) Shift::noWe else Shift::soWe
    val right = if (data.isWhite) Shift::noEa else Shift::soEa
    var normalCaptureLeft = left(pawns and nonPromoRanks) and data.oppPieces
    var normalCaptureRight = right(pawns and nonPromoRanks) and data.oppPieces
    var promoCaptureLeft = left(pawns and promotionRank) and data.oppPieces
    var promoCaptureRight = right(pawns and promotionRank) and data.oppPieces

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

    if (data.board.enpassantTarget != -1) {
        val leftPawn = data.board.enpassantTarget - 1
        val rightPawn = data.board.enpassantTarget + 1
        val to = data.board.enpassantTarget + forwardOffset
        if ((pawns shr leftPawn) and 1UL == 1UL) {
            moves.add(Move(leftPawn, to, type = Move.EP_CAPTURE))
        }
        if ((pawns shr rightPawn) and 1UL == 1UL) {
            moves.add(Move(rightPawn, to, type = Move.EP_CAPTURE))
        }
    }
}

private fun generateKnightMoves(
    moves: MutableList<Move>,
    data: MoveGenData,
) {
    var knights = data.board.getKnightBB()
    if (knights != 0UL) {
        var (lsb1, from) = lsb(knights)
        while (lsb1 != 0UL) {
            var attacks = knightAttacks[from]!! and data.emptyOrOppSqrs
            var (lsb2, to) = lsb(attacks)
            while (lsb2 != 0UL) {
                val type = if (lsb2 and data.oppPieces != 0UL) Move.CAPTURE else Move.QUIET
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

private fun generateKingMoves(
    moves: MutableList<Move>,
    data: MoveGenData,
) {
    val king = data.board.getKingBB()
    val (_, from) = lsb(king)

//    Normal move
    var attacks = kingAttacks[from]!! and data.emptySqrs
    if (attacks != 0UL) {
        var (lsb, to) = lsb(attacks)
        while (lsb != 0UL) {
            val type = if (lsb and data.oppPieces != 0UL) Move.CAPTURE else Move.QUIET
            moves.add(Move(from, to, type))

            attacks = attacks xor lsb
            with(lsb(attacks)) {
                lsb = first
                to = second
            }
        }
    }

//    Castling
    if (data.board.castlingRights != 0.b) {
        val isWhite = data.board.side == Piece.WHITE
        val backrank = if (isWhite) RANK_1 else RANK_8
        val sideMask: Byte = if (isWhite) 0b1100 else 0b0011
        if (
            data.board.castlingRights and sideMask and 0b0101 != 0.b &&
            backrank and K_CASTLE_CHECK and data.ownPieces == 0UL
        ) {
            moves.add(Move(from, to = from + 2, type = Move.K_CASTLE))
        }
        if (
            data.board.castlingRights and sideMask and 0b1010 != 0.b &&
            backrank and Q_CASTLE_CHECK and data.ownPieces == 0UL
        ) {
            moves.add(Move(from, to = from - 2, type = Move.Q_CASTLE))
        }
    }
}

data class MoveGenData(
    val board: Board,
) {
    var inCheck = false
    var inDoubleCheck = false
    var checkRayBitmask = 0UL
    var pinRays = 0UL

    // Store some info for convenience
    val isWhite = board.side == Piece.WHITE
    val ownClr = board.side
    val oppClr = if (isWhite) Piece.BLACK else Piece.WHITE
    val ownKingSqr = lsb(board.getKingBB()).second

    // Store some bitboards for convenience
    val oppPieces = board.getOpponentBB()
    val ownPieces = board.getColorBB()
    val allPieces = board.occupancyBB
    val emptySqrs = allPieces.inv()
    val emptyOrOppSqrs = emptySqrs or oppPieces
}

object MoveGen {
    fun generateMoves(board: Board): List<Move> {
        val moves = mutableListOf<Move>()
        val moveGenData = MoveGenData(board)

        generateKingMoves(moves, moveGenData)
        generateOrthogonalMoves(moves, moveGenData)
        generateDiagonalMoves(moves, moveGenData)
        generatePawnMoves(moves, moveGenData)
        generateKnightMoves(moves, moveGenData)

        return moves
    }
}

private fun main() {
//    TODO: Perft tests
}
