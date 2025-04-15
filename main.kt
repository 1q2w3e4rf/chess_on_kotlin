import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.LineBorder
import kotlin.math.sign

fun main() {
    ChessGame().start()
}

class ChessGame {
    private val board = ChessBoard()
    private var selectedPosition: ChessPosition? = null
    private val frame = JFrame("Шахматы")
    private val boardPanel = JPanel()
    private val messageLabel = JLabel("Ход белых", SwingConstants.CENTER)
    private val buttons = Array(8) { Array(8) { JButton() } }
    private var possibleMoves: List<ChessMove> = emptyList()
    private var promotionDialog: JDialog? = null

    fun start() {
        setupUI()
        updateBoard()
    }

    private fun setupUI() {
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.layout = BorderLayout()
        frame.preferredSize = Dimension(600, 600)

        boardPanel.layout = GridLayout(8, 8)
        boardPanel.preferredSize = Dimension(500, 500)

        for (y in 0..7) {
            for (x in 0..7) {
                val button = JButton()
                button.background = if ((x + y) % 2 == 0) Color.WHITE else Color.GRAY
                button.border = LineBorder(Color.BLACK)
                button.isOpaque = true
                button.addMouseListener(ChessMouseListener(x, y))
                buttons[y][x] = button
                boardPanel.add(button)
            }
        }

        messageLabel.font = Font("Arial", Font.BOLD, 16)
        messageLabel.preferredSize = Dimension(600, 50)

        frame.add(boardPanel, BorderLayout.CENTER)
        frame.add(messageLabel, BorderLayout.SOUTH)
        frame.pack()
        frame.isVisible = true
    }

    private fun updateBoard() {
        for (y in 0..7) {
            for (x in 0..7) {
                val button = buttons[y][x]
                val piece = board.getPieceAt(ChessPosition(x, y))
                button.text = getPieceSymbol(piece)
                button.font = Font("Arial Unicode MS", Font.PLAIN, 40)
                button.background = if ((x + y) % 2 == 0) Color.WHITE else Color.GRAY
            }
        }

        possibleMoves.forEach { move ->
            buttons[move.to.y][move.to.x].background = Color.GREEN
        }

        when {
            board.isCheckmate(PieceColor.WHITE) -> {
                messageLabel.text = "Чёрные победили! Мат!"
                endGame()
            }
            board.isCheckmate(PieceColor.BLACK) -> {
                messageLabel.text = "Белые победили! Мат!"
                endGame()
            }
            board.isCheck(board.currentPlayer) -> {
                messageLabel.text = "Шах ${if (board.currentPlayer == PieceColor.WHITE) "белым" else "чёрным"}!"
            }
            else -> {
                messageLabel.text = when (board.currentPlayer) {
                    PieceColor.WHITE -> "Ход белых"
                    PieceColor.BLACK -> "Ход черных"
                }
            }
        }
    }

    private fun getPieceSymbol(piece: ChessPiece?): String {
        return when (piece) {
            is ChessPiece.Pawn -> if (piece.color == PieceColor.WHITE) "♙" else "♟"
            is ChessPiece.Rook -> if (piece.color == PieceColor.WHITE) "♖" else "♜"
            is ChessPiece.Knight -> if (piece.color == PieceColor.WHITE) "♘" else "♞"
            is ChessPiece.Bishop -> if (piece.color == PieceColor.WHITE) "♗" else "♝"
            is ChessPiece.Queen -> if (piece.color == PieceColor.WHITE) "♕" else "♛"
            is ChessPiece.King -> if (piece.color == PieceColor.WHITE) "♔" else "♚"
            null -> ""
        }
    }

    private fun endGame() {
        JOptionPane.showMessageDialog(frame, messageLabel.text + "\nНачать новую игру?")
        board.resetBoard()
        updateBoard()
    }

    inner class ChessMouseListener(private val x: Int, private val y: Int) : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            val position = ChessPosition(x, y)

            if (selectedPosition == null) {
                val piece = board.getPieceAt(position)
                if (piece != null && piece.color == board.currentPlayer) {
                    selectedPosition = position
                    buttons[y][x].background = Color.YELLOW
                    possibleMoves = board.getPossibleMoves(position)
                    updateBoard()
                }
            } else {
                val move = ChessMove(selectedPosition!!, position)
                if (possibleMoves.contains(move)) {
                    if (board.makeMove(move)) {
                        buttons[selectedPosition!!.y][selectedPosition!!.x].background =
                            if ((selectedPosition!!.x + selectedPosition!!.y) % 2 == 0) Color.WHITE else Color.GRAY
                        selectedPosition = null
                        possibleMoves = emptyList()
                        val piece = board.getPieceAt(position)
                        if (piece is ChessPiece.Pawn && (position.y == 0 || position.y == 7)) {
                            showPromotionDialog(position)
                        } else {
                            updateBoard()
                        }
                    } else {
                        resetSelection()
                    }
                } else {
                    resetSelection()
                }
            }
        }

        private fun resetSelection() {
            if (selectedPosition != null) {
                buttons[selectedPosition!!.y][selectedPosition!!.x].background =
                    if ((selectedPosition!!.x + selectedPosition!!.y) % 2 == 0) Color.WHITE else Color.GRAY
            }
            selectedPosition = null
            possibleMoves = emptyList()
            updateBoard()
        }
    }

    private fun showPromotionDialog(position: ChessPosition) {
        promotionDialog = JDialog(frame, "Выберите фигуру для повышения", true)
        promotionDialog!!.layout = FlowLayout()

        val queenButton = JButton(getPieceSymbol(ChessPiece.Queen(board.currentPlayer)))
        val rookButton = JButton(getPieceSymbol(ChessPiece.Rook(board.currentPlayer)))
        val bishopButton = JButton(getPieceSymbol(ChessPiece.Bishop(board.currentPlayer)))
        val knightButton = JButton(getPieceSymbol(ChessPiece.Knight(board.currentPlayer)))

        queenButton.font = Font("Arial Unicode MS", Font.PLAIN, 40)
        rookButton.font = Font("Arial Unicode MS", Font.PLAIN, 40)
        bishopButton.font = Font("Arial Unicode MS", Font.PLAIN, 40)
        knightButton.font = Font("Arial Unicode MS", Font.PLAIN, 40)

        queenButton.addActionListener { promotePawn(position, ChessPiece.Queen(board.currentPlayer)) }
        rookButton.addActionListener { promotePawn(position, ChessPiece.Rook(board.currentPlayer)) }
        bishopButton.addActionListener { promotePawn(position, ChessPiece.Bishop(board.currentPlayer)) }
        knightButton.addActionListener { promotePawn(position, ChessPiece.Knight(board.currentPlayer)) }

        promotionDialog!!.add(queenButton)
        promotionDialog!!.add(rookButton)
        promotionDialog!!.add(bishopButton)
        promotionDialog!!.add(knightButton)

        promotionDialog!!.pack()
        promotionDialog!!.setLocationRelativeTo(frame)
        promotionDialog!!.isVisible = true
    }

    private fun promotePawn(position: ChessPosition, newPiece: ChessPiece) {
        board.promotePawn(position, newPiece)
        promotionDialog!!.dispose()
        updateBoard()
    }
}

sealed class ChessPiece(open val color: PieceColor, val type: PieceType) {
    data class Pawn(override val color: PieceColor) : ChessPiece(color, PieceType.PAWN)
    data class Rook(override val color: PieceColor) : ChessPiece(color, PieceType.ROOK)
    data class Knight(override val color: PieceColor) : ChessPiece(color, PieceType.KNIGHT)
    data class Bishop(override val color: PieceColor) : ChessPiece(color, PieceType.BISHOP)
    data class Queen(override val color: PieceColor) : ChessPiece(color, PieceType.QUEEN)
    data class King(override val color: PieceColor) : ChessPiece(color, PieceType.KING)
}

enum class PieceColor { WHITE, BLACK }
enum class PieceType { PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING }

data class ChessPosition(val x: Int, val y: Int) {
    init {
        require(x in 0..7 && y in 0..7) { "Position out of bounds" }
    }
}

data class ChessMove(val from: ChessPosition, val to: ChessPosition)

class ChessBoard {
    private val board = Array(8) { arrayOfNulls<ChessPiece>(8) }
    var currentPlayer: PieceColor = PieceColor.WHITE
        private set
    private var moveHistory: MutableList<ChessMove> = mutableListOf()

    init {
        setupInitialPosition()
    }

    fun resetBoard() {
        for (i in 0..7) {
            for (j in 0..7) {
                board[i][j] = null
            }
        }
        setupInitialPosition()
        currentPlayer = PieceColor.WHITE
        moveHistory.clear()
    }

    private fun setupInitialPosition() {
        for (x in 0..7) {
            board[1][x] = ChessPiece.Pawn(PieceColor.BLACK)
            board[6][x] = ChessPiece.Pawn(PieceColor.WHITE)
        }

        board[0][0] = ChessPiece.Rook(PieceColor.BLACK)
        board[0][7] = ChessPiece.Rook(PieceColor.BLACK)
        board[7][0] = ChessPiece.Rook(PieceColor.WHITE)
        board[7][7] = ChessPiece.Rook(PieceColor.WHITE)

        board[0][1] = ChessPiece.Knight(PieceColor.BLACK)
        board[0][6] = ChessPiece.Knight(PieceColor.BLACK)
        board[7][1] = ChessPiece.Knight(PieceColor.WHITE)
        board[7][6] = ChessPiece.Knight(PieceColor.WHITE)

        board[0][2] = ChessPiece.Bishop(PieceColor.BLACK)
        board[0][5] = ChessPiece.Bishop(PieceColor.BLACK)
        board[7][2] = ChessPiece.Bishop(PieceColor.WHITE)
        board[7][5] = ChessPiece.Bishop(PieceColor.WHITE)

        board[0][3] = ChessPiece.Queen(PieceColor.BLACK)
        board[7][3] = ChessPiece.Queen(PieceColor.WHITE)

        board[0][4] = ChessPiece.King(PieceColor.BLACK)
        board[7][4] = ChessPiece.King(PieceColor.WHITE)
    }

    fun getPieceAt(position: ChessPosition): ChessPiece? = board[position.y][position.x]

    fun getPossibleMoves(position: ChessPosition): List<ChessMove> {
        val piece = getPieceAt(position) ?: return emptyList()
        val moves = mutableListOf<ChessMove>()
        for (y in 0..7) {
            for (x in 0..7) {
                val to = ChessPosition(x, y)
                val move = ChessMove(position, to)
                if (isValidMove(move)) {
                    moves.add(move)
                }
            }
        }
        return moves
    }

    fun isValidMove(move: ChessMove): Boolean {
        val piece = getPieceAt(move.from) ?: return false
        if (piece.color != currentPlayer) return false

        val tempBoard = copyBoard()
        val tempPiece = tempBoard[move.from.y][move.from.x]!!
        tempBoard[move.from.y][move.from.x] = null
        tempBoard[move.to.y][move.to.x] = tempPiece

        // Check if the move puts the king in check
        if (isCheckAfterMove(piece.color, tempBoard)) {
            return false // Invalid move: King in check
        }

        return when (piece.type) {
            PieceType.PAWN -> isValidPawnMove(move, piece.color)
            PieceType.ROOK -> isValidRookMove(move)
            PieceType.KNIGHT -> isValidKnightMove(move)
            PieceType.BISHOP -> isValidBishopMove(move)
            PieceType.QUEEN -> isValidQueenMove(move)
            PieceType.KING -> isValidKingMove(move)
        }
    }

    fun makeMove(move: ChessMove): Boolean {
        if (!isValidMove(move)) return false

        val piece = getPieceAt(move.from)!!
        board[move.from.y][move.from.x] = null
        board[move.to.y][move.to.x] = piece

        moveHistory.add(move)

        currentPlayer = if (currentPlayer == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        return true
    }

    fun promotePawn(position: ChessPosition, newPiece: ChessPiece) {
        board[position.y][position.x] = newPiece
    }

    private fun isValidPawnMove(move: ChessMove, color: PieceColor): Boolean {
        val direction = if (color == PieceColor.WHITE) -1 else 1
        val startRow = if (color == PieceColor.WHITE) 6 else 1

        if (move.from.x == move.to.x) {
            if (move.to.y == move.from.y + direction && getPieceAt(move.to) == null) {
                return true
            }
            if (move.from.y == startRow && move.to.y == move.from.y + 2 * direction &&
                getPieceAt(move.to) == null && getPieceAt(ChessPosition(move.from.x, move.from.y + direction)) == null) {
                return true
            }
        }

        if (Math.abs(move.to.x - move.from.x) == 1 && move.to.y == move.from.y + direction) {
            val targetPiece = getPieceAt(move.to)
            return targetPiece != null && targetPiece.color != color
        }

        return false
    }

    private fun isValidRookMove(move: ChessMove): Boolean {
        if (move.from.x != move.to.x && move.from.y != move.to.y) return false
        return isPathClear(move.from, move.to)
    }

    private fun isValidKnightMove(move: ChessMove): Boolean {
        val dx = Math.abs(move.to.x - move.from.x)
        val dy = Math.abs(move.to.y - move.from.y)
        return (dx == 1 && dy == 2 || dx == 2 && dy == 1) &&
                (getPieceAt(move.to)?.color != currentPlayer)
    }

    private fun isValidBishopMove(move: ChessMove): Boolean {
        if (Math.abs(move.to.x - move.from.x) != Math.abs(move.to.y - move.from.y)) return false
        return isPathClear(move.from, move.to)
    }

    private fun isValidQueenMove(move: ChessMove): Boolean {
        return isValidRookMove(move) || isValidBishopMove(move)
    }

    private fun isValidKingMove(move: ChessMove): Boolean {
        val dx = Math.abs(move.to.x - move.from.x)
        val dy = Math.abs(move.to.y - move.from.y)
        return dx <= 1 && dy <= 1 && (dx != 0 || dy != 0) &&
                (getPieceAt(move.to)?.color != currentPlayer)
    }

    private fun isPathClear(from: ChessPosition, to: ChessPosition): Boolean {
        val dx = (to.x - from.x).sign
        val dy = (to.y - from.y).sign

        var x = from.x + dx
        var y = from.y + dy

        while (x != to.x || y != to.y) {
            if (board[y][x] != null) return false
            x += dx
            y += dy
        }

        val targetPiece = board[to.y][to.x]
        return targetPiece == null || targetPiece.color != getPieceAt(from)!!.color
    }

    fun isCheck(color: PieceColor): Boolean {
        val kingPosition = findKing(color) ?: return false

        for (y in 0..7) {
            for (x in 0..7) {
                val piece = getPieceAt(ChessPosition(x, y))
                if (piece != null && piece.color != color) {
                    val move = ChessMove(ChessPosition(x, y), kingPosition)
                    if (isValidMoveOnOriginalBoard(move)) {
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun findKing(color: PieceColor): ChessPosition? {
        for (y in 0..7) {
            for (x in 0..7) {
                val piece = getPieceAt(ChessPosition(x, y))
                if (piece is ChessPiece.King && piece.color == color) {
                    return ChessPosition(x, y)
                }
            }
        }
        return null
    }

    fun isCheckmate(color: PieceColor): Boolean {
        if (!isCheck(color)) return false

        for (y in 0..7) {
            for (x in 0..7) {
                val position = ChessPosition(x, y)
                val piece = getPieceAt(position)
                if (piece != null && piece.color == color) {
                    val possibleMoves = getPossibleMoves(position)
                    for (move in possibleMoves) {
                        val tempBoard = copyBoard()
                        val tempPiece = tempBoard[move.from.y][move.from.x]!!
                        tempBoard[move.from.y][move.from.x] = null
                        tempBoard[move.to.y][move.to.x] = tempPiece

                        if (!isCheckAfterMove(color, tempBoard)) {
                            return false
                        }
                    }
                }
            }
        }

        return true
    }

    private fun isCheckAfterMove(color: PieceColor, boardState: Array<Array<ChessPiece?>>): Boolean {
        val kingPosition = findKingInBoardState(color, boardState) ?: return false

        for (y in 0..7) {
            for (x in 0..7) {
                val piece = boardState[y][x]
                if (piece != null && piece.color != color) {
                    val move = ChessMove(ChessPosition(x, y), kingPosition)

                    if (isValidMoveOnBoardState(move, boardState)) {
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun findKingInBoardState(color: PieceColor, boardState: Array<Array<ChessPiece?>>): ChessPosition? {
        for (y in 0..7) {
            for (x in 0..7) {
                val piece = boardState[y][x]
                if (piece is ChessPiece.King && piece.color == color) {
                    return ChessPosition(x, y)
                }
            }
        }
        return null
    }

    private fun isValidMoveOnOriginalBoard(move: ChessMove): Boolean {
        val piece = getPieceAt(move.from) ?: return false

        return when (piece.type) {
            PieceType.PAWN -> isValidPawnMove(move, piece.color)
            PieceType.ROOK -> isValidRookMove(move)
            PieceType.KNIGHT -> isValidKnightMove(move)
            PieceType.BISHOP -> isValidBishopMove(move)
            PieceType.QUEEN -> isValidQueenMove(move)
            PieceType.KING -> isValidKingMove(move)
        }
    }

    private fun isValidMoveOnBoardState(move: ChessMove, boardState: Array<Array<ChessPiece?>>): Boolean {
        val piece = boardState[move.from.y][move.from.x] ?: return false
        return when (piece.type) {
            PieceType.PAWN -> isValidPawnMoveOnBoardState(move, piece.color, boardState)
            PieceType.ROOK -> isValidRookMoveOnBoardState(move, boardState)
            PieceType.KNIGHT -> isValidKnightMoveOnBoardState(move, boardState)
            PieceType.BISHOP -> isValidBishopMoveOnBoardState(move, boardState)
            PieceType.QUEEN -> isValidQueenMoveOnBoardState(move, boardState)
            PieceType.KING -> isValidKingMoveOnBoardState(move, boardState)
        }
    }

    private fun isValidPawnMoveOnBoardState(move: ChessMove, color: PieceColor, boardState: Array<Array<ChessPiece?>>): Boolean {
        val direction = if (color == PieceColor.WHITE) -1 else 1
        val startRow = if (color == PieceColor.WHITE) 6 else 1

        if (move.from.x == move.to.x) {
            if (move.to.y == move.from.y + direction && boardState[move.to.y][move.to.x] == null) {
                return true
            }
            if (move.from.y == startRow && move.to.y == move.from.y + 2 * direction &&
                boardState[move.to.y][move.to.x] == null && boardState[move.from.y + direction][move.from.x] == null) {
                return true
            }
        }

        if (Math.abs(move.to.x - move.from.x) == 1 && move.to.y == move.from.y + direction) {
            val targetPiece = boardState[move.to.y][move.to.x]
            return targetPiece != null && targetPiece.color != color
        }

        return false
    }

    private fun isValidRookMoveOnBoardState(move: ChessMove, boardState: Array<Array<ChessPiece?>>): Boolean {
        if (move.from.x != move.to.x && move.from.y != move.to.y) return false
        return isPathClearOnBoardState(move.from, move.to, boardState)
    }

    private fun isValidKnightMoveOnBoardState(move: ChessMove, boardState: Array<Array<ChessPiece?>>): Boolean {
        val dx = Math.abs(move.to.x - move.from.x)
        val dy = Math.abs(move.to.y - move.from.y)
        val piece = boardState[move.from.y][move.from.x]
        return (dx == 1 && dy == 2 || dx == 2 && dy == 1) &&
                (boardState[move.to.y][move.to.x]?.color != piece?.color)
    }

    private fun isValidBishopMoveOnBoardState(move: ChessMove, boardState: Array<Array<ChessPiece?>>): Boolean {
        if (Math.abs(move.to.x - move.from.x) != Math.abs(move.to.y - move.from.y)) return false
        return isPathClearOnBoardState(move.from, move.to, boardState)
    }

    private fun isValidQueenMoveOnBoardState(move: ChessMove, boardState: Array<Array<ChessPiece?>>): Boolean {
        return isValidRookMoveOnBoardState(move, boardState) || isValidBishopMoveOnBoardState(move, boardState)
    }

    private fun isValidKingMoveOnBoardState(move: ChessMove, boardState: Array<Array<ChessPiece?>>): Boolean {
        val dx = Math.abs(move.to.x - move.from.x)
        val dy = Math.abs(move.to.y - move.from.y)
        val piece = boardState[move.from.y][move.from.x]
        return dx <= 1 && dy <= 1 && (dx != 0 || dy != 0) &&
                (boardState[move.to.y][move.to.x]?.color != piece?.color)
    }

    private fun isPathClearOnBoardState(from: ChessPosition, to: ChessPosition, boardState: Array<Array<ChessPiece?>>): Boolean {
        val dx = (to.x - from.x).sign
        val dy = (to.y - from.y).sign

        var x = from.x + dx
        var y = from.y + dy

        while (x != to.x || y != to.y) {
            if (boardState[y][x] != null) return false
            x += dx
            y += dy
        }

        val piece = boardState[from.y][from.x]
        val targetPiece = boardState[to.y][to.x]
        return targetPiece == null || targetPiece.color != piece?.color
    }

    private fun copyBoard(): Array<Array<ChessPiece?>> {
        val newBoard = Array(8) { arrayOfNulls<ChessPiece>(8) }
        for (y in 0..7) {
            for (x in 0..7) {
                newBoard[y][x] = board[y][x]
            }
        }
        return newBoard
    }
}
