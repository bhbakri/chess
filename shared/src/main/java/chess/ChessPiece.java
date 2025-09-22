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
                addSlidingMoves(board, position, moves, 1, 0);
                addSlidingMoves(board, position, moves, -1, 0);
                addSlidingMoves(board, position, moves, 0, 1);
                addSlidingMoves(board, position, moves, 0, -1);
                break;

            case BISHOP:
                addSlidingMoves(board, position, moves, 1, 1);
                addSlidingMoves(board, position, moves, 1, -1);
                addSlidingMoves(board, position, moves, -1, 1);
                addSlidingMoves(board, position, moves, -1, -1);
                break;

            case QUEEN:
                int[][] queenDirs = {
                        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                        {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
                };
                for (int[] d : queenDirs) {
                    addSlidingMoves(board, position, moves, d[0], d[1]);
                }
                break;

            case KING:
                int[][] kingDirs = {
                        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                        {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
                };
                for (int[] d : kingDirs) {
                    int newRow = position.getRow() + d[0];
                    int newCol = position.getColumn() + d[1];
                    if (isInBounds(newRow, newCol)) {
                        addIfValid(board, position, moves, newRow, newCol);
                    }
                }
                break;

            case KNIGHT:
                int[][] knightMoves = {
                        {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
                        {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
                };
                for (int[] d : knightMoves) {
                    int newRow = position.getRow() + d[0];
                    int newCol = position.getColumn() + d[1];
                    if (isInBounds(newRow, newCol)) {
                        addIfValid(board, position, moves, newRow, newCol);
                    }
                }
                break;

            case PAWN:
                addPawnMoves(board, position, moves);
                break;
        }

        return moves;
    }

    /** Pawn movement rules */
    private void addPawnMoves(ChessBoard board, ChessPosition position, List<ChessMove> moves) {
        int dir = (pieceColor == ChessGame.TeamColor.WHITE) ? 1 : -1;
        int startRow = (pieceColor == ChessGame.TeamColor.WHITE) ? 2 : 7;
        int promotionRow = (pieceColor == ChessGame.TeamColor.WHITE) ? 8 : 1;

        int row = position.getRow();
        int col = position.getColumn();

        // Forward move
        int forwardRow = row + dir;
        if (isInBounds(forwardRow, col) && board.getPiece(new ChessPosition(forwardRow, col)) == null) {
            addPawnMoveWithPromotion(position, moves, forwardRow, col, promotionRow);

            // Double move from start
            if (row == startRow) {
                int twoForward = row + 2 * dir;
                if (board.getPiece(new ChessPosition(twoForward, col)) == null) {
                    moves.add(new ChessMove(position, new ChessPosition(twoForward, col), null));
                }
            }
        }

        // Captures (diagonal left & right)
        int[][] captureDirs = {{dir, -1}, {dir, 1}};
        for (int[] d : captureDirs) {
            int newRow = row + d[0];
            int newCol = col + d[1];
            if (isInBounds(newRow, newCol)) {
                ChessPosition newPos = new ChessPosition(newRow, newCol);
                ChessPiece target = board.getPiece(newPos);
                if (target != null && target.getTeamColor() != this.getTeamColor()) {
                    addPawnMoveWithPromotion(position, moves, newRow, newCol, promotionRow);
                }
            }
        }
    }

    /** Adds pawn promotion options if reaching last rank */
    private void addPawnMoveWithPromotion(ChessPosition from, List<ChessMove> moves, int row, int col, int promotionRow) {
        ChessPosition newPos = new ChessPosition(row, col);
        if (row == promotionRow) {
            moves.add(new ChessMove(from, newPos, PieceType.QUEEN));
            moves.add(new ChessMove(from, newPos, PieceType.ROOK));
            moves.add(new ChessMove(from, newPos, PieceType.BISHOP));
            moves.add(new ChessMove(from, newPos, PieceType.KNIGHT));
        } else {
            moves.add(new ChessMove(from, newPos, null));
        }
    }

    /** Helper for sliding pieces (rook, bishop, queen) */
    private void addSlidingMoves(ChessBoard board, ChessPosition position,
                                 List<ChessMove> moves, int rowDir, int colDir) {
        int row = position.getRow();
        int col = position.getColumn();

        while (true) {
            row += rowDir;
            col += colDir;

            if (!isInBounds(row, col)) break;

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

    /** Helper for one-square moves (king, knight) */
    private void addIfValid(ChessBoard board, ChessPosition from, List<ChessMove> moves, int row, int col) {
        ChessPosition newPos = new ChessPosition(row, col);
        ChessPiece pieceAt = board.getPiece(newPos);
        if (pieceAt == null || pieceAt.getTeamColor() != this.getTeamColor()) {
            moves.add(new ChessMove(from, newPos, null));
        }
    }

    private boolean isInBounds(int row, int col) {
        return row >= 1 && row <= 8 && col >= 1 && col <= 8;
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
