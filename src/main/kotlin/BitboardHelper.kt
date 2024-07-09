const val NOT_A_FILE = 0xfefefefefefefefeUL
const val NOT_H_FILE = 0x7f7f7f7f7f7f7f7fUL
const val K_CASTLE_CHECK = 0x6000000000000060UL
const val Q_CASTLE_CHECK = 0xe0000000000000eUL
const val RANK_1 = 0xff00000000000000UL
const val RANK_2 = 0xff000000000000UL
const val RANK_7 = 0xff00UL
const val RANK_8 = 0xffUL

val knightAttacks =
    buildMap {
        val notGHfiles = NOT_H_FILE and (NOT_H_FILE shr 1)
        val notABfiles = NOT_A_FILE and (NOT_A_FILE shl 1)
        val notRank8 = RANK_8.inv()
        val notRanks8or7 = (RANK_8 or RANK_7).inv()

        for (y in 0..7) {
            for (x in 0..7) {
                val i = y * 8 + x
                var attacks = 1UL shl (i + 10) and notABfiles and notRank8
                attacks = attacks or (1UL shl (i + 17) and NOT_A_FILE and notRanks8or7)
                attacks = attacks or (((1UL shl i) shr 6) and notABfiles)
                attacks = attacks or (((1UL shl i) shr 15) and NOT_A_FILE)
                attacks = attacks or (1UL shl (i + 15) and NOT_H_FILE and notRanks8or7)
                attacks = attacks or (1UL shl (i + 6) and notGHfiles and notRank8)
                attacks = attacks or (((1UL shl i) shr 10) and notGHfiles)
                attacks = attacks or (((1UL shl i) shr 17) and NOT_H_FILE)

                set(i, attacks)
            }
        }
    }

val kingAttacks =
    buildMap {
        val notRank8 = RANK_8.inv()

        for (y in 0..7) {
            for (x in 0..7) {
                val i = y * 8 + x
                var attacks = 1UL shl (i - 9) and NOT_H_FILE
                attacks = attacks or (1UL shl (i - 8))
                attacks = attacks or (1UL shl (i - 7) and NOT_A_FILE)
                attacks = attacks or (1UL shl (i - 1) and NOT_H_FILE)
                attacks = attacks or (1UL shl (i + 1) and NOT_A_FILE)
                attacks = attacks or (1UL shl (i + 7) and notRank8 and NOT_H_FILE)
                attacks = attacks or (1UL shl (i + 8) and notRank8)
                attacks = attacks or (1UL shl (i + 9) and notRank8 and NOT_A_FILE)

                set(i, attacks)
            }
        }
    }

// PLAIN FILLS
object Fill {
    fun south(a: ULong): ULong {
        var gen = a or (a shl 8)
        gen = gen or (gen shl 16)
        return gen or (gen shl 32)
    }

    fun north(a: ULong): ULong {
        var gen = a or (a shr 8)
        gen = gen or (gen shr 16)
        return gen or (gen shr 32)
    }

    fun east(a: ULong): ULong {
        val pr0 = NOT_A_FILE
        val pr1 = pr0 and (pr0 shl 1)
        val pr2 = pr1 and (pr1 shl 2)
        var gen = a or (pr0 and (a shl 1))
        gen = gen or (pr1 and (gen shl 2))
        return gen or (pr2 and (gen shl 4))
    }

    fun soEa(a: ULong): ULong {
        val pr0 = NOT_A_FILE
        val pr1 = pr0 and (pr0 shl 9)
        val pr2 = pr1 and (pr1 shl 18)

        var gen = a or (pr0 and (a shl 9))
        gen = gen or (pr1 and (gen shl 18))
        return gen or (pr2 and (gen shl 36))
    }

    fun noEa(a: ULong): ULong {
        val pr0 = NOT_A_FILE
        val pr1 = pr0 and (pr0 shr 7)
        val pr2 = pr1 and (pr1 shr 14)
        var gen = a or (pr0 and (a shr 7))
        gen = gen or (pr1 and (gen shr 14))
        return gen or (pr2 and (gen shr 28))
    }

    fun west(a: ULong): ULong {
        val pr0 = NOT_H_FILE
        val pr1 = pr0 and (pr0 shr 1)
        val pr2 = pr1 and (pr1 shr 2)
        var gen = a or (pr0 and (a shr 1))
        gen = gen or (pr1 and (gen shr 2))
        return gen or (pr2 and (gen shr 4))
    }

    fun noWe(a: ULong): ULong {
        val pr0 = NOT_H_FILE
        val pr1 = pr0 and (pr0 shr 9)
        val pr2 = pr1 and (pr1 shr 18)
        var gen = a or (pr0 and (a shr 9))
        gen = gen or (pr1 and (gen shr 18))
        return gen or (pr2 and (gen shr 36))
    }

    fun soWe(a: ULong): ULong {
        val pr0 = NOT_H_FILE
        val pr1 = pr0 and (pr0 shl 7)
        val pr2 = pr1 and (pr1 shl 14)
        var gen = a or (pr0 and (a shl 7))
        gen = gen or (pr1 and (gen shl 14))
        return gen or (pr2 and (gen shl 21))
    }
}

// OCCLUDED FILLS
object Occl {
    fun nort(
        a: ULong,
        b: ULong,
    ): ULong {
        var gen = a or (b and (a shr 8))
        var pro = b and (b shr 8)
        gen = gen or (pro and (gen shr 16))
        pro = pro and (pro shr 16)
        return gen or (pro and (gen shr 32))
    }

    fun sout(
        a: ULong,
        b: ULong,
    ): ULong {
        var gen = a or (b and (a shl 8))
        var pro = b and (b shl 8)
        gen = gen or (pro and (gen shl 16))
        pro = pro and (pro shl 16)
        return gen or (pro and (gen shl 32))
    }

    fun east(
        a: ULong,
        b: ULong,
    ): ULong {
        var pro = b and NOT_A_FILE
        var gen = a or (pro and (a shl 1))
        pro = pro and (pro shl 1)
        gen = gen or (pro and (gen shl 2))
        pro = pro and (pro shl 2)
        return gen or (pro and (gen shl 4))
    }

    fun west(
        a: ULong,
        b: ULong,
    ): ULong {
        var pro = b and NOT_H_FILE
        var gen = a or (pro and (a shr 1))
        pro = pro and (pro shr 1)
        gen = gen or (pro and (gen shr 2))
        pro = pro and (pro shr 2)
        return gen or (pro and (gen shr 4))
    }

    fun soEa(
        a: ULong,
        b: ULong,
    ): ULong {
        var pro = b and NOT_A_FILE
        var gen = a or (pro and (a shl 9))
        pro = pro and (pro shl 9)
        gen = gen or (pro and (gen shl 18))
        pro = pro and (pro shl 18)
        return gen or (pro and (gen shl 36))
    }

    fun noEa(
        a: ULong,
        b: ULong,
    ): ULong {
        var pro = b and NOT_A_FILE
        var gen = a or (pro and (a shr 7))
        pro = pro and (pro shr 7)
        gen = gen or (pro and (gen shr 14))
        pro = pro and (pro shr 14)
        return gen or (pro and (gen shr 28))
    }

    fun noWe(
        a: ULong,
        b: ULong,
    ): ULong {
        var pro = b and NOT_H_FILE
        var gen = a or (pro and (a shr 9))
        pro = pro and (pro shr 9)
        gen = gen or (pro and (gen shr 18))
        pro = pro and (pro shr 18)
        gen = gen or (pro and (gen shr 36))
        return gen
    }

    fun soWe(
        a: ULong,
        b: ULong,
    ): ULong {
        var pro = b and NOT_H_FILE
        var gen = a or (pro and (a shl 7))
        pro = pro and (pro shl 7)
        gen = gen or (pro and (gen shl 14))
        pro = pro and (pro shl 14)
        gen = gen or (pro and (gen shl 28))
        return gen
    }
}

// SHIFT BY ONE
object Shift {
    fun nort(
        a: ULong,
        by: Int,
    ) = a shr (8 * by)

    fun sout(
        a: ULong,
        by: Int,
    ) = a shl (8 * by)

    fun west(a: ULong) = (a shr 1) and NOT_H_FILE

    fun east(a: ULong) = (a shl 1) and NOT_A_FILE

    fun soEa(b: ULong) = (b shl 9) and NOT_A_FILE

    fun noEa(b: ULong) = (b shr 7) and NOT_A_FILE

    fun noWe(b: ULong) = (b shr 9) and NOT_H_FILE

    fun soWe(b: ULong) = (b shl 7) and NOT_H_FILE
}
