package com.sudoku.model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Random;
import java.util.Stack;

/**
 * SudokuModel - core model (Model layer) for Sudoku.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *     <li>Load puzzles from {@code puzzles.txt}</li>
 *     <li>Maintain game state</li>
 *     <li>Validate moves and detect completion</li>
 *     <li>Notify observers (GUI View) on state changes</li>
 * </ul>
 *
 * <p>The model contains no UI logic.</p>
 */
@SuppressWarnings("deprecation")
public class SudokuModel extends Observable implements SudokuModelInterface {
    // Sudoku board state
    private SudokuBoard board;
    // Current puzzle index
    private int currentPuzzleIndex;
    // Total number of puzzles in the file
    private int totalPuzzleCount;
    // Whether the game has started
    private boolean gameStarted;
    // Timer fields
    private long startTime;
    private long elapsedTime;

    // ===== FR3: three boolean flags =====
    // Validation feedback flag - immediately reports invalid moves
    private boolean validationFeedbackEnabled;
    // Hint flag - enables/disables hint functionality
    private boolean hintEnabled;
    // Puzzle selection flag - true=random, false=fixed
    private boolean puzzleSelectionRandom;

    // Difficulty selection (used for random selection)
    private Difficulty selectedDifficulty;

    // ===== FR5: undo support (single-level) =====
    private Stack<Move> undoStack;

    // ===== FR4: initial puzzle snapshot (for reset) =====
    private int[][] initialPuzzle;

    /** Move record used for undo. */
    private static class Move {
        int row, col, previousValue, newValue;

        Move(int row, int col, int previousValue, int newValue) {
            this.row = row;
            this.col = col;
            this.previousValue = previousValue;
            this.newValue = newValue;
        }
    }

    /** Constructs a new model instance. */
    public SudokuModel() {
        board = new SudokuBoard();
        currentPuzzleIndex = 0;
        gameStarted = false;
        initialPuzzle = new int[9][9];

        // Default flags (FR3)
        validationFeedbackEnabled = true;
        hintEnabled = true;
        puzzleSelectionRandom = false;
        selectedDifficulty = Difficulty.MEDIUM;

        // Undo stack
        undoStack = new Stack<>();

        countPuzzles();
    }

    /** Notifies observers (GUI View) to refresh. */
    private void notifyModelChanged() {
        setChanged();
        notifyObservers();
    }

    // ============================================================
    // Specifications (JML-style) and assertion helpers (NFR4)
    // ============================================================

    /**
     * @invariant board != null
     * @invariant totalPuzzleCount > 0
     * @invariant initialPuzzle != null && initialPuzzle.length == 9 && initialPuzzle[0].length == 9
     * @invariant undoStack != null
     */

    private static boolean isInRange(int x, int minInclusive, int maxInclusive) {
        return x >= minInclusive && x <= maxInclusive;
    }

    private static void assertRowColInRange(int row, int col) {
        assert isInRange(row, 0, 8) : "row must be in [0,8]";
        assert isInRange(col, 0, 8) : "col must be in [0,8]";
    }

    private static boolean rowColInRange(int row, int col) {
        return isInRange(row, 0, 8) && isInRange(col, 0, 8);
    }

    private void assertGameStartedIfRequired() {
        assert !gameStarted || board != null : "If gameStarted, board must not be null";
    }

    private void assertPostLoadPuzzleState() {
        assert board != null : "Board must not be null after load";
        assert initialPuzzle != null : "Initial puzzle snapshot must exist";
        assert undoStack != null : "Undo stack must exist";
        assert gameStarted : "Game must be started after successful load";
    }

    /** Counts puzzles in {@code puzzles.txt}. */
    private void countPuzzles() {
        totalPuzzleCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader("puzzles.txt"))) {
            while (reader.readLine() != null) {
                totalPuzzleCount++;
            }
        } catch (IOException e) {
            System.err.println("Unable to read puzzles.txt: " + e.getMessage());
            totalPuzzleCount = 1;
        }
    }

    /**
     * Reads one puzzle line from {@code puzzles.txt}.
     *
     * @param lineNumber zero-based line index
     * @return puzzle string or {@code null} if not found
     */
    private String readPuzzleLine(int lineNumber) {
        try (BufferedReader reader = new BufferedReader(new FileReader("puzzles.txt"))) {
            String line;
            int currentLine = 0;
            while ((line = reader.readLine()) != null) {
                if (currentLine == lineNumber) {
                    return line.trim();
                }
                currentLine++;
            }
        } catch (IOException e) {
            System.err.println("Failed to read puzzle: " + e.getMessage());
        }
        return null;
    }

    @Override
    public int getPuzzleCount() {
        return totalPuzzleCount;
    }

    @Override
    public int getCurrentPuzzleIndex() {
        return currentPuzzleIndex;
    }

    @Override
    public void setCurrentPuzzleIndex(int index) {
        /*
         * @requires 0 <= index < totalPuzzleCount
         * @ensures currentPuzzleIndex == index
         */
        assertInvariants();
        assert index >= 0 && index < totalPuzzleCount : "index must be within puzzle count";
        if (index >= 0 && index < totalPuzzleCount) {
            currentPuzzleIndex = index;
        }
        assertInvariants();
    }

    /**
     * Gets the raw puzzle string for an index.
     *
     * <p>This is not used by CLI/GUI directly; it exists for completeness.</p>
     */
    public String getPuzzle(int index) {
        if (index >= 0 && index < totalPuzzleCount) {
            return readPuzzleLine(index);
        }
        return null;
    }

    /** Starts a new game (FR5: respects puzzle selection flag). */
    @Override
    public boolean newGame() {
        /*
         * @requires totalPuzzleCount > 0
         * @ensures \result == true ==> gameStarted == true
         */
        assertInvariants();
        if (puzzleSelectionRandom) {
            // Random selection by inferred difficulty buckets
            currentPuzzleIndex = getRandomPuzzleByDifficulty(selectedDifficulty);
        }
        boolean ok = loadPuzzle(currentPuzzleIndex);
        assertInvariants();
        return ok;
    }

    /** Picks a random puzzle matching a difficulty bucket. */
    private int getRandomPuzzleByDifficulty(Difficulty difficulty) {
        Random random = new Random();
        int maxAttempts = 50;

        for (int i = 0; i < maxAttempts; i++) {
            int index = random.nextInt(totalPuzzleCount);
            String puzzle = readPuzzleLine(index);
            if (puzzle != null && matchesDifficulty(puzzle, difficulty)) {
                return index;
            }
        }
        // Fallback to any random puzzle
        return random.nextInt(totalPuzzleCount);
    }

    /** Checks whether a puzzle string matches a rough difficulty bucket. */
    private boolean matchesDifficulty(String puzzle, Difficulty difficulty) {
        int emptyCount = 0;
        for (char c : puzzle.toCharArray()) {
            if (c == '0' || c == '.') {
                emptyCount++;
            }
        }

        switch (difficulty) {
            case EASY: return emptyCount <= 40;
            case MEDIUM: return emptyCount > 40 && emptyCount <= 50;
            case HARD: return emptyCount > 50 && emptyCount <= 55;
            case EXPERT: return emptyCount > 55;
            default: return true;
        }
    }

    /**
     * Loads a puzzle by index.
     *
     * <p>FR5: loading must not trigger completion detection.</p>
     */
    @Override
    public boolean loadPuzzle(int puzzleIndex) {
        /*
         * @requires 0 <= puzzleIndex < totalPuzzleCount
         * @ensures \result == true ==> gameStarted == true
         * @ensures \result == true ==> undoStack.isEmpty()
         */
        assertInvariants();
        if (puzzleIndex < 0 || puzzleIndex >= totalPuzzleCount) {
            return false;
        }

        String puzzleString = readPuzzleLine(puzzleIndex);
        if (puzzleString == null) {
            return false;
        }

        currentPuzzleIndex = puzzleIndex;
        board = new SudokuBoard();
        board.loadFromString(puzzleString);

        // Snapshot for reset (FR4/FR5)
        saveInitialPuzzle(puzzleString);

        // Clear undo stack
        undoStack.clear();

        gameStarted = true;
        startTime = System.currentTimeMillis();
        elapsedTime = 0;
        notifyModelChanged();
        assertPostLoadPuzzleState();
        assertInvariants();
        return true;
    }

    /** Saves the initial puzzle values for reset/locking rules. */
    private void saveInitialPuzzle(String puzzle) {
        for (int i = 0; i < 81; i++) {
            int row = i / 9;
            int col = i % 9;
            char c = puzzle.charAt(i);
            initialPuzzle[row][col] = (c == '.' || c == '0') ? 0 : Character.getNumericValue(c);
        }
    }

    /**
     * Exposes the internal board for legacy code.
     *
     * <p>New code (especially CLI) should use {@link #getCellValue(int, int)} instead.</p>
     */
    public SudokuBoard getBoard() {
        return board;
    }

    @Override
    public int getCellValue(int row, int col) {
        /*
         * @requires 0 <= row <= 8 && 0 <= col <= 8
         * @ensures \result == board.getValue(row,col)
         */
        assertInvariants();
        assertRowColInRange(row, col);
        return board.getValue(row, col);
    }

    /**
     * Sets a value at (row,col).
     *
     * <p>FR4: only initially empty cells may be edited.</p>
     * <p>FR2: when validation feedback is enabled, invalid moves are allowed but flagged.</p>
     */
    @Override
    public MoveResult setValue(int row, int col, int value) {
        /*
         * @requires gameStarted == true
         * @requires 0 <= row <= 8 && 0 <= col <= 8
         * @requires 1 <= value <= 9
         * @requires initialPuzzle[row][col] == 0  (editable cell)
         * @ensures \result.success == true ==> board.getValue(row,col) == value
         * @ensures \result.success == false ==> board.getValue(row,col) unchanged
         */
        assertInvariants();
        assertGameStartedIfRequired();
        if (!rowColInRange(row, col)) {
            return new MoveResult(false, false, "Row/col out of range");
        }
        if (!gameStarted) {
            return new MoveResult(false, false, "Game has not started");
        }

        // Locked cell check (FR4)
        if (initialPuzzle[row][col] != 0) {
            return new MoveResult(false, false, "Locked (pre-filled) cells cannot be modified");
        }

        // Input range check (FR4)
        if (value < 1 || value > 9) {
            return new MoveResult(false, false, "Invalid input: please enter a digit 1-9");
        }

        int previousValue = board.getValue(row, col);
        boolean moveOk = board.isValidMove(row, col, value);
        boolean success = board.setValue(row, col, value);

        if (success) {
            undoStack.push(new Move(row, col, previousValue, value));
            // FR2: keep the move but flag it when validation feedback is enabled.
            boolean hasWarning = validationFeedbackEnabled && !moveOk;
            String message = hasWarning ? ("Placed " + value + " (conflict detected)") : ("Placed " + value);
            notifyModelChanged();
            assert board.getValue(row, col) == value : "Postcondition: value must be written";
            assertInvariants();
            return new MoveResult(true, hasWarning, message);
        }

        assert board.getValue(row, col) == previousValue : "Postcondition: value must remain unchanged on failure";
        assertInvariants();
        return new MoveResult(false, false, "Failed to set value");
    }

    /** Encapsulates the result of a move. */
    public static class MoveResult {
        public final boolean success;
        public final boolean hasWarning;
        public final String message;

        public MoveResult(boolean success, boolean hasWarning, String message) {
            this.success = success;
            this.hasWarning = hasWarning;
            this.message = message;
        }
    }

    /** Clears a cell (FR4: locked cells cannot be cleared). */
    @Override
    public MoveResult clearCell(int row, int col) {
        /*
         * @requires gameStarted == true
         * @requires 0 <= row <= 8 && 0 <= col <= 8
         * @requires initialPuzzle[row][col] == 0
         * @ensures \result.success == true ==> board.getValue(row,col) == 0
         */
        assertInvariants();
        if (!rowColInRange(row, col)) {
            return new MoveResult(false, false, "Row/col out of range");
        }
        if (!gameStarted) {
            return new MoveResult(false, false, "Game has not started");
        }

        // Locked cell check (FR4)
        if (initialPuzzle[row][col] != 0) {
            return new MoveResult(false, false, "Locked (pre-filled) cells cannot be cleared");
        }

        int previousValue = board.getValue(row, col);
        if (previousValue == 0) {
            return new MoveResult(false, false, "Cell is already empty");
        }

        board.clearCell(row, col);
        undoStack.push(new Move(row, col, previousValue, 0));

        notifyModelChanged();
        assert board.getValue(row, col) == 0 : "Postcondition: cell must be empty after clear";
        assertInvariants();
        return new MoveResult(true, false, "Cleared");
    }

    /** Undo the last action (FR5: single-level undo). */
    @Override
    public UndoResult undo() {
        /*
         * @requires gameStarted == true
         * @ensures \result.success == true ==> the last move is reverted
         */
        assertInvariants();
        if (!gameStarted) {
            return new UndoResult(false, "Game has not started");
        }

        if (undoStack.isEmpty()) {
            return new UndoResult(false, "Nothing to undo");
        }

        Move lastMove = undoStack.pop();
        int current = board.getValue(lastMove.row, lastMove.col);
        board.setValue(lastMove.row, lastMove.col, lastMove.previousValue);

        notifyModelChanged();
        assert board.getValue(lastMove.row, lastMove.col) == lastMove.previousValue
                : "Postcondition: undo must restore previous value";
        assertInvariants();
        return new UndoResult(true, "Undone: " + lastMove.newValue + " -> " +
                (lastMove.previousValue == 0 ? "empty" : lastMove.previousValue));
    }

    /** Encapsulates the result of an undo operation. */
    public static class UndoResult {
        public final boolean success;
        public final String message;

        public UndoResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    /** Returns a hint (FR5: reveals one correct value for an empty cell). */
    @Override
    public HintResult getHint() {
        /*
         * @requires gameStarted == true
         * @requires hintEnabled == true
         * @ensures \result.success == true ==> 0 <= row,col <= 8 and 1 <= value <= 9
         */
        assertInvariants();
        if (!gameStarted) {
            return new HintResult(false, -1, -1, 0, "Game has not started");
        }

        if (!hintEnabled) {
            return new HintResult(false, -1, -1, 0, "Hint is disabled");
        }

        // Create a copy for solving
        SudokuBoard tempBoard = board.copy();

        // Find the first empty cell
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (tempBoard.getValue(row, col) == 0) {
                    // Use backtracking to find a solution
                    for (int num = 1; num <= 9; num++) {
                        if (tempBoard.isValidMove(row, col, num)) {
                            tempBoard.setValue(row, col, num);
                            if (solveSudoku(tempBoard)) {
                                return new HintResult(true, row, col, num,
                                        "Hint: row " + (row + 1) + ", col " + (col + 1) + " should be " + num);
                            }
                            tempBoard.setValue(row, col, 0);
                        }
                    }
                    return new HintResult(false, -1, -1, 0, "No solution found");
                }
            }
        }

        return new HintResult(false, -1, -1, 0, "No empty cell available for hint");
    }

    /** Backtracking Sudoku solver used for hint generation. */
    private boolean solveSudoku(SudokuBoard tempBoard) {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (tempBoard.getValue(row, col) == 0) {
                    for (int num = 1; num <= 9; num++) {
                        if (tempBoard.isValidMove(row, col, num)) {
                            tempBoard.setValue(row, col, num);
                            if (solveSudoku(tempBoard)) {
                                return true;
                            }
                            tempBoard.setValue(row, col, 0);
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    /** Encapsulates the result of a hint request. */
    public static class HintResult {
        public final boolean success;
        public final int row, col, value;
        public final String message;

        public HintResult(boolean success, int row, int col, int value, String message) {
            this.success = success;
            this.row = row;
            this.col = col;
            this.value = value;
            this.message = message;
        }
    }

    /** Checks whether a move is valid with Sudoku rules (FR2). */
    @Override
    public boolean isValidMove(int row, int col, int value) {
        /*
         * @requires 0 <= row <= 8 && 0 <= col <= 8
         * @ensures \result == true ==> placing value would not violate Sudoku constraints
         */
        assertInvariants();
        assertRowColInRange(row, col);
        return board.isValidMove(row, col, value);
    }

    /** Validates the whole board (FR2). */
    @Override
    public boolean isBoardValid() {
        return board.isValid();
    }

    /** Returns positions of invalid cells (FR2 validation feedback support). */
    @Override
    public List<int[]> getInvalidCells() {
        /*
         * @ensures \result contains only positions within the 9x9 board
         */
        assertInvariants();
        List<int[]> invalidCells = new ArrayList<>();

        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                int value = board.getValue(row, col);
                if (value == 0) continue;

                // Row conflict
                for (int c = 0; c < 9; c++) {
                    if (c != col && board.getValue(row, c) == value) {
                        invalidCells.add(new int[]{row, col});
                        break;
                    }
                }
                if (invalidCells.size() > 0 && invalidCells.get(invalidCells.size() - 1)[0] == row && invalidCells.get(invalidCells.size() - 1)[1] == col) {
                    continue;
                }

                // Column conflict
                for (int r = 0; r < 9; r++) {
                    if (r != row && board.getValue(r, col) == value) {
                        invalidCells.add(new int[]{row, col});
                        break;
                    }
                }
                if (invalidCells.size() > 0 && invalidCells.get(invalidCells.size() - 1)[0] == row && invalidCells.get(invalidCells.size() - 1)[1] == col) {
                    continue;
                }

                // 3x3 box conflict
                int boxRow = (row / 3) * 3;
                int boxCol = (col / 3) * 3;
                for (int r = boxRow; r < boxRow + 3; r++) {
                    for (int c = boxCol; c < boxCol + 3; c++) {
                        if (r != row && c != col && board.getValue(r, c) == value) {
                            invalidCells.add(new int[]{row, col});
                            break;
                        }
                    }
                    if (invalidCells.size() > 0 && invalidCells.get(invalidCells.size() - 1)[0] == row && invalidCells.get(invalidCells.size() - 1)[1] == col) {
                        break;
                    }
                }
            }
        }
        for (int[] pos : invalidCells) {
            assert pos != null && pos.length == 2 : "Invalid cell position must be a pair";
            assertRowColInRange(pos[0], pos[1]);
        }
        assertInvariants();
        return invalidCells;
    }

    /** Checks whether the puzzle is correctly completed (FR1). */
    @Override
    public boolean isGameWon() {
        return gameStarted && board.isWon();
    }

    @Override
    public boolean isBoardComplete() {
        return board.isComplete();
    }

    /** Returns elapsed time in milliseconds. */
    public long getElapsedTime() {
        if (!gameStarted) {
            return 0;
        }
        return System.currentTimeMillis() - startTime + elapsedTime;
    }

    /** Pauses the timer. */
    public void pauseTimer() {
        if (gameStarted) {
            elapsedTime += System.currentTimeMillis() - startTime;
        }
    }

    /** Resumes the timer. */
    public void resumeTimer() {
        if (gameStarted) {
            startTime = System.currentTimeMillis();
        }
    }

    /** Returns formatted time as {@code MM:SS}. */
    @Override
    public String getFormattedTime() {
        long totalSeconds = getElapsedTime() / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Exposes gameStarted for compatibility.
     *
     * <p>GUI/CLI should not rely on this for correctness; it is mainly used by tests.</p>
     */
    public boolean isGameStarted() {
        return gameStarted;
    }

    /**
     * Resets the current puzzle to its initial state (FR5).
     *
     * <p>Reset must not trigger completion detection.</p>
     */
    @Override
    public void resetGame() {
        /*
         * @requires currentPuzzleIndex is within range
         * @ensures board state equals the initially loaded puzzle
         */
        assertInvariants();
        boolean wasStarted = gameStarted;

        // Reload the puzzle (also clears undo stack)
        loadPuzzle(currentPuzzleIndex);

        // Keep previous started state
        if (!wasStarted) {
            gameStarted = false;
        }
        assertInvariants();
    }

    /** Returns candidate numbers for a cell position. */
    public int[] getCandidateNumbers(int row, int col) {
        /*
         * @requires 0 <= row <= 8 && 0 <= col <= 8
         * @ensures all returned values are in [1,9] and valid for the current board
         */
        assertInvariants();
        assertRowColInRange(row, col);
        List<Integer> candidates = new ArrayList<>();
        for (int num = 1; num <= 9; num++) {
            if (board.isValidMove(row, col, num)) {
                candidates.add(num);
            }
        }
        return candidates.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Returns true if the cell is locked (pre-filled) (FR4). */
    @Override
    public boolean isCellLocked(int row, int col) {
        /*
         * @requires 0 <= row <= 8 && 0 <= col <= 8
         * @ensures \result == true <==> cell was pre-filled in the initial puzzle
         */
        assertInvariants();
        assertRowColInRange(row, col);
        return initialPuzzle[row][col] != 0;
    }

    // ===== FR3: flag accessors =====

    @Override
    public boolean isValidationFeedbackEnabled() {
        return validationFeedbackEnabled;
    }

    @Override
    public void setValidationFeedbackEnabled(boolean enabled) {
        /*
         * @ensures validationFeedbackEnabled == enabled
         */
        assertInvariants();
        this.validationFeedbackEnabled = enabled;
        assert this.validationFeedbackEnabled == enabled : "Postcondition: flag must be set";
        assertInvariants();
    }

    @Override
    public boolean isHintEnabled() {
        return hintEnabled;
    }

    @Override
    public void setHintEnabled(boolean enabled) {
        /*
         * @ensures hintEnabled == enabled
         */
        assertInvariants();
        this.hintEnabled = enabled;
        assert this.hintEnabled == enabled : "Postcondition: flag must be set";
        assertInvariants();
    }

    @Override
    public boolean isPuzzleSelectionRandom() {
        return puzzleSelectionRandom;
    }

    @Override
    public void setPuzzleSelectionRandom(boolean random) {
        /*
         * @ensures puzzleSelectionRandom == random
         */
        assertInvariants();
        this.puzzleSelectionRandom = random;
        assert this.puzzleSelectionRandom == random : "Postcondition: flag must be set";
        assertInvariants();
    }

    public Difficulty getSelectedDifficulty() {
        return selectedDifficulty;
    }

    public void setSelectedDifficulty(Difficulty difficulty) {
        /*
         * @requires difficulty != null
         * @ensures selectedDifficulty == difficulty
         */
        assertInvariants();
        assert difficulty != null : "difficulty must not be null";
        this.selectedDifficulty = difficulty;
        assert this.selectedDifficulty == difficulty : "Postcondition: difficulty must be set";
        assertInvariants();
    }

    // ===== Difficulty inference =====

    /** Difficulty buckets inferred from empty cell count. */
    public enum Difficulty {
        EASY("Easy"),
        MEDIUM("Medium"),
        HARD("Hard"),
        EXPERT("Expert");

        private final String displayName;

        Difficulty(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    public Difficulty inferDifficulty() {
        /*
         * @ensures \result != null
         */
        assertInvariants();
        int emptyCells = getEmptyCellCount();

        if (emptyCells <= 40) {
            return Difficulty.EASY;
        } else if (emptyCells <= 50) {
            return Difficulty.MEDIUM;
        } else if (emptyCells <= 55) {
            return Difficulty.HARD;
        } else {
            return Difficulty.EXPERT;
        }
    }

    @Override
    public int getEmptyCellCount() {
        /*
         * @ensures 0 <= \result <= 81
         */
        assertInvariants();
        int count = 0;
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (board.getValue(row, col) == 0) {
                    count++;
                }
            }
        }
        assert count >= 0 && count <= 81 : "Postcondition: empty cell count must be within [0,81]";
        return count;
    }

    // ===== NFR4: assertions for model invariants =====

    @Override
    public void assertInvariants() {
        assert board != null : "Invariant: board must not be null";
        assert totalPuzzleCount > 0 : "Invariant: puzzle count must be > 0";
        assert initialPuzzle != null && initialPuzzle.length == 9 && initialPuzzle[0].length == 9
                : "Invariant: initialPuzzle must be a 9x9 array";
        assert undoStack != null : "Invariant: undoStack must not be null";
    }
}
