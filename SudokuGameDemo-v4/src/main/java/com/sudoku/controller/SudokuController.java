package com.sudoku.controller;

import com.sudoku.model.SudokuModel;
import com.sudoku.model.SudokuModelInterface;
import com.sudoku.view.SudokuGUIView;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * SudokuController - GUI controller (Controller layer).
 *
 * <p>Coordinates the Model and the View. Contains no GUI rendering code.</p>
 */
public class SudokuController implements ActionListener, KeyListener {
    private final SudokuModelInterface model;
    private final SudokuGUIView view;
    private Timer timer;
    private boolean gameActive = false;

    /** Creates a controller for the given Model and View. */
    public SudokuController(SudokuModelInterface model, SudokuGUIView view) {
        this.model = model;
        this.view = view;
        this.view.setController(this);

        // Keyboard listener
        view.addKeyListener(this);
        view.setFocusable(true);

        // Timer updates once per second
        timer = new Timer(1000, e -> {
            if (gameActive) {
                view.updateTimer();
            }
        });

        // Initialize view
        view.updatePuzzleSelector();
        view.updateBoard();
    }

    /** Starts a new game. */
    public void newGame() {
        int puzzleIndex = view.getSelectedPuzzleIndex();
        model.setCurrentPuzzleIndex(puzzleIndex);
        loadSelectedPuzzle();
    }

    /** Loads the currently selected puzzle. */
    private void loadSelectedPuzzle() {
        if (model.newGame()) {  // FR5: respects random selection flag
            view.updatePuzzleSelector();
            view.updateStatus("Game started.");
            gameActive = true;
            timer.start();
        } else {
            view.showError("Failed to load puzzle.");
        }
    }

    /** Resets the current game to its initial state. */
    public void resetGame() {
        if (!view.showConfirmReset()) {
            return;
        }
        model.resetGame();
        view.updateStatus("Game reset.");
    }

    /** Inputs a number into the currently selected cell (1-9). */
    public void inputNumber(int number) {
        if (!gameActive) {
            view.updateStatus("Start a new game first.");
            return;
        }

        int row = view.getSelectedRow();
        int col = view.getSelectedCol();

        if (row < 0 || col < 0) {
            view.updateStatus("Select a cell first.");
            return;
        }

        // Use the move API that returns a result object
        SudokuModel.MoveResult result = model.setValue(row, col, number);

        if (result.success) {
            view.updateStatus(result.message);

            // Check win
            if (model.isGameWon()) {
                gameActive = false;
                timer.stop();
                view.updateStatus("Congratulations! You win.");
                view.showWinDialog();
            } else if (model.isBoardComplete()) {
                view.updateStatus("Board is full, but there are mistakes.");
            }
        } else {
            view.updateStatus(result.message);
        }
    }

    /** Clears the currently selected cell. */
    public void clearCell() {
        if (!gameActive) {
            view.updateStatus("Start a new game first.");
            return;
        }

        int row = view.getSelectedRow();
        int col = view.getSelectedCol();

        if (row < 0 || col < 0) {
            view.updateStatus("Select a cell first.");
            return;
        }

        SudokuModel.MoveResult result = model.clearCell(row, col);

        if (result.success) {
            view.updateStatus(result.message);
        } else {
            view.updateStatus(result.message);
        }
    }

    /** Undoes the last action (FR5). */
    public void undo() {
        if (!gameActive) {
            view.updateStatus("Start a new game first.");
            return;
        }

        SudokuModel.UndoResult result = model.undo();

        if (result.success) {
            view.updateStatus(result.message);
        } else {
            view.updateStatus(result.message);
        }
    }

    /** Requests a hint (FR5). */
    public void hint() {
        if (!gameActive) {
            view.updateStatus("Start a new game first.");
            return;
        }

        SudokuModel.HintResult result = model.getHint();

        if (result.success) {
            // Select the hinted cell and fill it
            view.selectCell(result.row, result.col);
            model.setValue(result.row, result.col, result.value);
            view.updateStatus(result.message);

            // Check win
            if (model.isGameWon()) {
                gameActive = false;
                timer.stop();
                view.updateStatus("Congratulations! You win.");
                view.showWinDialog();
            }
        } else {
            view.updateStatus(result.message);
        }
    }

    /** Selects a puzzle index (does not auto-load). */
    public void selectPuzzle(int index) {
        model.setCurrentPuzzleIndex(index);
    }

    /** Toggles validation feedback (FR2/FR3). */
    public void setValidationFeedback(boolean enabled) {
        model.setValidationFeedbackEnabled(enabled);
        view.updateInvalidCells();
        view.updateStatus("Validation feedback: " + (enabled ? "ON" : "OFF"));
    }

    /** Toggles hint availability (FR3/FR5). */
    public void setHintEnabled(boolean enabled) {
        model.setHintEnabled(enabled);
        view.updateStatus("Hint: " + (enabled ? "ON" : "OFF"));
    }

    /** Toggles random puzzle selection (FR3/FR5). */
    public void setPuzzleSelectionRandom(boolean random) {
        model.setPuzzleSelectionRandom(random);
        view.updateStatus("Random selection: " + (random ? "ON" : "OFF"));
    }

    /** Returns whether validation feedback is enabled. */
    public boolean isValidationFeedbackEnabled() {
        return model.isValidationFeedbackEnabled();
    }

    /** Returns whether hints are enabled. */
    public boolean isHintEnabled() {
        return model.isHintEnabled();
    }

    /** Returns whether random selection is enabled. */
    public boolean isPuzzleSelectionRandom() {
        return model.isPuzzleSelectionRandom();
    }

    /** Handles keyboard input for the GUI. */
    @Override
    public void keyPressed(KeyEvent e) {
        if (!gameActive) {
            return;
        }

        int keyCode = e.getKeyCode();

        switch (keyCode) {
            // Navigation
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                view.moveSelectionUp();
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                view.moveSelectionDown();
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                view.moveSelectionLeft();
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                view.moveSelectionRight();
                break;

            // Digit input
            case KeyEvent.VK_1:
            case KeyEvent.VK_NUMPAD1:
                inputNumber(1);
                break;
            case KeyEvent.VK_2:
            case KeyEvent.VK_NUMPAD2:
                inputNumber(2);
                break;
            case KeyEvent.VK_3:
            case KeyEvent.VK_NUMPAD3:
                inputNumber(3);
                break;
            case KeyEvent.VK_4:
            case KeyEvent.VK_NUMPAD4:
                inputNumber(4);
                break;
            case KeyEvent.VK_5:
            case KeyEvent.VK_NUMPAD5:
                inputNumber(5);
                break;
            case KeyEvent.VK_6:
            case KeyEvent.VK_NUMPAD6:
                inputNumber(6);
                break;
            case KeyEvent.VK_7:
            case KeyEvent.VK_NUMPAD7:
                inputNumber(7);
                break;
            case KeyEvent.VK_8:
            case KeyEvent.VK_NUMPAD8:
                inputNumber(8);
                break;
            case KeyEvent.VK_9:
            case KeyEvent.VK_NUMPAD9:
                inputNumber(9);
                break;

            // Clear
            case KeyEvent.VK_0:
            case KeyEvent.VK_BACK_SPACE:
            case KeyEvent.VK_DELETE:
            case KeyEvent.VK_SPACE:
                clearCell();
                break;

            // Undo (FR5)
            case KeyEvent.VK_Z:
                if (e.isControlDown()) {
                    undo();
                }
                break;

            // New game
            case KeyEvent.VK_N:
                newGame();
                break;

            // Reset
            case KeyEvent.VK_R:
                resetGame();
                break;

            // Hint (FR5)
            case KeyEvent.VK_H:
            case KeyEvent.VK_SLASH:
                hint();
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Not used
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Button events are handled by their own listeners
    }

    /** Stops the timer. */
    public void stopTimer() {
        timer.stop();
    }

    /**
     * Starts the timer (if game is active).
     */
    public void startTimer() {
        if (gameActive) {
            timer.start();
        }
    }
}
