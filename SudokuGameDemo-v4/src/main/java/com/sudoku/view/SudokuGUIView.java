package com.sudoku.view;



import com.sudoku.controller.SudokuController;
import com.sudoku.model.SudokuModel;
import com.sudoku.model.SudokuModelInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * SudokuGUIView - GUI view (View layer).
 *
 * <p>Provides the Swing UI. Contains no game logic.</p>
 */
@SuppressWarnings("deprecation")
public class SudokuGUIView extends JFrame implements Observer {
    // Board cell size (pixels)
    private static final int CELL_SIZE = 60;
    // Board size
    private static final int BOARD_SIZE = 9;
    // 3x3 box size
    private static final int BOX_SIZE = 3;

    // Colors (cool slate / amber accent)
    private static final Color BACKGROUND_COLOR = new Color(236, 241, 248);
    private static final Color LOCKED_COLOR = new Color(38, 50, 66);
    private static final Color EMPTY_COLOR = new Color(255, 255, 255);
    private static final Color SELECTED_COLOR = new Color(255, 193, 102);
    private static final Color HIGHLIGHT_COLOR = new Color(214, 228, 247);
    private static final Color ERROR_COLOR = new Color(239, 108, 92);
    private static final Color SAME_NUMBER_COLOR = new Color(215, 237, 212);
    private static final Color BORDER_COLOR = new Color(54, 69, 91);

    private final SudokuModelInterface model;
    // Controller reference
    private SudokuController controller;

    // UI components
    private JPanel boardPanel;
    private JPanel[][] cellPanels;
    private JLabel[][] cellLabels;
    private JLabel timerLabel;
    private JLabel statusLabel;
    private JComboBox<String> puzzleSelector;
    private JButton newGameButton;
    private JButton resetButton;
    private JCheckBox validationFeedbackCheckBox;
    private JCheckBox hintCheckBox;
    private JCheckBox randomSelectCheckBox;

    /** User-driven puzzle selection only; removed while rebuilding the combo to avoid spurious index 0. */
    private ActionListener puzzleSelectionListener;

    // Selected cell
    private int selectedRow = -1;
    private int selectedCol = -1;
    // Difficulty label
    private JLabel difficultyLabel;

    /**
     * Creates the GUI window.
     */
    public SudokuGUIView(SudokuModelInterface model) {
        this.model = model;
        model.addObserver(this);
        initializeUI();
    }

    /**
     * NFR2: refresh the View when the Observable Model changes.
     */
    @Override
    public void update(Observable o, Object arg) {
        SwingUtilities.invokeLater(() -> {
            updateBoard();
            updateInvalidCells();
            updateTimer();
            updateDifficulty();
        });
    }

    /** Sets the controller reference. */
    public void setController(SudokuController controller) {
        this.controller = controller;
    }

    /** Returns the controller reference. */
    private SudokuController getController() {
        return controller;
    }

    /** Initializes Swing UI components. */
    private void initializeUI() {
        setTitle("Sudoku Desk");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(BACKGROUND_COLOR);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(BACKGROUND_COLOR);

        // Top panel (title, status, timer)
        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Board panel
        boardPanel = createBoardPanel();
        mainPanel.add(boardPanel, BorderLayout.CENTER);

        // Bottom panel (number pad and controls)
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Window size
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Creates the top panel (title, status, timer).
     */
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_COLOR);

        JLabel titleLabel = new JLabel("Sudoku", JLabel.CENTER);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        titleLabel.setForeground(new Color(45, 62, 88));

        // Timer label
        timerLabel = new JLabel("00:00", JLabel.CENTER);
        timerLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
        timerLabel.setForeground(new Color(56, 108, 176));

        statusLabel = new JLabel("Pick a board from the list, then play.", JLabel.CENTER);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        statusLabel.setForeground(new Color(88, 96, 108));

        // Difficulty label
        difficultyLabel = new JLabel("", JLabel.CENTER);
        difficultyLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 13));
        difficultyLabel.setForeground(new Color(46, 125, 95));

        // Left: title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setBackground(BACKGROUND_COLOR);
        leftPanel.add(titleLabel);

        // Center: status and difficulty
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerPanel.setBackground(BACKGROUND_COLOR);
        centerPanel.add(statusLabel);
        centerPanel.add(Box.createHorizontalStrut(20));
        centerPanel.add(difficultyLabel);

        // Right: timer
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(BACKGROUND_COLOR);
        rightPanel.add(timerLabel);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * Creates the board panel.
     */
    private JPanel createBoardPanel() {
        JPanel panel = new JPanel(new GridLayout(BOARD_SIZE, BOARD_SIZE, 1, 1));
        panel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 2));
        panel.setBackground(BORDER_COLOR);

        // Cell components
        cellPanels = new JPanel[BOARD_SIZE][BOARD_SIZE];
        cellLabels = new JLabel[BOARD_SIZE][BOARD_SIZE];

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                // Cell panel
                JPanel cellPanel = new JPanel(new BorderLayout());
                cellPanel.setBackground(EMPTY_COLOR);
                cellPanel.setBorder(null);
                cellPanel.setPreferredSize(new Dimension(CELL_SIZE, CELL_SIZE));
                cellPanels[row][col] = cellPanel;

                // Digit label
                JLabel label = new JLabel(" ", JLabel.CENTER);
                label.setFont(new Font(Font.DIALOG, Font.BOLD, 30));
                label.setForeground(LOCKED_COLOR);
                label.setOpaque(true);
                label.setBackground(EMPTY_COLOR);
                cellLabels[row][col] = label;
                cellPanel.add(label, BorderLayout.CENTER);

                // Click selection
                int r = row, c = col;
                cellPanel.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        selectCell(r, c);
                    }
                });
                label.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        selectCell(r, c);
                    }
                });

                // Borders for 3x3 box separation
                int top = (row % BOX_SIZE == 0) ? 3 : 1;
                int left = (col % BOX_SIZE == 0) ? 3 : 1;
                int bottom = (row == BOARD_SIZE - 1) ? 3 : 1;
                int right = (col == BOARD_SIZE - 1) ? 3 : 1;
                cellPanel.setBorder(BorderFactory.createMatteBorder(top, left, bottom, right, BORDER_COLOR));

                panel.add(cellPanel);
            }
        }

        return panel;
    }

    /**
     * Creates the bottom panel (number pad and controls).
     */
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BACKGROUND_COLOR);

        // Number pad
        JPanel numberPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        numberPanel.setBackground(BACKGROUND_COLOR);

        for (int num = 1; num <= 9; num++) {
            final int number = num;
            JButton button = new JButton(String.valueOf(num));
            button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            button.setPreferredSize(new Dimension(48, 48));
            button.setBackground(new Color(220, 232, 248));
            button.setFocusPainted(false);
            button.addActionListener(e -> {
                SudokuController ctrl = getController();
                if (ctrl != null) {
                    ctrl.inputNumber(number);
                }
            });
            numberPanel.add(button);
        }

        // Control buttons
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        controlPanel.setBackground(BACKGROUND_COLOR);

        newGameButton = new JButton("New round");
        newGameButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        newGameButton.setPreferredSize(new Dimension(90, 35));
        newGameButton.setFocusPainted(false);
        newGameButton.addActionListener(e -> {
            SudokuController ctrl = getController();
            if (ctrl != null) {
                ctrl.newGame();
            }
        });
        controlPanel.add(newGameButton);

        resetButton = new JButton("Restart");
        resetButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        resetButton.setPreferredSize(new Dimension(90, 35));
        resetButton.setFocusPainted(false);
        resetButton.addActionListener(e -> {
            SudokuController ctrl = getController();
            if (ctrl != null) {
                ctrl.resetGame();
            }
        });
        controlPanel.add(resetButton);

        JButton clearButton = new JButton("Clear cell");
        clearButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        clearButton.setPreferredSize(new Dimension(90, 35));
        clearButton.setFocusPainted(false);
        clearButton.addActionListener(e -> {
            SudokuController ctrl = getController();
            if (ctrl != null) {
                ctrl.clearCell();
            }
        });
        controlPanel.add(clearButton);

        JButton undoButton = new JButton("Step back");
        undoButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        undoButton.setPreferredSize(new Dimension(90, 35));
        undoButton.setFocusPainted(false);
        undoButton.addActionListener(e -> {
            SudokuController ctrl = getController();
            if (ctrl != null) {
                ctrl.undo();
            }
        });
        controlPanel.add(undoButton);

        JButton hintButton = new JButton("Clue");
        hintButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        hintButton.setPreferredSize(new Dimension(90, 35));
        hintButton.setFocusPainted(false);
        hintButton.addActionListener(e -> {
            SudokuController ctrl = getController();
            if (ctrl != null) {
                ctrl.hint();
            }
        });
        controlPanel.add(hintButton);

        // Puzzle selector
        puzzleSelector = new JComboBox<>();
        puzzleSelector.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        puzzleSelector.setPreferredSize(new Dimension(150, 35));
        puzzleSelectionListener = e -> {
            SudokuController ctrl = getController();
            if (ctrl != null) {
                int index = puzzleSelector.getSelectedIndex();
                if (index >= 0) {
                    ctrl.selectPuzzle(index);
                }
            }
        };
        puzzleSelector.addActionListener(puzzleSelectionListener);
        controlPanel.add(puzzleSelector);

        // Flags (FR3)
        JPanel flagsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        flagsPanel.setBackground(BACKGROUND_COLOR);

        validationFeedbackCheckBox = new JCheckBox("Highlight conflicts");
        validationFeedbackCheckBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        validationFeedbackCheckBox.setBackground(BACKGROUND_COLOR);
        validationFeedbackCheckBox.setSelected(true);
        validationFeedbackCheckBox.addActionListener(e -> {
            SudokuController ctrl = getController();
            if (ctrl != null) {
                ctrl.setValidationFeedback(validationFeedbackCheckBox.isSelected());
            }
        });
        flagsPanel.add(validationFeedbackCheckBox);

        hintCheckBox = new JCheckBox("Allow clues");
        hintCheckBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        hintCheckBox.setBackground(BACKGROUND_COLOR);
        hintCheckBox.setSelected(true);
        hintCheckBox.addActionListener(e -> {
            SudokuController ctrl = getController();
            if (ctrl != null) {
                ctrl.setHintEnabled(hintCheckBox.isSelected());
            }
        });
        flagsPanel.add(hintCheckBox);

        randomSelectCheckBox = new JCheckBox("Random board");
        randomSelectCheckBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        randomSelectCheckBox.setBackground(BACKGROUND_COLOR);
        randomSelectCheckBox.setSelected(false);
        randomSelectCheckBox.addActionListener(e -> {
            SudokuController ctrl = getController();
            if (ctrl != null) {
                ctrl.setPuzzleSelectionRandom(randomSelectCheckBox.isSelected());
            }
        });
        flagsPanel.add(randomSelectCheckBox);

        panel.add(numberPanel, BorderLayout.NORTH);
        panel.add(controlPanel, BorderLayout.CENTER);
        panel.add(flagsPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Selects a cell.
     */
    public void selectCell(int row, int col) {
        // Do not allow selecting locked cells
        if (model.isCellLocked(row, col)) {
            return;
        }

        // Update selection
        int oldRow = selectedRow;
        int oldCol = selectedCol;
        selectedRow = row;
        selectedCol = col;

        // Update cell display
        if (oldRow >= 0 && oldCol >= 0) {
            updateCellDisplay(oldRow, oldCol);
        }
        updateCellDisplay(row, col);

        // Update highlighting
        updateHighlights(row, col);
    }

    /**
     * Updates highlights based on selection and validity.
     */
    private void updateHighlights(int row, int col) {
        int selectedValue = model.getCellValue(row, col);

        // Invalid cells list
        List<int[]> invalidCells = new ArrayList<>();
        if (model.isValidationFeedbackEnabled()) {
            invalidCells = model.getInvalidCells();
        }

        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                Color bgColor = EMPTY_COLOR;

                // Invalid cell?
                boolean isInvalid = false;
                for (int[] pos : invalidCells) {
                    if (pos[0] == r && pos[1] == c && !model.isCellLocked(r, c)) {
                        isInvalid = true;
                        break;
                    }
                }

                if (isInvalid) {
                    bgColor = ERROR_COLOR;
                }
                // Selected cell
                else if (r == row && c == col) {
                    bgColor = SELECTED_COLOR;
                }
                // Same row/column
                else if (r == row || c == col) {
                    bgColor = HIGHLIGHT_COLOR;
                }
                // Same 3x3 box
                else if ((r / BOX_SIZE == row / BOX_SIZE) && (c / BOX_SIZE == col / BOX_SIZE)) {
                    bgColor = HIGHLIGHT_COLOR;
                }
                // Same digit
                else if (selectedValue != 0 && model.getCellValue(r, c) == selectedValue) {
                    bgColor = SAME_NUMBER_COLOR;
                }

                cellLabels[r][c].setBackground(bgColor);
                cellPanels[r][c].setBackground(bgColor);
            }
        }
    }

    /**
     * Updates the full board display.
     */
    public void updateBoard() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                updateCellDisplay(row, col);
            }
        }

        // Re-apply highlight if a cell is selected
        if (selectedRow >= 0 && selectedCol >= 0) {
            updateHighlights(selectedRow, selectedCol);
        }
    }

    /**
     * Updates a single cell display.
     */
    private void updateCellDisplay(int row, int col) {
        JLabel label = cellLabels[row][col];

        int value = model.getCellValue(row, col);
        boolean locked = model.isCellLocked(row, col);

        if (value == 0) {
            label.setText(" ");
        } else {
            label.setText(String.valueOf(value));
        }

        // Font color
        if (locked) {
            label.setForeground(LOCKED_COLOR);
            label.setFont(new Font(Font.DIALOG, Font.BOLD, 30));
        } else {
            label.setForeground(new Color(21, 101, 192));
            label.setFont(new Font(Font.DIALOG, Font.PLAIN, 30));
        }
    }

    /**
     * Highlights invalid cells when validation feedback is enabled (FR2).
     */
    public void updateInvalidCells() {
        // Clear error highlighting
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                cellLabels[row][col].setBackground(EMPTY_COLOR);
                cellPanels[row][col].setBackground(EMPTY_COLOR);
            }
        }

        // Highlight invalid cells when validation feedback is enabled
        if (model.isValidationFeedbackEnabled()) {
            List<int[]> invalidCells = model.getInvalidCells();
            for (int[] pos : invalidCells) {
                int row = pos[0];
                int col = pos[1];
                // Only highlight editable (non-locked) cells
                if (!model.isCellLocked(row, col)) {
                    cellLabels[row][col].setBackground(ERROR_COLOR);
                    cellPanels[row][col].setBackground(ERROR_COLOR);
                }
            }
        }

        // Re-apply selection highlight
        if (selectedRow >= 0 && selectedCol >= 0) {
            updateHighlights(selectedRow, selectedCol);
        }
    }

    /**
     * Updates the timer display.
     */
    public void updateTimer() {
        timerLabel.setText(model.getFormattedTime());
    }

    /**
     * Updates the status line.
     */
    public void updateStatus(String status) {
        statusLabel.setText(status);
    }

    /**
     * Updates the difficulty label.
     */
    public void updateDifficulty() {
        // Difficulty is based on empty cell count; it is safe after a puzzle is loaded.
        if (model.getPuzzleCount() > 0) {
            SudokuModel.Difficulty difficulty = model.inferDifficulty();
            int emptyCells = model.getEmptyCellCount();
            difficultyLabel.setText("Level: " + difficulty.getDisplayName() + ", open cells: " + emptyCells);
        } else {
            difficultyLabel.setText("");
        }
    }

    /**
     * Shows a win dialog (FR1).
     */
    public void showWinDialog() {
        String time = model.getFormattedTime();
        JOptionPane.showMessageDialog(this,
            "Board complete.\nElapsed time: " + time,
            "Finished",
            JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Returns selected row.
     */
    public int getSelectedRow() {
        return selectedRow;
    }

    /**
     * Returns selected column.
     */
    public int getSelectedCol() {
        return selectedCol;
    }

    /**
     * Sets selected row.
     */
    public void setSelectedRow(int row) {
        if (row >= 0 && row < BOARD_SIZE) {
            selectCell(row, selectedCol >= 0 ? selectedCol : 0);
        }
    }

    /**
     * Sets selected column.
     */
    public void setSelectedCol(int col) {
        if (col >= 0 && col < BOARD_SIZE) {
            selectCell(selectedRow >= 0 ? selectedRow : 0, col);
        }
    }

    /**
     * Moves selection up.
     */
    public void moveSelectionUp() {
        if (selectedRow > 0) {
            // Find next non-locked cell
            for (int r = selectedRow - 1; r >= 0; r--) {
                if (!model.isCellLocked(r, selectedCol)) {
                    selectCell(r, selectedCol);
                    return;
                }
            }
            // Wrap search
            for (int r = 0; r < selectedRow; r++) {
                if (!model.isCellLocked(r, selectedCol)) {
                    selectCell(r, selectedCol);
                    return;
                }
            }
        }
    }

    /**
     * Moves selection down.
     */
    public void moveSelectionDown() {
        if (selectedRow < BOARD_SIZE - 1) {
            // Find next non-locked cell
            for (int r = selectedRow + 1; r < BOARD_SIZE; r++) {
                if (!model.isCellLocked(r, selectedCol)) {
                    selectCell(r, selectedCol);
                    return;
                }
            }
            // Wrap search
            for (int r = BOARD_SIZE - 1; r > selectedRow; r--) {
                if (!model.isCellLocked(r, selectedCol)) {
                    selectCell(r, selectedCol);
                    return;
                }
            }
        }
    }

    /**
     * Moves selection left.
     */
    public void moveSelectionLeft() {
        if (selectedCol > 0) {
            // Find next non-locked cell
            for (int c = selectedCol - 1; c >= 0; c--) {
                if (!model.isCellLocked(selectedRow, c)) {
                    selectCell(selectedRow, c);
                    return;
                }
            }
            // Wrap search
            for (int c = 0; c < selectedCol; c++) {
                if (!model.isCellLocked(selectedRow, c)) {
                    selectCell(selectedRow, c);
                    return;
                }
            }
        }
    }

    /**
     * Moves selection right.
     */
    public void moveSelectionRight() {
        if (selectedCol < BOARD_SIZE - 1) {
            // Find next non-locked cell
            for (int c = selectedCol + 1; c < BOARD_SIZE; c++) {
                if (!model.isCellLocked(selectedRow, c)) {
                    selectCell(selectedRow, c);
                    return;
                }
            }
            // Wrap search
            for (int c = BOARD_SIZE - 1; c > selectedCol; c--) {
                if (!model.isCellLocked(selectedRow, c)) {
                    selectCell(selectedRow, c);
                    return;
                }
            }
        }
    }

    /**
     * Updates the puzzle selector.
     */
    public void updatePuzzleSelector() {
        puzzleSelector.removeActionListener(puzzleSelectionListener);
        try {
            puzzleSelector.removeAllItems();
            for (int i = 0; i < model.getPuzzleCount(); i++) {
                puzzleSelector.addItem("Board #" + (i + 1));
            }
            puzzleSelector.setSelectedIndex(model.getCurrentPuzzleIndex());
        } finally {
            puzzleSelector.addActionListener(puzzleSelectionListener);
        }
    }

    /**
     * Returns the selected puzzle index.
     */
    public int getSelectedPuzzleIndex() {
        return puzzleSelector.getSelectedIndex();
    }

    /**
     * Shows an error dialog.
     */
    public void showError(String message) {
        JOptionPane.showMessageDialog(this,
            message,
            "Something went wrong",
            JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows reset confirmation dialog.
     */
    public boolean showConfirmReset() {
        return JOptionPane.showConfirmDialog(this,
                "Restart from the original clues? Unsaved moves disappear.",
                "Restart board",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    /**
     * Shows the window.
     */
    public void showWindow() {
        setVisible(true);
    }

    /**
     * Closes the window.
     */
    public void closeWindow() {
        dispose();
    }
}
