package chess;

import java.util.*;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    private final ChessGame.TeamColor pieceColor;
    private final PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Calculates all the positions a chess piece can move to.
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition position) {
        List<ChessMove> moves = new ArrayList<>();

        if (type == PieceType.ROOK) {
            // 4 directions: up, down, left, right
            addSlidingMoves(board, position, moves, 1, 0);   // up
            addSlidingMoves(board, position, moves, -1, 0);  // down
            addSlidingMoves(board, position, moves, 0, 1);   // right
            addSlidingMoves(board, position, moves, 0, -1);  // left
        }

        return moves;
    }

    /**
     * Helper method for sliding pieces (rook, bishop, queen).
     * Moves in a direction until blocked.
     */
    private void addSlidingMoves(ChessBoard board, ChessPosition position,
                                 List<ChessMove> moves, int rowDir, int colDir) {
        int row = position.getRow();
        int col = position.getColumn();

        while (true) {
            row += rowDir;
            col += colDir;

            if (row < 1 || row > 8 || col < 1 || col > 8) {
                break; // out of bounds
            }

            ChessPosition newPos = new ChessPosition(row, col);
            ChessPiece pieceAt = board.getPiece(newPos);

            if (pieceAt == null) {
                moves.add(new ChessMove(position, newPos, null));
            } else {
                if (pieceAt.getTeamColor() != this.getTeamColor()) {
                    moves.add(new ChessMove(position, newPos, null));
                }
                break; // stop when blocked
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChessPiece)) return false;
        ChessPiece that = (ChessPiece) o;
        return pieceColor == that.pieceColor && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pieceColor, type);
    }

    @Override
    public String toString() {
        return pieceColor + " " + type;
    }
}
