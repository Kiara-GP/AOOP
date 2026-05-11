package com.sudoku.model;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Cell}.
 */
public class CellTest {

    @Test
    public void testCreateEmptyCell() {
        Cell cell = new Cell(0, false);
        assertEquals(0, cell.getValue());
        assertTrue(cell.isEmpty());
        assertFalse(cell.isLocked());
    }

    @Test
    public void testCreateCellWithValue() {
        Cell cell = new Cell(5, false);
        assertEquals(5, cell.getValue());
        assertFalse(cell.isEmpty());
        assertFalse(cell.isLocked());
    }

    @Test
    public void testCreateLockedCell() {
        Cell cell = new Cell(3, true);
        assertEquals(3, cell.getValue());
        assertFalse(cell.isEmpty());
        assertTrue(cell.isLocked());
    }

    @Test
    public void testSetValueOnUnlockedCell() {
        Cell cell = new Cell(0, false);
        cell.setValue(7);
        assertEquals(7, cell.getValue());
        assertFalse(cell.isEmpty());
    }

    @Test
    public void testCannotModifyLockedCell() {
        Cell cell = new Cell(5, true);
        cell.setValue(9);
        assertEquals(5, cell.getValue());
    }

    @Test
    public void testClearUnlockedCell() {
        Cell cell = new Cell(5, false);
        cell.clear();
        assertEquals(0, cell.getValue());
        assertTrue(cell.isEmpty());
    }

    @Test
    public void testCannotClearLockedCell() {
        Cell cell = new Cell(5, true);
        cell.clear();
        assertEquals(5, cell.getValue());
    }

    @Test
    public void testToString() {
        Cell emptyCell = new Cell(0, false);
        Cell filledCell = new Cell(5, false);

        assertEquals(" ", emptyCell.toString());
        assertEquals("5", filledCell.toString());
    }
}
