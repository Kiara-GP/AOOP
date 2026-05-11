package com.sudoku.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SudokuBoard}.
 */
public class SudokuBoardTest {

    @Test
    public void testCreateEmptyBoard() {
        SudokuBoard board = new SudokuBoard();
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                assertEquals(0, board.getValue(row, col));
                assertTrue(board.getCell(row, col).isEmpty());
            }
        }
    }

    @Test
    public void testLoadFromString() {
        SudokuBoard board = new SudokuBoard();
        // Must be 81 characters; build by repeating "123456789" nine times.
        StringBuilder sb = new StringBuilder(81);
        for (int i = 0; i < 9; i++) {
            sb.append("123456789");
        }
        board.loadFromString(sb.toString());
        assertEquals(1, board.getValue(0, 0));
        assertEquals(9, board.getValue(0, 8));
    }

    @Test
    public void testLoadPuzzleWithZeros() {
        SudokuBoard board = new SudokuBoard();
        board.loadFromString("530070000600195000098000060800060003400803001700020006060000280000419005000080079");

        // First digit
        assertEquals(5, board.getValue(0, 0));
        // Row starts with 530..., so (0,1)=3 and (0,2) is empty.
        assertEquals(0, board.getValue(0, 2));
    }

    @Test
    public void testSetCellValue() {
        SudokuBoard board = new SudokuBoard();
        assertTrue(board.setValue(0, 0, 5));
        assertEquals(5, board.getValue(0, 0));
    }

    @Test
    public void testCannotModifyLockedCell() {
        SudokuBoard board = new SudokuBoard();
        board.loadFromString("530000000000000000000000000000000000000000000000000000000000000000000000000000000");
        // First cell is locked
        assertTrue(board.getCell(0, 0).isLocked());
        assertFalse(board.setValue(0, 0, 9));
        assertEquals(5, board.getValue(0, 0));
    }

    @Test
    public void testRowValidationValid() {
        SudokuBoard board = new SudokuBoard();
        board.setValue(0, 0, 1);
        board.setValue(0, 1, 2);
        assertTrue(board.isValidMove(0, 2, 3));
    }

    @Test
    public void testRowValidationInvalid() {
        SudokuBoard board = new SudokuBoard();
        board.setValue(0, 0, 5);
        assertFalse(board.isValidMove(0, 1, 5));
    }

    @Test
    public void testColumnValidationInvalid() {
        SudokuBoard board = new SudokuBoard();
        board.setValue(0, 0, 5);
        assertFalse(board.isValidMove(1, 0, 5));
    }

    @Test
    public void testBoxValidationInvalid() {
        SudokuBoard board = new SudokuBoard();
        board.setValue(0, 0, 5);
        assertFalse(board.isValidMove(1, 1, 5));
    }

    @Test
    public void testIsCompleteFalse() {
        SudokuBoard board = new SudokuBoard();
        board.setValue(0, 0, 1);
        assertFalse(board.isComplete());
    }

    @Test
    public void testIsCompleteTrueButInvalid() {
        SudokuBoard board = new SudokuBoard();
        // Fill the board but with duplicates (complete but invalid)
        for (int i = 0; i < 81; i++) {
            int row = i / 9;
            int col = i % 9;
            board.setValue(row, col, (i % 9) + 1);
        }
        assertTrue(board.isComplete());
        assertFalse(board.isValid());
    }

    @Test
    public void testGetBoxIndex() {
        int[] index1 = SudokuBoard.getBoxIndex(0, 0);
        assertEquals(0, index1[0]);
        assertEquals(0, index1[1]);

        int[] index2 = SudokuBoard.getBoxIndex(4, 4);
        assertEquals(1, index2[0]);
        assertEquals(1, index2[1]);

        int[] index3 = SudokuBoard.getBoxIndex(8, 8);
        assertEquals(2, index3[0]);
        assertEquals(2, index3[1]);
    }

    @Test
    public void testClearCell() {
        SudokuBoard board = new SudokuBoard();
        board.setValue(0, 0, 5);
        board.clearCell(0, 0);
        assertEquals(0, board.getValue(0, 0));
    }

    @Test
    public void testCopy() {
        SudokuBoard board = new SudokuBoard();
        board.setValue(0, 0, 5);

        SudokuBoard copy = board.copy();
        assertEquals(5, copy.getValue(0, 0));

        // Modifying the copy does not affect the original
        copy.setValue(0, 0, 9);
        assertEquals(5, board.getValue(0, 0));
    }
}
