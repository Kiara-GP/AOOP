package com.sudoku.model;

import java.util.List;
import java.util.Observer;

/**
 * Public interface for the Sudoku Model.
 *
 * <p>This interface exists to enforce loose coupling: the GUI Controller, GUI View,
 * CLI application, and JUnit tests should depend on this interface rather than the
 * concrete {@link SudokuModel} class or internal helper classes.</p>
 */
public interface SudokuModelInterface {

    // Observer wiring (to support the Observable/Observer requirement without exposing concrete classes)
    void addObserver(Observer o);

    void deleteObserver(Observer o);

    // Puzzle management
    int getPuzzleCount();

    int getCurrentPuzzleIndex();

    void setCurrentPuzzleIndex(int index);

    boolean newGame();

    boolean loadPuzzle(int puzzleIndex);

    // Board access via the model interface only (no SudokuBoard/Cell exposure)
    int getCellValue(int row, int col);

    boolean isCellLocked(int row, int col);

    // Moves / commands
    SudokuModel.MoveResult setValue(int row, int col, int value);

    SudokuModel.MoveResult clearCell(int row, int col);

    SudokuModel.UndoResult undo();

    SudokuModel.HintResult getHint();

    void resetGame();

    // Validation / completion
    boolean isValidMove(int row, int col, int value);

    boolean isBoardValid();

    List<int[]> getInvalidCells();

    boolean isGameWon();

    boolean isBoardComplete();

    // Flags (FR3)
    boolean isValidationFeedbackEnabled();

    void setValidationFeedbackEnabled(boolean enabled);

    boolean isHintEnabled();

    void setHintEnabled(boolean enabled);

    boolean isPuzzleSelectionRandom();

    void setPuzzleSelectionRandom(boolean random);

    SudokuModel.Difficulty inferDifficulty();

    int getEmptyCellCount();

    String getFormattedTime();

    // Assertions / specification helper
    void assertInvariants();
}

