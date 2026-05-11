package com.sudoku.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SudokuModel}.
 *
 * <p>NFR5: tests focus on the Model and include both valid and invalid scenarios.</p>
 */
public class SudokuModelTest {

    @Test
    public void testCreateModel() {
        SudokuModel model = new SudokuModel();
        assertNotNull(model.getBoard());
        assertFalse(model.isGameStarted());
    }

    @Test
    public void testLoadPuzzle() {
        SudokuModel model = new SudokuModel();
        assertTrue(model.loadPuzzle(0));
        assertTrue(model.isGameStarted());
        assertEquals(0, model.getCurrentPuzzleIndex());
    }

    @Test
    public void testLoadInvalidPuzzleIndex() {
        SudokuModel model = new SudokuModel();
        assertFalse(model.loadPuzzle(-1));
        assertFalse(model.loadPuzzle(99999));
    }

    @Test
    public void testSetValue() {
        SudokuModel model = new SudokuModel();
        model.loadPuzzle(0);
        // Disable validation feedback to avoid warnings affecting this test.
        model.setValidationFeedbackEnabled(false);

        // Find the first editable cell and place a value.
        boolean foundEmpty = false;
        for (int row = 0; row < 9 && !foundEmpty; row++) {
            for (int col = 0; col < 9 && !foundEmpty; col++) {
                if (!model.isCellLocked(row, col)) {
                    SudokuModel.MoveResult result = model.setValue(row, col, 1);
                    assertTrue(result.success);
                    assertFalse(result.hasWarning);
                    assertEquals(1, model.getCellValue(row, col));
                    foundEmpty = true;
                }
            }
        }
    }

    @Test
    public void testCannotModifyLockedCell() {
        SudokuModel model = new SudokuModel();
        model.loadPuzzle(0);

        // Find a locked (pre-filled) cell.
        boolean foundLocked = false;
        for (int row = 0; row < 9 && !foundLocked; row++) {
            for (int col = 0; col < 9 && !foundLocked; col++) {
                if (model.isCellLocked(row, col)) {
                    SudokuModel.MoveResult result = model.setValue(row, col, 9);
                    assertFalse(result.success);
                    assertTrue(result.message.toLowerCase().contains("locked"));
                    foundLocked = true;
                }
            }
        }
    }

    @Test
    public void testValidationFeedbackOnInvalidMove() {
        SudokuModel model = new SudokuModel();
        model.loadPuzzle(0);
        model.setValidationFeedbackEnabled(true);

        // In the same row, find two editable cells and place the same number to create a conflict (FR2 warning).
        for (int r = 0; r < 9; r++) {
            int c1 = -1;
            int c2 = -1;
            for (int c = 0; c < 9; c++) {
                if (!model.isCellLocked(r, c)) {
                    if (c1 < 0) {
                        c1 = c;
                    } else {
                        c2 = c;
                        break;
                    }
                }
            }
            if (c1 >= 0 && c2 >= 0) {
                model.setValue(r, c1, 5);
                SudokuModel.MoveResult result = model.setValue(r, c2, 5);
                assertTrue(result.success);
                assertTrue(result.hasWarning);
                return;
            }
        }
        fail("Could not find two editable cells in one row to construct a conflict scenario.");
    }

    @Test
    public void testClearCell() {
        SudokuModel model = new SudokuModel();
        model.loadPuzzle(0);

        // Find the first editable cell, set a value, then clear it.
        boolean foundEmpty = false;
        for (int row = 0; row < 9 && !foundEmpty; row++) {
            for (int col = 0; col < 9 && !foundEmpty; col++) {
                if (!model.isCellLocked(row, col)) {
                    model.setValue(row, col, 5);
                    SudokuModel.MoveResult result = model.clearCell(row, col);
                    assertTrue(result.success);
                    assertEquals(0, model.getCellValue(row, col));
                    foundEmpty = true;
                }
            }
        }
    }

    @Test
    public void testCannotClearLockedCell() {
        SudokuModel model = new SudokuModel();
        model.loadPuzzle(0);

        // Find a locked cell and verify clear is rejected.
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (model.isCellLocked(row, col)) {
                    SudokuModel.MoveResult result = model.clearCell(row, col);
                    assertFalse(result.success);
                    assertTrue(result.message.toLowerCase().contains("locked"));
                    return;
                }
            }
        }
    }

    @Test
    public void testUndo() {
        SudokuModel model = new SudokuModel();
        model.loadPuzzle(0);

        // Find the first editable cell and place a value.
        int targetRow = -1, targetCol = -1;
        for (int row = 0; row < 9 && targetRow == -1; row++) {
            for (int col = 0; col < 9 && targetRow == -1; col++) {
                if (!model.isCellLocked(row, col)) {
                    targetRow = row;
                    targetCol = col;
                    model.setValue(row, col, 5);
                }
            }
        }

        // Undo and verify the cell is empty again.
        SudokuModel.UndoResult result = model.undo();
        assertTrue(result.success);
        assertEquals(0, model.getCellValue(targetRow, targetCol));
    }

    @Test
    public void testUndoWithEmptyStack() {
        SudokuModel model = new SudokuModel();
        model.loadPuzzle(0);

        SudokuModel.UndoResult result = model.undo();
        assertFalse(result.success);
        assertTrue(result.message.toLowerCase().contains("undo"));
    }

    @Test
    public void testHint() {
        SudokuModel model = new SudokuModel();
        model.loadPuzzle(0);

        SudokuModel.HintResult result = model.getHint();
        assertTrue(result.success);
        assertTrue(result.row >= 0 && result.row < 9);
        assertTrue(result.col >= 0 && result.col < 9);
        assertTrue(result.value >= 1 && result.value <= 9);
    }

    @Test
    public void testHintDisabled() {
        SudokuModel model = new SudokuModel();
        model.loadPuzzle(0);
        model.setHintEnabled(false);

        SudokuModel.HintResult result = model.getHint();
        assertFalse(result.success);
        assertTrue(result.message.toLowerCase().contains("disabled"));
    }

    @Test
    public void testGetCandidateNumbers() {
        SudokuModel model = new SudokuModel();
        model.loadPuzzle(0);

        // Find the first editable cell and request candidates.
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (!model.isCellLocked(row, col)) {
                    int[] candidates = model.getCandidateNumbers(row, col);
                    assertTrue(candidates.length > 0);
                    assertTrue(candidates.length <= 9);
                    return;
                }
            }
        }
    }

    @Test
    public void testInferDifficulty() {
        SudokuModel model = new SudokuModel();
        model.loadPuzzle(0);

        SudokuModel.Difficulty difficulty = model.inferDifficulty();
        assertNotNull(difficulty);
        assertTrue(difficulty == SudokuModel.Difficulty.EASY ||
                   difficulty == SudokuModel.Difficulty.MEDIUM ||
                   difficulty == SudokuModel.Difficulty.HARD ||
                   difficulty == SudokuModel.Difficulty.EXPERT);
    }

    @Test
    public void testGetEmptyCellCount() {
        SudokuModel model = new SudokuModel();
        model.loadPuzzle(0);

        int emptyCount = model.getEmptyCellCount();
        assertTrue(emptyCount > 0);
        assertTrue(emptyCount < 81);
    }

    @Test
    public void testResetGame() {
        SudokuModel model = new SudokuModel();
        model.loadPuzzle(0);
        int initialEmpty = model.getEmptyCellCount();

        boolean placed = false;
        for (int row = 0; row < 9 && !placed; row++) {
            for (int col = 0; col < 9 && !placed; col++) {
                if (!model.isCellLocked(row, col)) {
                    model.setValue(row, col, 1);
                    placed = true;
                }
            }
        }

        model.resetGame();
        assertEquals(initialEmpty, model.getEmptyCellCount());
    }

    @Test
    public void testFlags() {
        SudokuModel model = new SudokuModel();

        // Validation feedback flag
        model.setValidationFeedbackEnabled(false);
        assertFalse(model.isValidationFeedbackEnabled());
        model.setValidationFeedbackEnabled(true);
        assertTrue(model.isValidationFeedbackEnabled());

        // Hint flag
        model.setHintEnabled(false);
        assertFalse(model.isHintEnabled());
        model.setHintEnabled(true);
        assertTrue(model.isHintEnabled());

        // Puzzle selection flag
        model.setPuzzleSelectionRandom(true);
        assertTrue(model.isPuzzleSelectionRandom());
        model.setPuzzleSelectionRandom(false);
        assertFalse(model.isPuzzleSelectionRandom());
    }

    @Test
    public void testNewGameWithRandomSelection() {
        SudokuModel model = new SudokuModel();
        model.setPuzzleSelectionRandom(true);

        // Start a new game; for random selection we only assert it does not crash.
        model.newGame();
        assertTrue(model.isGameStarted());
    }

    @Test
    public void testGetFormattedTime() {
        SudokuModel model = new SudokuModel();
        String time = model.getFormattedTime();
        assertNotNull(time);
        // Format should be MM:SS
        assertTrue(time.matches("\\d{2}:\\d{2}"));
    }

    @Test
    public void testAssertInvariants() {
        SudokuModel model = new SudokuModel();
        // Should not throw
        model.assertInvariants();
    }
}
