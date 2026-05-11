package com.sudoku;



import com.sudoku.controller.SudokuController;
import com.sudoku.model.SudokuModel;
import com.sudoku.model.SudokuModelInterface;
import com.sudoku.view.SudokuGUIView;

import javax.swing.*;

/**
 * GUI entry point.
 *
 * <p>NFR1: GUI and CLI must be separate programs, each with its own main method.</p>
 */
public class SudokuGUI {
    /**
     * Program entry point - starts the GUI version.
     */
    public static void main(String[] args) {
        SudokuModelInterface model = new SudokuModel();

        // Create Swing components on the EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            SudokuGUIView view = new SudokuGUIView(model);

            new SudokuController(model, view);

            view.showWindow();
        });
    }
}
