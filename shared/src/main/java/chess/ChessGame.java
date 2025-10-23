package chess;

import java.util.*;

public class ChessGame {

    private ChessBoard board;
    private TeamColor turn;
    private ChessPosition enPassantTarget;
    private boolean whiteKingMoved, whiteQRookMoved, whiteKRookMoved;
    private boolean blackKingMoved, blackQRookMoved, blackKRookMoved;

    public ChessGame() {
        board = new ChessBoard();
        board.resetBoard();
        turn = TeamColor.WHITE;
        enPassantTarget = null;
        whiteKingMoved = whiteQRookMoved = whiteKRookMoved = false;
        blackKingMoved = blackQRookMoved = blackKRookMoved = false;
    }

    public TeamColor getTeamTurn() {
        if (turn == null) {
            turn = TeamColor.WHITE;
        }
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

    // Valid Moves Checklist
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        if (startPosition == null) {
            return null;
        }

        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) {
            return null;
        }

        Collection<ChessMove> raw = piece.pieceMoves(board, startPosition);
        if (raw == null) {
            raw = Collections.emptyList();
        }

        List<ChessMove> candidates = new ArrayList<>(raw);
        addEnPassantIfAny(startPosition, piece, candidates);
        addCastlingIfAny(startPosition, piece, candidates);
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        TeamColor mover = piece.getTeamColor();
        List<ChessMove> legal = new ArrayList<>(candidates.size());

        // keeps only moves that don't leave the king in check
        ChessPosition savedEPT = enPassantTarget;
        boolean swkm = whiteKingMoved, swq = whiteQRookMoved, swk = whiteKRookMoved;
        boolean sbkm = blackKingMoved, sbq = blackQRookMoved, sbk = blackKRookMoved;

        for (ChessMove mv : candidates) {
            if (mv == null) {
                continue;
            }

            ChessBoard sandbox = copyBoard(board);
            applyMove(sandbox, mv);

            if (!isInCheckOnBoard(sandbox, mover)) {
                legal.add(mv);
            }

            enPassantTarget = savedEPT;
            whiteKingMoved = swkm;
            whiteQRookMoved = swq;
            whiteKRookMoved = swk;
            blackKingMoved = sbkm;
            blackQRookMoved = sbq;
            blackKRookMoved = sbk;
        }

        return legal;
    }

    // Ensures it's their turn and that the move is legal
    public void makeMove(ChessMove move) throws InvalidMoveException {
        if (move == null) {
            throw new InvalidMoveException("Move is null");
        }

        ChessPosition start = move.getStartPosition();
        ChessPiece moverPiece = board.getPiece(start);

        if (moverPiece == null) {
            throw new InvalidMoveException("No piece at start");
        }
        if (moverPiece.getTeamColor() != getTeamTurn()) {
            throw new InvalidMoveException("It's not " + moverPiece.getTeamColor() + "'s turn");
        }

        Collection<ChessMove> legalFromHere = validMoves(start);
        if (legalFromHere == null || !legalFromHere.contains(move)) {
            throw new InvalidMoveException("Illegal move");
        }

        applyMove(board, move);
        toggleTurn();
    }

    // True if checked
    public boolean isInCheck(TeamColor teamColor) {
        if (teamColor == null) {
            return false;
        }
        return isInCheckOnBoard(board, teamColor);
    }

    // true if checkmate
    public boolean isInCheckmate(TeamColor teamColor) {
        if (teamColor == null) {
            return false;
        }
        if (!isInCheck(teamColor)) {
            return false;
        }
        return !hasAnyLegalMove(teamColor);
    }

    // stalemates the game
    public boolean isInStalemate(TeamColor teamColor) {
        if (teamColor == null) {
            return false;
        }
        if (isInCheck(teamColor)) {
            return false;
        }
        return !hasAnyLegalMove(teamColor);
    }

    // actual board
    public ChessBoard getBoard() {
        return board;
    }

    // replaces the board
    public void setBoard(ChessBoard board) {
        this.board = (board != null) ? board : new ChessBoard();
        enPassantTarget = null;
        whiteKingMoved = whiteQRookMoved = whiteKRookMoved = false;
        blackKingMoved = blackQRookMoved = blackKRookMoved = false;
    }

    // checks if any remaining legal movies are preset
    private boolean hasAnyLegalMove(TeamColor team) {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece p = board.getPiece(pos);
                if (p == null || p.getTeamColor() != team) {
                    continue;
                }

                Collection<ChessMove> vm = validMoves(pos);
                if (vm != null && !vm.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    //helper functions VVVVV

    // checks if a color is checked
    private boolean isInCheckOnBoard(ChessBoard b, TeamColor team) {
        if (b == null) {
            return false;
        }

        ChessPosition king = findKing(b, team);
        if (king == null) {
            return false;
        }

        TeamColor enemy = opposite(team);

        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition p = new ChessPosition(r, c);
                ChessPiece piece = b.getPiece(p);
                if (piece == null || piece.getTeamColor() != enemy) {
                    continue;
                }

                Collection<ChessMove> raw = piece.pieceMoves(b, p);
                if (raw == null || raw.isEmpty()) {
                    continue;
                }

                for (ChessMove mv : raw) {
                    if (mv != null && king.equals(mv.getEndPosition())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // finds the king for a color
    private ChessPosition findKing(ChessBoard b, TeamColor team) {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece p = b.getPiece(pos);
                if (p == null) {
                    continue;
                }
                if (p.getTeamColor() != team) {
                    continue;
                }
                if (p.getPieceType() == ChessPiece.PieceType.KING) {
                    return pos;
                }
            }
        }
        return null;
    }

    // copies the board
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

    //hanldes all the chess moves
    private void applyMove(ChessBoard b, ChessMove mv) {
        if (b == null || mv == null) {
            return;
        }

        boolean isReal = (b == this.board);

        ChessPosition from = mv.getStartPosition();
        ChessPosition to = mv.getEndPosition();

        ChessPiece moving = b.getPiece(from);
        ChessPiece captured = b.getPiece(to);

        if (moving != null && moving.getPieceType() == ChessPiece.PieceType.KING
                && from.getRow() == to.getRow()
                && Math.abs(to.getColumn() - from.getColumn()) == 2) {

            int row = from.getRow();
            b.addPiece(from, null);
            b.addPiece(to, moving);

            if (to.getColumn() == 7) {
                ChessPosition rookFrom = new ChessPosition(row, 8);
                ChessPosition rookTo = new ChessPosition(row, 6);
                ChessPiece rook = b.getPiece(rookFrom);
                b.addPiece(rookFrom, null);
                b.addPiece(rookTo, rook);
            } else {
                ChessPosition rookFrom = new ChessPosition(row, 1);
                ChessPosition rookTo = new ChessPosition(row, 4);
                ChessPiece rook = b.getPiece(rookFrom);
                b.addPiece(rookFrom, null);
                b.addPiece(rookTo, rook);
            }

            if (isReal) {
                setCastlingMovedFlag(moving.getTeamColor(), true, null);
                enPassantTarget = null;
            }
            return;
        }

        boolean enPassant = false;
        if (moving != null && moving.getPieceType() == ChessPiece.PieceType.PAWN) {
            if (captured == null && enPassantTarget != null && to.equals(enPassantTarget)
                    && from.getColumn() != to.getColumn()) {
                enPassant = true;
                int dir = (moving.getTeamColor() == TeamColor.WHITE) ? 1 : -1;
                int capRow = to.getRow() - dir;
                ChessPosition pawnSquare = new ChessPosition(capRow, to.getColumn());
                b.addPiece(pawnSquare, null);
            }
        }

        b.addPiece(from, null);
        ChessPiece.PieceType promo = mv.getPromotionPiece();
        if (promo != null && moving != null && moving.getPieceType() == ChessPiece.PieceType.PAWN) {
            b.addPiece(to, new ChessPiece(moving.getTeamColor(), promo));
        } else {
            b.addPiece(to, moving);
        }

        if (isReal) {
            updateCastlingRightsOnMove(from, to, moving, enPassant ? new ChessPiece(opposite(moving.getTeamColor()), ChessPiece.PieceType.PAWN) : captured);

            if (moving != null && moving.getPieceType() == ChessPiece.PieceType.PAWN) {
                int delta = Math.abs(to.getRow() - from.getRow());
                if (delta == 2) {
                    int midRow = (to.getRow() + from.getRow()) / 2;
                    enPassantTarget = new ChessPosition(midRow, from.getColumn());
                } else {
                    enPassantTarget = null;
                }
            } else {
                enPassantTarget = null;
            }
        }
    }

    // swap to the other side
    private void toggleTurn() {
        turn = (turn == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }

    // enemy color returner
    private TeamColor opposite(TeamColor team) {
        return (team == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }

    private void addEnPassantIfAny(ChessPosition start, ChessPiece piece, List<ChessMove> out) {
        if (enPassantTarget == null) {
            return;
        }
        if (piece.getPieceType() != ChessPiece.PieceType.PAWN) {
            return;
        }

        int sr = start.getRow(), sc = start.getColumn();
        int tr = enPassantTarget.getRow(), tc = enPassantTarget.getColumn();

        int dir = (piece.getTeamColor() == TeamColor.WHITE) ? 1 : -1;
        if (tr != sr + dir) {
            return;
        }
        if (Math.abs(tc - sc) != 1) {
            return;
        }
        if (board.getPiece(enPassantTarget) != null) {
            return;
        }

        int capturedRow = tr - dir;
        ChessPiece captured = board.getPiece(new ChessPosition(capturedRow, tc));
        if (captured == null || captured.getPieceType() != ChessPiece.PieceType.PAWN
                || captured.getTeamColor() == piece.getTeamColor()) {
            return;
        }

        out.add(new ChessMove(start, enPassantTarget, null));
    }

    private void addCastlingIfAny(ChessPosition start, ChessPiece piece, List<ChessMove> out) {
        if (piece.getPieceType() != ChessPiece.PieceType.KING) {
            return;
        }

        TeamColor team = piece.getTeamColor();
        int row = (team == TeamColor.WHITE) ? 1 : 8;

        if (start.getRow() != row || start.getColumn() != 5) {
            return;
        }
        if (isInCheck(team)) {
            return;
        }

        if (canCastleKingSide(team)) {
            if (areEmpty(row, 6, 7) &&
                    !isSquareAttacked(board, opposite(team), new ChessPosition(row, 6)) &&
                    !isSquareAttacked(board, opposite(team), new ChessPosition(row, 7))) {
                out.add(new ChessMove(start, new ChessPosition(row, 7), null));
            }
        }

        if (canCastleQueenSide(team)) {
            if (areEmpty(row, 2, 3, 4) &&
                    !isSquareAttacked(board, opposite(team), new ChessPosition(row, 4)) &&
                    !isSquareAttacked(board, opposite(team), new ChessPosition(row, 3))) {
                out.add(new ChessMove(start, new ChessPosition(row, 3), null));
            }
        }
    }

    private boolean areEmpty(int row, int... cols) {
        for (int c : cols) {
            if (board.getPiece(new ChessPosition(row, c)) != null) {
                return false;
            }
        }
        return true;
    }

    private boolean canCastleKingSide(TeamColor team) {
        int row = (team == TeamColor.WHITE) ? 1 : 8;
        ChessPiece rook = board.getPiece(new ChessPosition(row, 8));
        if (rook == null || rook.getPieceType() != ChessPiece.PieceType.ROOK || rook.getTeamColor() != team) {
            return false;
        }
        if (team == TeamColor.WHITE) {
            return !whiteKingMoved && !whiteKRookMoved;
        }
        return !blackKingMoved && !blackKRookMoved;
    }

    private boolean canCastleQueenSide(TeamColor team) {
        int row = (team == TeamColor.WHITE) ? 1 : 8;
        ChessPiece rook = board.getPiece(new ChessPosition(row, 1));
        if (rook == null || rook.getPieceType() != ChessPiece.PieceType.ROOK || rook.getTeamColor() != team) {
            return false;
        }
        if (team == TeamColor.WHITE) {
            return !whiteKingMoved && !whiteQRookMoved;
        }
        return !blackKingMoved && !blackQRookMoved;
    }

    private boolean isSquareAttacked(ChessBoard b, TeamColor by, ChessPosition target) {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPiece p = b.getPiece(new ChessPosition(r, c));
                if (p == null || p.getTeamColor() != by) {
                    continue;
                }
                Collection<ChessMove> moves = p.pieceMoves(b, new ChessPosition(r, c));
                if (moves == null) {
                    continue;
                }
                for (ChessMove mv : moves) {
                    if (mv != null && target.equals(mv.getEndPosition())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void updateCastlingRightsOnMove(ChessPosition from, ChessPosition to,
                                            ChessPiece moving, ChessPiece captured) {
        if (moving != null) {
            if (moving.getPieceType() == ChessPiece.PieceType.KING) {
                setCastlingMovedFlag(moving.getTeamColor(), true, null);
            } else if (moving.getPieceType() == ChessPiece.PieceType.ROOK) {
                if (isCorner(from)) {
                    setCastlingMovedFlag(moving.getTeamColor(), false, from);
                }
            }
        }
        if (captured != null && captured.getPieceType() == ChessPiece.PieceType.ROOK) {
            if (isCorner(to)) {
                setCastlingMovedFlag(captured.getTeamColor(), false, to);
            }
        }
    }

    private boolean isCorner(ChessPosition p) {
        int r = p.getRow(), c = p.getColumn();
        return (r == 1 && (c == 1 || c == 8)) || (r == 8 && (c == 1 || c == 8));
    }

    private void setCastlingMovedFlag(TeamColor team, boolean king, ChessPosition rookCorner) {
        if (king) {
            if (team == TeamColor.WHITE) {
                whiteKingMoved = true;
            } else {
                blackKingMoved = true;
            }
            return;
        }
        int row = (team == TeamColor.WHITE) ? 1 : 8;
        if (rookCorner.getRow() != row) {
            return;
        }
        if (rookCorner.getColumn() == 1) {
            if (team == TeamColor.WHITE) {
                whiteQRookMoved = true;
            } else {
                blackQRookMoved = true;
            }
        } else if (rookCorner.getColumn() == 8) {
            if (team == TeamColor.WHITE) {
                whiteKRookMoved = true;
            } else {
                blackKRookMoved = true;
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ChessGame other)) {
            return false;
        }
        return Objects.equals(board, other.board) && turn == other.turn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, turn);
    }

    public enum TeamColor {WHITE, BLACK}
}
