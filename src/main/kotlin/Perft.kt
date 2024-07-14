fun perft(
    moveGen: MoveGen,
    depth: Int,
    path: MutableList<Move>,
): Int {
    if (depth == 0) return 1

    var nodes = 0
    val moves = moveGen.generateMoves()
    var i = 0
    try {
        for (move in moves) {
            path.add(move)
            moveGen.data.board.makeMove(move)
            nodes += perft(moveGen, depth - 1, path)
            moveGen.data.board.unmakeMove(move)

            i++
            path.removeLast()
        }
    } catch (err: Exception) {
        val mv = moves[i]
        println("ERROR ON : ${mv.from} -> ${mv.to} , ${mv.type}\n$path")
        return -1
    }

    return nodes
}
