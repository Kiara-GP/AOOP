package com.sudoku.model;

/**
 * Cell - a single Sudoku board cell.
 *
 * <p>A cell has a value (1-9, or 0 for empty) and a locked flag.</p>
 */
public class Cell {
    // 0 means empty, 1-9 are digits
    private int value;
    // Locked (pre-filled) cells cannot be modified
    private final boolean locked;

    /**
     * Creates a cell.
     *
     * @param value 0-9
     * @param locked true if pre-filled
     */
    public Cell(int value, boolean locked) {
        this.value = value;
        this.locked = locked;
    }

    /**
     * Returns the current value (0-9).
     */
    public int getValue() {
        return value;
    }

    /**
     * Sets the value if not locked.
     */
    public void setValue(int value) {
        if (!locked) {
            this.value = value;
        }
    }

    /**
     * Returns true if the cell is empty.
     */
    public boolean isEmpty() {
        return value == 0;
    }

    /**
     * Returns true if the cell is locked (pre-filled).
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * Clears the cell if not locked.
     */
    public void clear() {
        if (!locked) {
            this.value = 0;
        }
    }

    @Override
    public String toString() {
        if (value == 0) {
            return " ";
        }
        return String.valueOf(value);
    }
}
