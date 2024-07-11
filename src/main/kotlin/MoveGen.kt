import kotlin.experimental.and

fun lsb(bb: ULong) = bb.takeLowestOneBit().let { it to it.countTrailingZeroBits() }

private fun knightFill(knights: ULong): ULong {
    var east = Shift.east(knights)
    var west = Shift.west(knights)
    var attacks = east or west shl 16
    attacks = attacks or ((east or west) shr 16)
    east = Shift.east(east)
    west = Shift.west(west)
    attacks = attacks or ((east or west) shl 8)
    return attacks or ((east or west) shr 8)
}

private object Rays {
    fun nort(
        gen: ULong,
        pro: ULong,
    ) = Shift.nort(Occl.nort(gen, pro), 1)

    fun sout(
        gen: ULong,
        pro: ULong,
    ) = Shift.sout(Occl.sout(gen, pro), 1)

    fun west(
        gen: ULong,
        pro: ULong,
    ) = Shift.west(Occl.west(gen, pro))

    fun east(
        gen: ULong,
        pro: ULong,
    ) = Shift.east(Occl.east(gen, pro))

    fun noWe(
        gen: ULong,
        pro: ULong,
    ) = Shift.noWe(Occl.noWe(gen, pro))

    fun noEa(
        gen: ULong,
        pro: ULong,
    ) = Shift.noEa(Occl.noEa(gen, pro))

    fun soWe(
        gen: ULong,
        pro: ULong,
    ) = Shift.soWe(Occl.soWe(gen, pro))

    fun soEa(
        gen: ULong,
        pro: ULong,
    ) = Shift.soEa(Occl.soEa(gen, pro))
}

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
    var attacks = kingAttacks[from]!! and data.emptyOrOppSqrs and data.safeSqrs
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
    if (!data.inCheck && data.board.castlingRights != 0.b) {
        val isWhite = data.board.side == Piece.WHITE
        val backrank = if (isWhite) RANK_1 else RANK_8
        val sideMask: Byte = if (isWhite) 0b1100 else 0b0011
        if (
            data.board.castlingRights and sideMask and 0b0101 != 0.b &&
            backrank and K_CASTLE_CHECK and (data.ownPieces or data.attackedSqrs) == 0UL
        ) {
            moves.add(Move(from, to = from + 2, type = Move.K_CASTLE))
        }
        if (
            data.board.castlingRights and sideMask and 0b1010 != 0.b &&
            backrank and Q_CASTLE_CHECK and (data.ownPieces or data.attackedSqrs) == 0UL
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

    var isWhite = false
    var ownClr: Byte = 0
    var oppClr: Byte = 0
    var ownKingMask = 0UL
    var ownKingSqr = 0
    var oppKingMask = 0UL
    var oppKingSqr = 0

    var oppPieces = 0UL
    var ownPieces = 0UL
    var allPieces = 0UL
    var emptySqrs = 0UL
    var emptyOrOppSqrs = 0UL

    var attackedSqrs = 0UL
    var safeSqrs = 0UL
    var attackers = 0UL
    var moveMask = 0UL

    init {
        update()
    }

    fun update() {
        inCheck = false
        inDoubleCheck = false

        isWhite = board.side == Piece.WHITE
        ownClr = board.side
        oppClr = if (isWhite) Piece.BLACK else Piece.WHITE
        ownKingMask = board.getKingBB()
        ownKingSqr = ownKingMask.countTrailingZeroBits()
        oppKingMask = board.getKingBB(oppClr)
        oppKingSqr = oppKingMask.countTrailingZeroBits()

        oppPieces = board.getOpponentBB()
        ownPieces = board.getColorBB()
        allPieces = board.occupancyBB
        emptySqrs = allPieces.inv()
        emptyOrOppSqrs = emptySqrs or oppPieces
        moveMask = 0xffffffffffffffffUL

        generateOppAttacks()
    }

    private fun generateOppAttacks() {
        val noKing = emptySqrs or ownKingMask
        val orthoSliders = (board.getQueenBB(oppClr) or board.getRookBB(oppClr))
        val diagonalSliders = (board.getQueenBB(oppClr) or board.getBishopBB(oppClr))
        val pawns = board.getPawnBB(oppClr)
        val knights = board.getKnightBB(oppClr)
        val left = if (isWhite) Shift::soWe else Shift::noWe
        val right = if (isWhite) Shift::soEa else Shift::noEa

        attackedSqrs = Rays.nort(orthoSliders, noKing)
        attackedSqrs = attackedSqrs or Rays.sout(orthoSliders, noKing)
        attackedSqrs = attackedSqrs or Rays.west(orthoSliders, noKing)
        attackedSqrs = attackedSqrs or Rays.east(orthoSliders, noKing)
        attackedSqrs = attackedSqrs or Rays.noWe(diagonalSliders, noKing)
        attackedSqrs = attackedSqrs or Rays.noEa(diagonalSliders, noKing)
        attackedSqrs = attackedSqrs or Rays.soWe(diagonalSliders, noKing)
        attackedSqrs = attackedSqrs or Rays.soEa(diagonalSliders, noKing)

        attackedSqrs = attackedSqrs or knightFill(knights)
        attackedSqrs = attackedSqrs or kingAttacks[oppKingSqr]!!
        attackedSqrs = attackedSqrs or left(pawns) or right(pawns)

        safeSqrs = attackedSqrs.inv()
        inCheck = attackedSqrs and ownKingMask != 0UL

        if (inCheck) {
            var attackers = knightAttacks[ownKingSqr]!! and knights
            attackers = attackers or ((left(ownKingMask) or right(ownKingMask)) and pawns)

            val nortRay = Rays.nort(ownKingMask, emptySqrs)
            val soutRay = Rays.sout(ownKingMask, emptySqrs)
            val westRay = Rays.west(ownKingMask, emptySqrs)
            val eastRay = Rays.east(ownKingMask, emptySqrs)
            attackers = attackers or (nortRay and orthoSliders)
            attackers = attackers or (soutRay and orthoSliders)
            attackers = attackers or (westRay and orthoSliders)
            attackers = attackers or (eastRay and orthoSliders)

            val noWeRay = Rays.noWe(ownKingMask, emptySqrs)
            val noEaRay = Rays.noEa(ownKingMask, emptySqrs)
            val soWeRay = Rays.soWe(ownKingMask, emptySqrs)
            val soEaRay = Rays.soEa(ownKingMask, emptySqrs)
            attackers = attackers or (noWeRay and diagonalSliders)
            attackers = attackers or (noEaRay and diagonalSliders)
            attackers = attackers or (soWeRay and diagonalSliders)
            attackers = attackers or (soEaRay and diagonalSliders)

            val checkerCnt = attackers.countOneBits()
            if (checkerCnt > 1) {
                inDoubleCheck = true
            } else if (checkerCnt == 1) {
                moveMask = attackers

                // Add rays
                if ((orthoSliders or diagonalSliders) and attackers != 0UL) {
                    moveMask = moveMask or
                        when (attackers) {
                            nortRay and attackers -> nortRay
                            soutRay and attackers -> soutRay
                            westRay and attackers -> westRay
                            eastRay and attackers -> eastRay
                            noWeRay and attackers -> noWeRay
                            noEaRay and attackers -> noEaRay
                            soWeRay and attackers -> soWeRay
                            soEaRay and attackers -> soEaRay
                            else -> 0UL
                        }
                }
            }
        }
    }
}

class MoveGen(
    board: Board,
) {
    val data: MoveGenData = MoveGenData(board)

    fun generateMoves(): List<Move> {
        val moves = mutableListOf<Move>()
        data.update()

        generateKingMoves(moves, data)

        if (!data.inDoubleCheck) {
            generateOrthogonalMoves(moves, data)
            generateDiagonalMoves(moves, data)
            generatePawnMoves(moves, data)
            generateKnightMoves(moves, data)
        }

        return moves
    }
}

private fun main() {
//    TODO: Perft tests
}
