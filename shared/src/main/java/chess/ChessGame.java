package chess;

import java.util.*;

public class ChessGame {

    private ChessBoard board;
    private TeamColor turn;

    public ChessGame() {
        board = new ChessBoard();
        board.resetBoard();
        turn = TeamColor.WHITE;
    }

    public TeamColor getTeamTurn() {
        if (turn == null) turn = TeamColor.WHITE;
        return turn;
    }

    // Decides team turn
    public void setTeamTurn(TeamColor team) {
        if (team == null) {
            this.turn = TeamColor.WHITE;
            return;
        }
        this.turn = team;
    }

    public enum TeamColor { WHITE, BLACK }

    // Valid Moves Checklist
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        if (startPosition == null) return null;

        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) return null;

        Collection<ChessMove> raw = piece.pieceMoves(board, startPosition);
        return (raw == null) ? Collections.emptyList() : raw;
    }

    // Ensures its their turn and that the move is legal
    public void makeMove(ChessMove move) throws InvalidMoveException {
        if (move == null) throw new InvalidMoveException("Move is null");

        ChessPosition start = move.getStartPosition();
        ChessPiece moverPiece = board.getPiece(start);

        if (moverPiece == null) throw new InvalidMoveException("No piece at start");
        if (moverPiece.getTeamColor() != getTeamTurn()) {
            throw new InvalidMoveException("It's not " + moverPiece.getTeamColor() + "'s turn");
        }

        Collection<ChessMove> rawFromHere = moverPiece.pieceMoves(board, start);
        if (rawFromHere == null || !rawFromHere.contains(move)) {
            throw new InvalidMoveException("Illegal move");
        }

        applyMove(board, move);

        if (turn == TeamColor.WHITE) {
            turn = TeamColor.BLACK;
        } else {
            turn = TeamColor.WHITE;
        }
    }

    // True If In Check
    public boolean isInCheck(TeamColor teamColor) {
        if (teamColor == null) return false;
        return isInCheckOnBoard(board, teamColor);
    }

    public boolean isInCheckmate(TeamColor teamColor) {
        return false;
    }

    // Stalemate
    public boolean isInStalemate(TeamColor teamColor) {
        return false;
    }

    public void setBoard(ChessBoard board) {
        this.board = (board != null) ? board : new ChessBoard();
    }

    public ChessBoard getBoard() {
        return board;
    }

    private boolean isInCheckOnBoard(ChessBoard b, TeamColor team) {
        if (b == null) return false;

        ChessPosition king = findKing(b, team);
        if (king == null) {
            return false;
        }

        TeamColor enemy = (team == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;

        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition p = new ChessPosition(r, c);
                ChessPiece piece = b.getPiece(p);
                if (piece == null || piece.getTeamColor() != enemy) continue;

                Collection<ChessMove> raw = piece.pieceMoves(b, p);
                if (raw == null || raw.isEmpty()) continue;

                for (ChessMove mv : raw) {
                    if (mv != null && king.equals(mv.getEndPosition())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private ChessPosition findKing(ChessBoard b, TeamColor team) {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece p = b.getPiece(pos);
                if (p == null) continue;
                if (p.getTeamColor() != team) continue;
                if (p.getPieceType() == ChessPiece.PieceType.KING) {
                    return pos;
                }
            }
        }
        return null;
    }

    private ChessBoard copyBoard(ChessBoard src) {
        ChessBoard dst = new ChessBoard();

        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece p = src.getPiece(pos);
                if (p == null) {
                    dst.addPiece(pos, null);
                } else {
                    dst.addPiece(pos, new ChessPiece(p.getTeamColor(), p.getPieceType()));
                }
            }
        }
        return dst;
    }

    //handles all the different types of moves
    private void applyMove(ChessBoard b, ChessMove mv) {
        if (b == null || mv == null) return;

        ChessPosition from = mv.getStartPosition();
        ChessPosition to   = mv.getEndPosition();

        ChessPiece moving = b.getPiece(from);

        b.addPiece(from, null);

        ChessPiece.PieceType promo = mv.getPromotionPiece();
        if (promo != null && moving != null && moving.getPieceType() == ChessPiece.PieceType.PAWN) {
            ChessPiece promoted = new ChessPiece(moving.getTeamColor(), promo);
            b.addPiece(to, promoted);
        } else {
            b.addPiece(to, moving);
        }
    }
}
