package chess;

import java.util.Objects;

/**
 * Represents a position on the chess board (row and column).
 * Rows and columns are 1-indexed (1 through 8).
 */
public class ChessPosition {
    private final int row;
    private final int column;

    public ChessPosition(int row, int column) {
        // Could add validation here (1–8), but tests may rely on out-of-range being allowed
        this.row = row;
        this.column = column;
    }

    /**
     * @return the row of the position (1–8)
     */
    public int getRow() {
        return row;
    }

    /**
     * @return the column of the position (1–8)
     */
    public int getColumn() {
        return column;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChessPosition)) {
            return false;
        }
        ChessPosition that = (ChessPosition) o;
        return row == that.row && column == that.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, column);
    }

    @Override
    public String toString() {
        return "ChessPosition{" +
                "row=" + row +
                ", column=" + column +
                '}';
    }
}
