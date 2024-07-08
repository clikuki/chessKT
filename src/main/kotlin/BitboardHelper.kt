private const val NOT_A_FILE = 0xfefefefefefefefeUL
private const val NOT_H_FILE = 0x7f7f7f7f7f7f7f7fUL

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
    fun nort(a: ULong) = a shr 8

    fun sout(a: ULong) = a shl 8

    fun west(a: ULong) = (a shr 1) and NOT_H_FILE

    fun east(a: ULong) = (a shl 1) and NOT_A_FILE

    fun noEa(b: ULong) = (b shl 9) and NOT_A_FILE

    fun soEa(b: ULong) = (b shr 7) and NOT_A_FILE

    fun soWe(b: ULong) = (b shr 9) and NOT_H_FILE

    fun noWe(b: ULong) = (b shl 7) and NOT_H_FILE
}
