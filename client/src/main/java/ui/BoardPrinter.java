package ui;

import static ui.EscapeSequences.*;

public class BoardPrinter {

    // Square colors
    private static final String LIGHT = SET_BG_COLOR_LIGHT_GREY;
    private static final String DARK  = SET_BG_COLOR_DARK_GREEN;

    private static final String[] FILES = {"a","b","c","d","e","f","g","h"};

    // Single-character piece symbols so width is consistent
    private static String pieceSymbol(int file, int rank) {
        // Pawns
        if (rank == 2 || rank == 7) {
            return "P";
        }

        // Back ranks (white rank 1, black rank 8)
        if (rank == 1 || rank == 8) {
            return backRankPiece(file);
        }

        // Empty squares
        return " ";
    }

    private static String backRankPiece(int file) {
        return switch (file) {
            case 1, 8 -> "R";
            case 2, 7 -> "N";
            case 3, 6 -> "B";
            case 4     -> "Q";
            case 5     -> "K";
            default    -> " ";
        };
    }

    // Color white pieces red, black pieces blue
    private static String pieceColor(int file, int rank) {
        if (rank == 1 || rank == 2) {
            return SET_TEXT_COLOR_RED;   // white
        }
        if (rank == 7 || rank == 8) {
            return SET_TEXT_COLOR_BLUE;  // black
        }
        return null;
    }

    private static void printFileLabels(boolean blackPerspective) {
        System.out.print("   ");
        if (!blackPerspective) {
            for (String f : FILES) {
                System.out.print(" " + f + " ");
            }
        } else {
            for (int i = FILES.length - 1; i >= 0; i--) {
                System.out.print(" " + FILES[i] + " ");
            }
        }
        System.out.println();
    }

    /** Draws the initial board. blackPerspective = true if viewing as black. */
    public static void drawInitial(boolean blackPerspective) {
        System.out.print(ERASE_SCREEN);
        System.out.println();

        // Top file labels
        printFileLabels(blackPerspective);

        // Each board row
        for (int row = 0; row < 8; row++) {
            int rank = blackPerspective ? (row + 1) : (8 - row); // white: 8→1, black: 1→8

            // Left rank label
            System.out.printf("%2d ", rank);

            for (int col = 0; col < 8; col++) {
                int file = blackPerspective ? (8 - col) : (col + 1); // white: a→h, black: h→a

                // Light/dark pattern so that h1 and a8 are light
                boolean light = ((file + rank) % 2 == 1);
                String bg = light ? LIGHT : DARK;

                String symbol = pieceSymbol(file, rank);
                String fg = pieceColor(file, rank);

                System.out.print(bg);      // background for this square
                System.out.print(" ");     // left padding

                if (!symbol.isBlank() && fg != null) {
                    System.out.print(fg + symbol + RESET_TEXT_COLOR);
                } else {
                    System.out.print(" ");
                }

                System.out.print(" ");     // right padding
                System.out.print(RESET_BG_COLOR);
            }

            // Right rank label
            System.out.printf(" %2d%n", rank);
        }

        // Bottom file labels
        printFileLabels(blackPerspective);

        System.out.println(RESET_BG_COLOR + RESET_TEXT_COLOR);
    }
}
