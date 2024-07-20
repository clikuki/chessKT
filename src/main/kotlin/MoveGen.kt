import kotlin.experimental.and

fun lsb(bb: ULong) = bb.takeLowestOneBit().let { it to it.countTrailingZeroBits() }

private fun generateOrthogonalMoves(
    moves: MutableList<Move>,
    data: MoveGenData,
) {
    var sliders = (data.board.getQueenBB() or data.board.getRookBB()) and data.allDiagPins.inv()
    if (sliders == 0UL) return

    var (lsb1, from) = lsb(sliders)
    while (lsb1 != 0UL) {
        val north = Rays.nort(lsb1, data.emptySqrs)
        val south = Rays.sout(lsb1, data.emptySqrs)
        val west = Rays.west(lsb1, data.emptySqrs)
        val east = Rays.east(lsb1, data.emptySqrs)
        var rays = (north or south or west or east) and data.emptyOrOppSqrs and data.moveMask
        if (lsb1 and data.orthoPins != 0UL) rays = rays and data.orthoPins

        if (rays != 0UL) {
            var (lsb2, to) = lsb(rays)
            while (rays != 0UL) {
                val type = if (lsb2 and data.oppPieces != 0UL) Move.CAPTURE else Move.QUIET
                moves.add(Move(from, to, type))

                rays = rays xor lsb2
                with(lsb(rays)) {
                    lsb2 = first
                    to = second
                }
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
    var sliders = (data.board.getQueenBB() or data.board.getBishopBB()) and data.orthoPins.inv()
    if (sliders == 0UL) return

    var (lsb1, from) = lsb(sliders)
    while (lsb1 != 0UL) {
        val noWe = Rays.noWe(lsb1, data.emptySqrs)
        val noEa = Rays.noEa(lsb1, data.emptySqrs)
        val soWe = Rays.soWe(lsb1, data.emptySqrs)
        val soEa = Rays.soEa(lsb1, data.emptySqrs)
        var rays = (noWe or noEa or soWe or soEa) and data.emptyOrOppSqrs and data.moveMask
        if (lsb1 and data.allDiagPins != 0UL) rays = rays and data.allDiagPins

        if (rays != 0UL) {
            var (lsb2, to) = lsb(rays)
            while (rays != 0UL) {
                val type = if (lsb2 and data.oppPieces != 0UL) Move.CAPTURE else Move.QUIET
                moves.add(Move(from, to, type))

                rays = rays xor lsb2
                with(lsb(rays)) {
                    lsb2 = first
                    to = second
                }
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
    val diagOffset = if (data.isWhite) -9 else 9
    val antiDiagOffset = if (data.isWhite) -7 else 7
    val shifter = if (data.isWhite) Shift::nort else Shift::sout
    val diagShifter = if (data.isWhite) Shift::noWe else Shift::soEa
    val antiDiagShifter = if (data.isWhite) Shift::noEa else Shift::soWe

    val promotionRank = if (data.isWhite) RANK_7 else RANK_2
    val nonPromoRanks = promotionRank.inv()
    val dblPushRank = if (data.isWhite) RANK_2 else RANK_7
    val dblPushMask = (data.emptySqrs and shifter(data.emptySqrs, 1)) and data.moveMask

    val pawns = data.board.getPawnBB()
    val forwardPawns = pawns and (data.allDiagPins or data.horzPins).inv()
    val diagPawns = pawns and (data.antiDiagonalPins or data.orthoPins).inv()
    val antiDiagPawns = pawns and (data.diagonalPins or data.orthoPins).inv()

    var normalPawns = shifter(forwardPawns and nonPromoRanks, 1) and data.emptySqrs and data.moveMask
    var promoPawns = shifter(forwardPawns and promotionRank, 1) and data.emptySqrs and data.moveMask
    var dblPushPawns = shifter(forwardPawns and dblPushRank, 2) and dblPushMask

    var normalCaptureA = diagShifter(diagPawns and nonPromoRanks) and data.oppPieces and data.moveMask
    var normalCaptureB = antiDiagShifter(antiDiagPawns and nonPromoRanks) and data.oppPieces and data.moveMask
    var promoCaptureA = diagShifter(diagPawns and promotionRank) and data.oppPieces and data.moveMask
    var promoCaptureB = antiDiagShifter(antiDiagPawns and promotionRank) and data.oppPieces and data.moveMask

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
    if (normalCaptureA != 0UL) {
        var (lsb, index) = lsb(normalCaptureA)
        while (lsb != 0UL) {
            moves.add(Move(from = index - diagOffset, to = index, type = Move.CAPTURE))

            normalCaptureA = normalCaptureA xor lsb
            with(lsb(normalCaptureA)) {
                lsb = first
                index = second
            }
        }
    }
    if (normalCaptureB != 0UL) {
        var (lsb, index) = lsb(normalCaptureB)
        while (lsb != 0UL) {
            moves.add(Move(from = index - antiDiagOffset, to = index, type = Move.CAPTURE))

            normalCaptureB = normalCaptureB xor lsb
            with(lsb(normalCaptureB)) {
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
    if (promoCaptureA != 0UL) {
        var (lsb, index) = lsb(promoCaptureA)
        while (lsb != 0UL) {
            generatePromotions(
                moves = moves,
                from = index - diagOffset,
                to = index,
                isCapture = true,
            )

            promoCaptureA = promoCaptureA xor lsb
            with(lsb(promoCaptureA)) {
                lsb = first
                index = second
            }
        }
    }
    if (promoCaptureB != 0UL) {
        var (lsb, index) = lsb(promoCaptureB)
        while (lsb != 0UL) {
            generatePromotions(
                moves = moves,
                from = index - antiDiagOffset,
                to = index,
                isCapture = true,
            )

            promoCaptureB = promoCaptureB xor lsb
            with(lsb(promoCaptureB)) {
                lsb = first
                index = second
            }
        }
    }

//    En passant
    data.board.enpassantTarget.let {
        if (it != -1 && data.moveMask shr it and 1UL == 1UL) {
            val epDestSqr = it + forwardOffset
            val epCaptureA = diagShifter(diagPawns) and (1UL shl epDestSqr)
            val epCaptureB = antiDiagShifter(antiDiagPawns) and (1UL shl epDestSqr)

//            Check for opponent ortho-sliders along the kings rank...
//            ...only when 1 pawn can capture, not both or none
            if ((epCaptureA == 0UL && epCaptureB != 0UL) || (epCaptureA != 0UL && epCaptureB == 0UL)) {
                var empty = data.emptySqrs or (1UL shl it)
                empty = empty or (((1UL shl (it - 1)) and NOT_H_FILE) and pawns)
                empty = empty or (((1UL shl (it + 1)) and NOT_A_FILE) and pawns)
                val ray = Rays.west(data.ownKingMask, empty) or Rays.east(data.ownKingMask, empty)

//                Exit early if pinned
                if (ray and (data.board.getQueenBB(data.oppClr) or data.board.getRookBB(data.oppClr)) != 0UL) {
                    return
                }
            }

            if (epCaptureA != 0UL) {
                moves.add(Move(epDestSqr - diagOffset, epDestSqr, type = Move.EP_CAPTURE))
            }
            if (epCaptureB != 0UL) {
                moves.add(Move(epDestSqr - antiDiagOffset, epDestSqr, type = Move.EP_CAPTURE))
            }
        }
    }
}

private fun generateKnightMoves(
    moves: MutableList<Move>,
    data: MoveGenData,
) {
    var knights = data.board.getKnightBB() and (data.orthoPins or data.allDiagPins).inv()
    if (knights == 0UL) return

    var (lsb1, from) = lsb(knights)
    while (lsb1 != 0UL) {
        var attacks = knightAttacks[from]!! and data.emptyOrOppSqrs and data.moveMask
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

private fun generateKingMoves(
    moves: MutableList<Move>,
    data: MoveGenData,
) {
//    Normal move
    var attacks = kingAttacks[data.ownKingSqr]!! and data.emptyOrOppSqrs and data.safeSqrs
    if (attacks != 0UL) {
        var (lsb, to) = lsb(attacks)
        while (lsb != 0UL) {
            val type = if (lsb and data.oppPieces != 0UL) Move.CAPTURE else Move.QUIET
            moves.add(Move(data.ownKingSqr, to, type))

            attacks = attacks xor lsb
            with(lsb(attacks)) {
                lsb = first
                to = second
            }
        }
    }

//    Castling
    if (!data.inCheck && data.board.castlingRights != 0.b) {
        val backrank = if (data.isWhite) RANK_1 else RANK_8
        val sideMask: Byte = if (data.isWhite) 0b1100 else 0b0011
        if (
            data.board.castlingRights and sideMask and 0b0101 != 0.b &&
            backrank and K_CASTLE_CHECK and (data.allPieces or data.attackedSqrs) == 0UL
        ) {
            moves.add(Move(data.ownKingSqr, to = data.ownKingSqr + 2, type = Move.K_CASTLE))
        }
        if (
            data.board.castlingRights and sideMask and 0b1010 != 0.b &&
            backrank and Q_CASTLE_PASS and data.attackedSqrs == 0UL &&
            backrank and Q_CASTLE_OBSTRUCT and data.allPieces == 0UL
        ) {
            moves.add(Move(data.ownKingSqr, to = data.ownKingSqr - 2, type = Move.Q_CASTLE))
        }
    }
}

data class MoveGenData(
    val board: Board,
) {
    var inCheck = false
    var inDoubleCheck = false

    var isWhite = false
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
    var checkers = 0UL
    var moveMask = 0UL

    var horzPins = 0UL
    var diagonalPins = 0UL
    var antiDiagonalPins = 0UL
    var orthoPins = 0UL
    var allDiagPins = 0UL

    init {
        update()
    }

    fun update() {
        inCheck = false
        inDoubleCheck = false

        isWhite = board.side == Piece.WHITE
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
        horzPins = 0UL
        orthoPins = 0UL
        diagonalPins = 0UL

        updateBitboards()
    }

    private fun updateBitboards() {
        val noKing = emptySqrs or ownKingMask
        val orthoSliders = (board.getQueenBB(oppClr) or board.getRookBB(oppClr))
        val diagonalSliders = (board.getQueenBB(oppClr) or board.getBishopBB(oppClr))
        val pawns = board.getPawnBB(oppClr)
        val knights = board.getKnightBB(oppClr)

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
        attackedSqrs = attackedSqrs or (if (isWhite) Shift::soWe else Shift::noWe)(pawns)
        attackedSqrs = attackedSqrs or (if (isWhite) Shift::soEa else Shift::noEa)(pawns)

        safeSqrs = attackedSqrs.inv()
        inCheck = attackedSqrs and ownKingMask != 0UL

//        Checkers and rays calculation
        if (inCheck) {
            checkers = knightAttacks[ownKingSqr]!! and knights
            checkers = checkers or ((if (isWhite) Shift::noWe else Shift::soWe)(ownKingMask) and pawns)
            checkers = checkers or ((if (isWhite) Shift::noEa else Shift::soEa)(ownKingMask) and pawns)

            val nortRay = Rays.nort(ownKingMask, emptySqrs)
            val soutRay = Rays.sout(ownKingMask, emptySqrs)
            val westRay = Rays.west(ownKingMask, emptySqrs)
            val eastRay = Rays.east(ownKingMask, emptySqrs)
            checkers = checkers or (nortRay and orthoSliders)
            checkers = checkers or (soutRay and orthoSliders)
            checkers = checkers or (westRay and orthoSliders)
            checkers = checkers or (eastRay and orthoSliders)

            val noWeRay = Rays.noWe(ownKingMask, emptySqrs)
            val noEaRay = Rays.noEa(ownKingMask, emptySqrs)
            val soWeRay = Rays.soWe(ownKingMask, emptySqrs)
            val soEaRay = Rays.soEa(ownKingMask, emptySqrs)
            checkers = checkers or (noWeRay and diagonalSliders)
            checkers = checkers or (noEaRay and diagonalSliders)
            checkers = checkers or (soWeRay and diagonalSliders)
            checkers = checkers or (soEaRay and diagonalSliders)

            if (checkers.countOneBits() > 1) {
//                Unblockable, king needs to move
                inDoubleCheck = true
            } else {
//                Allow attack ray to be blocked or for checker to be captured
                moveMask =
                    if ((orthoSliders or diagonalSliders) and checkers == 0UL) {
//                        Knight and pawn checks cannot be blocked
                        checkers
                    } else {
//                        Queen, rook, bishop checks
                        when (checkers) {
                            nortRay and checkers -> nortRay
                            soutRay and checkers -> soutRay
                            westRay and checkers -> westRay
                            eastRay and checkers -> eastRay
                            noWeRay and checkers -> noWeRay
                            noEaRay and checkers -> noEaRay
                            soWeRay and checkers -> soWeRay
                            soEaRay and checkers -> soEaRay
                            else -> throw Error("IMPOSSIBLE STATE")
                        }
                    }
            }
        }

//        Pin calculations
        if (!inDoubleCheck) {
            horzPins = calculatePin(Rays::west, orthoSliders)
            horzPins = horzPins or calculatePin(Rays::east, orthoSliders)
            orthoPins = horzPins or calculatePin(Rays::nort, orthoSliders) or calculatePin(Rays::sout, orthoSliders)
            diagonalPins = calculatePin(Rays::noWe, diagonalSliders) or calculatePin(Rays::soEa, diagonalSliders)
            antiDiagonalPins = calculatePin(Rays::noEa, diagonalSliders) or calculatePin(Rays::soWe, diagonalSliders)
            allDiagPins = diagonalPins or antiDiagonalPins
        }
    }

    private fun calculatePin(
        filler: (ULong, ULong) -> ULong,
        sliders: ULong,
    ): ULong {
        val possiblePinned = filler(ownKingMask, emptySqrs) and ownPieces
        if (possiblePinned == 0UL) return 0UL

        val ray = filler(ownKingMask, emptySqrs or possiblePinned)
        return if (sliders and ray == 0UL) 0UL else ray
    }
}

class MoveGen(
    board: Board,
) {
    val data: MoveGenData = MoveGenData(board)
    var moves = emptyList<Move>()

    init {
        generateMoves()
    }

    fun generateMoves(): List<Move> {
        val newMoves = mutableListOf<Move>()
        data.update()

        generateKingMoves(newMoves, data)

        if (!data.inDoubleCheck) {
            generateOrthogonalMoves(newMoves, data)
            generateDiagonalMoves(newMoves, data)
            generatePawnMoves(newMoves, data)
            generateKnightMoves(newMoves, data)
        }

        moves = newMoves
        return newMoves
    }
}

fun main() {
//    //    Single check
//    val depth = 6
//    val board = Board.from("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
//    println("$depth: ${perft(MoveGen(board), depth, emptyList())}")

    //    Divide by move
    val depth = 3
    val board = Board.from("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1R1K b kq - 1 1")
//    g1h1 > b2a1r > d1a1
//    val board = Board.from("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/P2P2PP/r2Q1R1K w kq - 0 2")
    val moveGen = MoveGen(board)
    var totalNodes = 0UL
    for (move in moveGen.generateMoves()) {
        board.makeMove(move)
        val nodeCnt = perft(MoveGen(board), depth - 1, emptyList())
        totalNodes += nodeCnt
        val promoTo =
            when (move.type and 0b1011) {
                0b1000.b -> "n"
                0b1001.b -> "b"
                0b1010.b -> "r"
                0b1011.b -> "q"
                else -> ""
            }
        println("${Board.indexToSqr(move.from)}${Board.indexToSqr(move.to)}$promoTo: $nodeCnt")
        board.unmakeMove(move)
    }
    println("\nTotal nodes: $totalNodes")

//    //    Check in depths
//    for (i in 1..5) {
//        val b = Board.from("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1")
//        val nodeCnt = perft(MoveGen(b), i, emptyList())
//        println("$i : $nodeCnt")
//    }
}
