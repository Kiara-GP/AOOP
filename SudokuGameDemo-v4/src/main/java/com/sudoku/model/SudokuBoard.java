package com.sudoku.model;

/**
 * SudokuBoard - a 9x9 Sudoku board.
 *
 * <p>Stores and manages 81 cells.</p>
 */
public class SudokuBoard {
    // Board size (9x9)
    public static final int SIZE = 9;
    // 3x3 box size
    public static final int BOX_SIZE = 3;
    // Minimum valid digit
    public static final int MIN_VALUE = 1;
    // Maximum valid digit
    public static final int MAX_VALUE = 9;

    // 81 cells
    private final Cell[][] board;

    /**
     * Creates an empty board.
     */
    public SudokuBoard() {
        board = new Cell[SIZE][SIZE];
        // Initialize all cells empty
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                board[row][col] = new Cell(0, false);
            }
        }
    }

    /**
     * Loads a puzzle from a string.
     *
     * @param puzzle 81 characters; '0' or '.' means empty
     */
    public void loadFromString(String puzzle) {
        // Remove whitespace and keep only digits/dots
        String cleaned = puzzle.replaceAll("[^0-9.]", "");
        if (cleaned.length() != SIZE * SIZE) {
            throw new IllegalArgumentException("Puzzle string must be exactly 81 characters");
        }

        for (int i = 0; i < SIZE * SIZE; i++) {
            char c = cleaned.charAt(i);
            int value = (c == '.' || c == '0') ? 0 : Character.getNumericValue(c);
            int row = i / SIZE;
            int col = i % SIZE;
            // Pre-filled digits are locked
            boolean locked = value != 0;
            board[row][col] = new Cell(value, locked);
        }
    }

    /**
     * Returns the cell at (row, col).
     */
    public Cell getCell(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            throw new IndexOutOfBoundsException("Row/col index out of bounds");
        }
        return board[row][col];
    }

    /**
     * Returns the value at (row, col).
     *
     * @return 0-9 (0 means empty)
     */
    public int getValue(int row, int col) {
        return board[row][col].getValue();
    }

    /**
     * Sets the value at (row, col).
     *
     * @return true if successful; false if locked/out of range/out of bounds
     */
    public boolean setValue(int row, int col, int value) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            return false;
        }
        if (board[row][col].isLocked()) {
            return false;
        }
        if (value < 0 || value > MAX_VALUE) {
            return false;
        }
        board[row][col].setValue(value);
        return true;
    }

    /**
     * Clears the value at (row, col) if the cell is not locked.
     */
    public void clearCell(int row, int col) {
        if (!board[row][col].isLocked()) {
            board[row][col].clear();
        }
    }

    /**
     * Returns all values for a row.
     */
    public int[] getRow(int row) {
        int[] values = new int[SIZE];
        for (int col = 0; col < SIZE; col++) {
            values[col] = board[row][col].getValue();
        }
        return values;
    }

    /**
     * Returns all values for a column.
     */
    public int[] getColumn(int col) {
        int[] values = new int[SIZE];
        for (int row = 0; row < SIZE; row++) {
            values[row] = board[row][col].getValue();
        }
        return values;
    }

    /**
     * Returns all values for a 3x3 box.
     */
    public int[] getBox(int boxRow, int boxCol) {
        int[] values = new int[SIZE];
        int index = 0;
        int startRow = boxRow * BOX_SIZE;
        int startCol = boxCol * BOX_SIZE;
        for (int row = startRow; row < startRow + BOX_SIZE; row++) {
            for (int col = startCol; col < startCol + BOX_SIZE; col++) {
                values[index++] = board[row][col].getValue();
            }
        }
        return values;
    }

    /**
     * Returns (boxRow, boxCol) for a cell position.
     */
    public static int[] getBoxIndex(int row, int col) {
        return new int[]{row / BOX_SIZE, col / BOX_SIZE};
    }

    /**
     * Checks whether placing {@code value} at (row, col) is valid (Sudoku rules).
     *
     * <p>Checks duplicates in the row, column, and the 3x3 box.</p>
     */
    public boolean isValidMove(int row, int col, int value) {
        if (value < MIN_VALUE || value > MAX_VALUE) {
            return false;
        }

        // Row
        for (int c = 0; c < SIZE; c++) {
            if (c != col && board[row][c].getValue() == value) {
                return false;
            }
        }

        // Column
        for (int r = 0; r < SIZE; r++) {
            if (r != row && board[r][col].getValue() == value) {
                return false;
            }
        }

        // 3x3 box
        int boxRow = (row / BOX_SIZE) * BOX_SIZE;
        int boxCol = (col / BOX_SIZE) * BOX_SIZE;
        for (int r = boxRow; r < boxRow + BOX_SIZE; r++) {
            for (int c = boxCol; c < boxCol + BOX_SIZE; c++) {
                if ((r != row || c != col) && board[r][c].getValue() == value) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns true if the board has no empty cells.
     */
    public boolean isComplete() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (board[row][col].isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns true if the board satisfies Sudoku constraints.
     */
    public boolean isValid() {
        // Rows
        for (int row = 0; row < SIZE; row++) {
            if (!isValidGroup(getRow(row))) {
                return false;
            }
        }

        // Columns
        for (int col = 0; col < SIZE; col++) {
            if (!isValidGroup(getColumn(col))) {
                return false;
            }
        }

        // Boxes
        for (int boxRow = 0; boxRow < BOX_SIZE; boxRow++) {
            for (int boxCol = 0; boxCol < BOX_SIZE; boxCol++) {
                if (!isValidGroup(getBox(boxRow, boxCol))) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns true if a group contains no duplicates (ignoring zeros).
     */
    private boolean isValidGroup(int[] values) {
        boolean[] seen = new boolean[SIZE + 1]; // indices 1-9 used
        for (int value : values) {
            if (value != 0) {
                if (seen[value]) {
                    return false; // duplicate digit
                }
                seen[value] = true;
            }
        }
        return true;
    }

    /**
     * Returns true if the board is complete and valid.
     */
    public boolean isWon() {
        return isComplete() && isValid();
    }

    /**
     * Deep-copies the board.
     */
    public SudokuBoard copy() {
        SudokuBoard newBoard = new SudokuBoard();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                Cell original = board[row][col];
                newBoard.board[row][col] = new Cell(original.getValue(), original.isLocked());
            }
        }
        return newBoard;
    }

    /**
     * Returns an 81-character string representation.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                sb.append(board[row][col].toString());
            }
        }
        return sb.toString();
    }
}
