package chess;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import boardgame.Board;
import boardgame.Piece;
import boardgame.Position;
import chess.pieces.King;
import chess.pieces.Pawn;
import chess.pieces.Queen;
import chess.pieces.Bishop;
import chess.pieces.Knight;
import chess.pieces.Rook;

public class ChessMatch
{
  private int turn;
  private Color currentPlayer;
  private Board board;
  private boolean check;
  private boolean checkMate;
  private ChessPiece enPassantVulnerable;
  private ChessPiece promoted;

  private List <Piece> piecesOnBoard = new ArrayList <> ();
  private List <Piece> capturedPieces = new ArrayList <> ();

  public ChessMatch ()
  {
    board = new Board (8, 8);
    turn = 1;
    currentPlayer = Color.WHITE;
    initialSetup ();
  }

  public ChessPiece [][] getPieces ()
  {
    ChessPiece [][] mat = new ChessPiece [board.getRows ()] [board.getColumns ()];

    for (int i = 0; i < board.getRows (); i++)
      for (int j = 0; j < board.getColumns (); j++)
        mat [i] [j] = (ChessPiece) board.piece(i, j);

    return mat;
  }

  public int getTurn ()
  {
    return turn;
  }

  public Color getCurrentPlayer ()
  {
    return currentPlayer;
  }

  public boolean getCheck ()
  {
    return check;
  }

  public boolean getCheckMate ()
  {
    return checkMate;
  }

  public ChessPiece getEnPassantVulnerable ()
  {
    return enPassantVulnerable;
  }

  public ChessPiece getPromoted ()
  {
    return promoted;
  }

  public boolean [][] possibleMoves (ChessPosition sourcePosition)
  {
    Position position = sourcePosition.toPosition ();
    validateSourcePosition (position);

    return board.piece (position).possibleMoves ();
  }

  public ChessPiece performChessMove (ChessPosition sourcePosition, ChessPosition targetPosition)
  {
    Position source = sourcePosition.toPosition ();
    Position target = targetPosition.toPosition ();
    validateSourcePosition (source);
    validateTargetPosition (source, target);

    Piece capturedPiece = makeMove (source, target);
    ChessPiece movedPiece = (ChessPiece) board.piece (target);

    // promotion
    promoted = null;
    if (movedPiece instanceof Pawn)
    {
      if ((movedPiece.getColor () == Color.WHITE && target.getRow () == 0) || (movedPiece.getColor () == Color.BLACK && target.getRow () == 7))
      {
        promoted = (ChessPiece) board.piece (target);
        promoted = replacePromotedPiece ("Q");
      }
    }

    if (testCheck (currentPlayer))
    {
      undoMove(source, target, capturedPiece);
      throw new ChessException ("You can't put yourself in check.");
    }

    check = (testCheck (opponent (currentPlayer))) ? true : false;
    if (testCheckMate (opponent (currentPlayer)))
      checkMate = true;
    else
      nextTurn ();

    if (movedPiece instanceof Pawn && Math.abs (target.getRow () - source.getRow ()) == 2)
      enPassantVulnerable = movedPiece;
    else
      enPassantVulnerable = null;

    return (ChessPiece) capturedPiece;
  }

  public ChessPiece replacePromotedPiece (String type)
  {
    if (promoted == null)
      throw new IllegalStateException ("There is no pawn to be promoted.");
    if (!type.equals ("Q") && !type.equals ("R") && !type.equals ("K") && !type.equals ("B"))
      return promoted;

    Position position = promoted.getChessPosition ().toPosition ();
    Piece piece = board.removePiece (position);
    piecesOnBoard.remove (piece);

    ChessPiece newPiece = newPiece (type, promoted.getColor ());
    board.placePiece(newPiece, position);
    piecesOnBoard.add (newPiece);

    return newPiece;
  }

  private ChessPiece newPiece (String type, Color color)
  {
    if (type.equals ("Q")) return new Queen (board, color);
    if (type.equals ("R")) return new Rook (board, color);
    if (type.equals ("K")) return new Knight (board, color);
    return new Bishop (board, color);

  }

  private Piece makeMove (Position sourcePosition, Position targetPosition)
  {
    ChessPiece movedPiece = (ChessPiece) board.removePiece (sourcePosition);
    movedPiece.increaseMoveCount ();
    Piece capturedPiece = board.removePiece (targetPosition);
    board.placePiece (movedPiece, targetPosition);

    if (capturedPiece != null)
    {
      piecesOnBoard.remove (capturedPiece);
      capturedPieces.add (capturedPiece);
    }

    // right side castling
    if (movedPiece instanceof King && targetPosition.getColumn () == sourcePosition.getColumn () + 2)
    {
      Position sourceR = new Position (sourcePosition.getRow (), sourcePosition.getColumn () + 3);
      Position targetR = new Position (sourcePosition.getRow (), sourcePosition.getColumn () + 1);

      ChessPiece rook = (ChessPiece) board.removePiece (sourceR);
      board.placePiece (rook, targetR);
      rook.increaseMoveCount ();
    }

    // left side castling
    if (movedPiece instanceof King && targetPosition.getColumn () == sourcePosition.getColumn () - 2)
    {
      Position sourceR = new Position (sourcePosition.getRow (), sourcePosition.getColumn () - 4);
      Position targetR = new Position (sourcePosition.getRow (), sourcePosition.getColumn () - 1);

      ChessPiece rook = (ChessPiece) board.removePiece (sourceR);
      board.placePiece (rook, targetR);
      rook.increaseMoveCount ();
    }

    // en passant
    if (movedPiece instanceof Pawn)
    {
      if (sourcePosition.getColumn () != targetPosition.getColumn () && capturedPiece == null)
      {
        Position pawnPosition;
        if (movedPiece.getColor () == Color.WHITE)
          pawnPosition = new Position (targetPosition.getRow () + 1, targetPosition.getColumn ());
        else
          pawnPosition = new Position (targetPosition.getRow () - 1, targetPosition.getColumn ());

        capturedPiece = board.removePiece (pawnPosition);
        piecesOnBoard.remove (capturedPiece);
        capturedPieces.add (capturedPiece);
      }
    }
    
    return capturedPiece;
  }

  private void undoMove (Position source, Position target, Piece capturedPiece)
  {
    ChessPiece p = (ChessPiece) board.removePiece (target);
    p.decreaseMoveCount ();
    board.placePiece (p, source);

    if (capturedPiece != null)
    {
      board.placePiece (capturedPiece, target);
      capturedPieces.remove (capturedPiece);
      piecesOnBoard.add (capturedPiece);
    }

    // right side castling
    if (p instanceof King && target.getColumn () == source.getColumn () + 2)
    {
      Position sourceR = new Position (source.getRow (), source.getColumn () + 3);
      Position targetR = new Position (source.getRow (), source.getColumn () + 1);

      ChessPiece rook = (ChessPiece) board.removePiece (targetR);
      board.placePiece (rook, sourceR);
      rook.decreaseMoveCount ();
    }

    // left side castling
    if (p instanceof King && target.getColumn () == source.getColumn () - 2)
    {
      Position sourceR = new Position (source.getRow (), source.getColumn () - 4);
      Position targetR = new Position (source.getRow (), source.getColumn () - 1);

      ChessPiece rook = (ChessPiece) board.removePiece (targetR);
      board.placePiece (rook, sourceR);
      rook.decreaseMoveCount ();
    }

    // en passant
    if (p instanceof Pawn)
    {
      if (source.getColumn () != target.getColumn () && capturedPiece == null)
      {
        ChessPiece pawn = (ChessPiece) board.removePiece (target);
        Position pawnPosition;
        if (p.getColor () == Color.WHITE)
          pawnPosition = new Position (3, target.getColumn ());
        else
          pawnPosition = new Position (4, target.getColumn ());

        board.placePiece(pawn, pawnPosition);
      }
    }
  }

  private void validateSourcePosition (Position position)
  {
    if (!board.thereIsAPiece (position))
      throw new ChessException ("No piece at source position.");
      
    if (currentPlayer != ((ChessPiece) board.piece (position)).getColor ())
      throw new ChessException ("Select a piece from your color.");

    if (!board.piece (position).isThereAnyPossibleMove ())
      throw new ChessException ("Selected piece has no possible moves.");
  }

  private void validateTargetPosition (Position source, Position target)
  {
    if (!board.piece (source).possibleMove (target))
      throw new ChessException ("Selected piece cannot move to target position.");
  }

  private void nextTurn ()
  {
    turn++;
    currentPlayer = (currentPlayer == Color.WHITE) ? Color.BLACK : Color.WHITE;
  }

  private Color opponent (Color color)
  {
    return (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
  }

  private ChessPiece king (Color color)
  {
    List <Piece> list = piecesOnBoard.stream ().filter (x -> ((ChessPiece) x).getColor () == color).collect (Collectors.toList ());

    for (Piece p : list)
    {
      if (p instanceof King)
        return (ChessPiece) p;
    }
    throw new IllegalStateException ("There is no " + color + "king on the board.");
  }

  private boolean testCheck (Color color)
  {
    Position kingPosition = king (color).getChessPosition ().toPosition ();
    List <Piece> opponentPieces = piecesOnBoard.stream ().filter (x -> ((ChessPiece) x).getColor () == opponent (color)).collect (Collectors.toList ());

    for (Piece p : opponentPieces)
    {
      boolean [][] matrix = p.possibleMoves ();
      if (matrix [kingPosition.getRow ()] [kingPosition.getColumn ()])
        return true;
    }
    return false;
  }

  private boolean testCheckMate (Color color)
  {
    if (testCheck (color))
    {
      List <Piece> pieces = piecesOnBoard.stream ().filter (x -> ((ChessPiece) x).getColor () == color).collect (Collectors.toList ());

      for (Piece p : pieces)
      {
        boolean [][] matrix = p.possibleMoves ();
        for (int i = 0; i < board.getRows (); i++)
          for (int j = 0; j < board.getColumns (); j++)
              if (matrix [i] [j])
              {
                Position source = ((ChessPiece) p).getChessPosition ().toPosition ();
                Position target = new Position (i, j);
                Piece capturedPiece = makeMove (source, target);
                boolean testCheck = testCheck (color);
                undoMove (source, target, capturedPiece);

                if (!testCheck)
                  return false;
              }
      }
      return true;
    }
    else
      return false;
  }

  private void placeNewPiece (char column, int row, ChessPiece piece)
  {
    board.placePiece (piece, new ChessPosition (column, row).toPosition ());
    piecesOnBoard.add (piece);
  }

  private void initialSetup ()
  {
    placeNewPiece ('a', 1, new Rook (board, Color.WHITE));
    placeNewPiece ('h', 1, new Rook (board, Color.WHITE));
    placeNewPiece ('a', 8, new Rook (board, Color.BLACK));
    placeNewPiece ('h', 8, new Rook (board, Color.BLACK));

    placeNewPiece('b', 1, new Knight (board, Color.WHITE));
    placeNewPiece('g', 1, new Knight (board, Color.WHITE));
    placeNewPiece('b', 8, new Knight (board, Color.BLACK));
    placeNewPiece('g', 8, new Knight (board, Color.BLACK));

    placeNewPiece('c', 1, new Bishop (board, Color.WHITE));
    placeNewPiece('f', 1, new Bishop (board, Color.WHITE));
    placeNewPiece('c', 8, new Bishop (board, Color.BLACK));
    placeNewPiece('f', 8, new Bishop (board, Color.BLACK));

    placeNewPiece ('d', 1, new Queen (board, Color.WHITE));
    placeNewPiece ('d', 8, new Queen (board, Color.BLACK));

    placeNewPiece ('e', 1, new King (board, Color.WHITE, this));
    placeNewPiece ('e', 8, new King (board, Color.BLACK, this));

    placeNewPiece ('a', 2, new Pawn (board, Color.WHITE, this));
    placeNewPiece ('b', 2, new Pawn (board, Color.WHITE, this));
    placeNewPiece ('c', 2, new Pawn (board, Color.WHITE, this));
    placeNewPiece ('d', 2, new Pawn (board, Color.WHITE, this));
    placeNewPiece ('e', 2, new Pawn (board, Color.WHITE, this));
    placeNewPiece ('f', 2, new Pawn (board, Color.WHITE, this));
    placeNewPiece ('g', 2, new Pawn (board, Color.WHITE, this));
    placeNewPiece ('h', 2, new Pawn (board, Color.WHITE, this));

    placeNewPiece ('a', 7, new Pawn (board, Color.BLACK, this));
    placeNewPiece ('b', 7, new Pawn (board, Color.BLACK, this));
    placeNewPiece ('c', 7, new Pawn (board, Color.BLACK, this));
    placeNewPiece ('d', 7, new Pawn (board, Color.BLACK, this));
    placeNewPiece ('e', 7, new Pawn (board, Color.BLACK, this));
    placeNewPiece ('f', 7, new Pawn (board, Color.BLACK, this));
    placeNewPiece ('g', 7, new Pawn (board, Color.BLACK, this));
    placeNewPiece ('h', 7, new Pawn (board, Color.BLACK, this));

    // placeNewPiece ('h', 7, new Rook (board, Color.WHITE));
    // placeNewPiece ('a', 1, new Rook (board, Color.WHITE));
    // placeNewPiece ('e', 1, new King (board, Color.WHITE, this));

    // placeNewPiece ('h', 8, new Rook (board, Color.BLACK));
    // placeNewPiece ('a', 8, new Rook (board, Color.BLACK));
    // placeNewPiece ('e', 8, new King (board, Color.BLACK, this));
  }
}
