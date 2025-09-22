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

    switch (type) {
        case ROOK:
            // 4 directions: up, down, left, right
            addSlidingMoves(board, position, moves, 1, 0);
            addSlidingMoves(board, position, moves, -1, 0);
            addSlidingMoves(board, position, moves, 0, 1);
            addSlidingMoves(board, position, moves, 0, -1);
            break;

        case BISHOP:
            // 4 diagonals
            addSlidingMoves(board, position, moves, 1, 1);
            addSlidingMoves(board, position, moves, 1, -1);
            addSlidingMoves(board, position, moves, -1, 1);
            addSlidingMoves(board, position, moves, -1, -1);
            break;

        case QUEEN:
            // Rook + Bishop directions
            int[][] queenDirs = {
                    {1, 0}, {-1, 0}, {0, 1}, {0, -1}, // rook
                    {1, 1}, {1, -1}, {-1, 1}, {-1, -1} // bishop
            };
            for (int[] d : queenDirs) {
                addSlidingMoves(board, position, moves, d[0], d[1]);
            }
            break;

        case KING:
            // One square in any direction
            int[][] kingDirs = {
                    {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                    {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
            };
            for (int[] d : kingDirs) {
                int newRow = position.getRow() + d[0];
                int newCol = position.getColumn() + d[1];

                if (newRow >= 1 && newRow <= 8 && newCol >= 1 && newCol <= 8) {
                    ChessPosition newPos = new ChessPosition(newRow, newCol);
                    ChessPiece pieceAt = board.getPiece(newPos);

                    if (pieceAt == null || pieceAt.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(position, newPos, null));
                    }
                }
            }
            break;

        default:
            // Knight & Pawn not yet implemented
            break;
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
