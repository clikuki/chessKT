fun perft(
    moveGen: MoveGen,
    depth: Int,
    path: List<Move>,
): ULong {
//    println(path.map { Board.indexToSqr(it.from) + Board.indexToSqr(it.to) })

    if (depth == 0) return 1UL

    var nodes = 0UL
    val moves = moveGen.generateMoves()
    var i = 0
    try {
        for (move in moves) {
            moveGen.data.board.makeMove(move)
            nodes += perft(moveGen, depth - 1, path + move)
            moveGen.data.board.unmakeMove(move)

            i++
        }
    } catch (err: Exception) {
        val mv = moves[i]
        val pathStr = path.map { Board.indexToSqr(it.from) + Board.indexToSqr(it.to) }
        println("ERROR ON : ${Board.indexToSqr(mv.from)}${Board.indexToSqr(mv.to)}\n$pathStr\n${err.stackTraceToString()}")
    }

    return nodes
}
