package com.sudoku;

import com.sudoku.model.SudokuModel;
import com.sudoku.model.SudokuModelInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Command-line version entry point (NFR1, NFR3).
 *
 * <p>The CLI handles user interaction in this class and only invokes the Model interface.
 * It must not access other model internals such as {@code Cell} or {@code SudokuBoard}.</p>
 */
public class SudokuCLI {

    public static void main(String[] args) {
        System.out.println("+===========================================+");
        System.out.println("|                                           |");
        System.out.println("|       Sudoku CLI - Terminal Edition       |");
        System.out.println("|                                           |");
        System.out.println("+===========================================+");
        System.out.println();

        new CliSession(new SudokuModel()).run();
    }

    private static final class CliSession {
        private static final String CLEAR_SCREEN = "\033[H\033[2J";
        private static final String RESET_CURSOR = "\033[H";
        private static final String HIGHLIGHT = "\033[1;96m";
        private static final String ERROR = "\033[1;91m";
        private static final String SUCCESS = "\033[1;92m";
        private static final String BOLD = "\033[1m";
        private static final String RESET = "\033[0m";

        private final SudokuModelInterface model;
        private int selectedRow = 0;
        private int selectedCol = 0;
        private boolean running;

        CliSession(SudokuModelInterface model) {
            this.model = model;
        }

        void run() {
            running = true;
            while (running) {
                showMenu();
                String input = readLine("Enter menu choice [1-4]: ");
                if (input == null) {
                    break;
                }
                switch (input.trim()) {
                    case "1":
                        startNewGame();
                        break;
                    case "2":
                        selectPuzzle();
                        break;
                    case "3":
                        showRules();
                        waitForEnter();
                        break;
                    case "4":
                        running = false;
                        showMessage("See you next time - bye!");
                        break;
                    default:
                        showError("That option is not valid. Try a digit between 1 and 4.");
                        waitForEnter();
                }
            }
        }

        private void startNewGame() {
            model.newGame();
            gameLoop();
        }

        private void selectPuzzle() {
            showPuzzleSelection();
            String input = readLine("Pick puzzle number [1-" + model.getPuzzleCount() + "]: ");
            try {
                int index = Integer.parseInt(input.trim()) - 1;
                if (index >= 0 && index < model.getPuzzleCount()) {
                    model.setCurrentPuzzleIndex(index);
                    showMessage("Loaded puzzle #" + (index + 1));
                } else {
                    showError("No puzzle with that index.");
                }
            } catch (NumberFormatException e) {
                showError("That is not a valid integer.");
            }
            waitForEnter();
        }

        private void gameLoop() {
            displayBoard();
            while (running && !model.isGameWon()) {
                String input = readLine("\n>> ");
                if (input == null) {
                    running = false;
                    break;
                }
                input = input.trim().toLowerCase();
                if (input.length() == 1 && Character.isDigit(input.charAt(0))) {
                    int num = Character.getNumericValue(input.charAt(0));
                    if (num >= 0 && num <= 9) {
                        handleNumberInput(num);
                    }
                } else if (input.length() > 0) {
                    handleCommand(input);
                }
                displayBoard();
            }
            if (model.isGameWon()) {
                showWinMessage();
                waitForEnter();
            }
        }

        private void handleNumberInput(int number) {
            int row = selectedRow;
            int col = selectedCol;
            if (row < 0 || col < 0) {
                showError("Move the cursor to a cell before entering a digit.");
                return;
            }
            if (number == 0) {
                SudokuModel.MoveResult result = model.clearCell(row, col);
                if (result.success) {
                    showMessage("Cell cleared -> (" + (row + 1) + "," + (col + 1) + ")");
                } else {
                    showError(result.message);
                }
            } else {
                SudokuModel.MoveResult result = model.setValue(row, col, number);
                if (result.success) {
                    if (result.hasWarning && model.isValidationFeedbackEnabled()) {
                        showError(result.message);
                    } else {
                        showMessage("Wrote " + number + " @ (" + (row + 1) + "," + (col + 1) + ")");
                    }
                } else {
                    showError(result.message);
                }
            }
        }

        private void handleCommand(String command) {
            switch (command) {
                case "n": {
                    String choice = readLine("Begin a fresh round? [y/n]: ");
                    if (choice != null && choice.trim().toLowerCase().equals("y")) {
                        startNewGame();
                    }
                    break;
                }
                case "r": {
                    String confirm = readLine("Discard progress and reset? [y/n]: ");
                    if (confirm != null && confirm.trim().toLowerCase().equals("y")) {
                        model.resetGame();
                        showMessage("Board restored to the starting layout.");
                    }
                    break;
                }
                case "q": {
                    String exitConfirm = readLine("Leave the game? [y/n]: ");
                    if (exitConfirm != null && exitConfirm.trim().toLowerCase().equals("y")) {
                        running = false;
                    }
                    break;
                }
                case "w":
                    selectedRow = Math.max(0, selectedRow - 1);
                    break;
                case "s":
                    selectedRow = Math.min(8, selectedRow + 1);
                    break;
                case "a":
                    selectedCol = Math.max(0, selectedCol - 1);
                    break;
                case "d":
                    selectedCol = Math.min(8, selectedCol + 1);
                    break;
                case "h":
                case "?":
                    showHelp();
                    waitForEnter();
                    break;
                case "hint":
                    showHint();
                    break;
                case "undo":
                    showUndo();
                    break;
                default:
                    showError("Unrecognized input - press H or ? for the command list.");
            }
        }

        private void showHint() {
            SudokuModel.HintResult result = model.getHint();
            if (!result.success) {
                showError(result.message);
                return;
            }
            SudokuModel.MoveResult move = model.setValue(result.row, result.col, result.value);
            if (move.success) {
                // Hint targets the first empty cell (row-major), not necessarily the cursor; move selection there like the GUI.
                selectedRow = result.row;
                selectedCol = result.col;
                showMessage(result.message);
            } else {
                showError(move.message);
            }
        }

        private void showUndo() {
            SudokuModel.UndoResult result = model.undo();
            if (result.success) {
                showMessage(result.message);
            } else {
                showError(result.message);
            }
        }

        private void showMenu() {
            clearScreen();
            System.out.println();
            System.out.println(BOLD + "+---------------------------------------+" + RESET);
            System.out.println(BOLD + "|     " + HIGHLIGHT + " * MAIN MENU *   " + RESET + BOLD + "           |" + RESET);
            System.out.println(BOLD + "+---------------------------------------+" + RESET);
            System.out.println(BOLD + "|                                       |" + RESET);
            System.out.println(BOLD + "|   1: New round                        |" + RESET);
            System.out.println(BOLD + "|   2: Choose puzzle                    |" + RESET);
            System.out.println(BOLD + "|   3: How to play                      |" + RESET);
            System.out.println(BOLD + "|   4: Quit                             |" + RESET);
            System.out.println(BOLD + "|                                       |" + RESET);
            System.out.println(BOLD + "+---------------------------------------+" + RESET);
            System.out.println();
        }

        private void showPuzzleSelection() {
            clearScreen();
            System.out.println();
            System.out.println(BOLD + "+---------------------------------------+" + RESET);
            System.out.println(BOLD + "|      " + HIGHLIGHT + " Puzzle library " + RESET + BOLD + "             |" + RESET);
            System.out.println(BOLD + "+---------------------------------------+" + RESET);
            int count = model.getPuzzleCount();
            String[] difficulties = {"Easy", "Medium", "Hard", "Expert", "Easy"};
            for (int i = 0; i < count; i++) {
                String marker = (i == model.getCurrentPuzzleIndex()) ? HIGHLIGHT + "> " + RESET : "  ";
                String difficulty = difficulties[i % 5];
                System.out.println(BOLD + "|   " + marker + (i + 1) + ". " + difficulty + padRight("", 28 - difficulty.length()) + "|" + RESET);
            }
            System.out.println(BOLD + "|                                       |" + RESET);
            System.out.println(BOLD + "+---------------------------------------+" + RESET);
            System.out.println();
        }

        private void displayBoard() {
            clearScreen();
            System.out.println();
            System.out.println(BOLD + "#########################################" + RESET);
            System.out.println(BOLD + "#  Grid view: 9x9                      #" + RESET);
            System.out.println(BOLD + "#########################################" + RESET);
            System.out.println(BOLD + "#  Clock " + BOLD + model.getFormattedTime() + padRight("", 31) + "#" + RESET);
            // The model is started after loading a puzzle; inferDifficulty is safe only then.
            if (model.getEmptyCellCount() >= 0) {
                SudokuModel.Difficulty difficulty = model.inferDifficulty();
                int emptyCells = model.getEmptyCellCount();
                String diffInfo = "  Level: " + difficulty.getDisplayName() + ", blanks:" + emptyCells;
                System.out.println(BOLD + "#" + diffInfo + padRight("", 39 - diffInfo.length()) + "#" + RESET);
            }
            System.out.println(BOLD + "#########################################" + RESET);
            System.out.println();
            System.out.print("      ");
            for (int j = 0; j < 9; j++) {
                System.out.print(" " + (j + 1) + " ");
                if (j == 2 || j == 5) {
                    System.out.print(" ");
                }
            }
            System.out.println();
            System.out.print("    +");
            for (int j = 0; j < 9; j++) {
                if (j > 0 && j % 3 == 0) {
                    System.out.print("+");
                }
                System.out.print("---+");
            }
            System.out.println();
            for (int i = 0; i < 9; i++) {
                System.out.print(BOLD + "  " + (i + 1) + " |" + RESET);
                for (int j = 0; j < 9; j++) {
                    String cellStr;
                    if (i == selectedRow && j == selectedCol) {
                        int v = model.getCellValue(i, j);
                        cellStr = HIGHLIGHT + "[" + (v == 0 ? " " : v) + "]" + RESET;
                    } else if (model.isCellLocked(i, j)) {
                        int v = model.getCellValue(i, j);
                        cellStr = "\033[38;5;141m" + (v == 0 ? " " : v) + "\033[0m";
                    } else {
                        int v = model.getCellValue(i, j);
                        cellStr = (v == 0) ? " " : String.valueOf(v);
                    }
                    String verticalBar = (j % 3 == 2) ? "|" : " ";
                    System.out.print(" " + cellStr + verticalBar);
                    if (j == 2 || j == 5) {
                        System.out.print("|");
                    }
                }
                System.out.println();
                if (i == 2 || i == 5 || i == 8) {
                    System.out.print("    +");
                    for (int j = 0; j < 9; j++) {
                        String separator = (j % 3 == 0) ? "+" : "|";
                        System.out.print("---+" + separator);
                    }
                    System.out.println();
                }
            }
            System.out.println();
            showHelp();
        }

        private void showHelp() {
            System.out.println(BOLD + " -----------------------------------------" + RESET);
            System.out.println(BOLD + "  Keys  WASD: cursor   1-9: fill   0: wipe" + RESET);
            System.out.println(BOLD + "        H ?: cheatsheet   hint   undo       " + RESET);
            System.out.println(BOLD + "        N: new   R: rewind board   Q: exit   " + RESET);
            System.out.println(BOLD + " -----------------------------------------" + RESET);
        }

        private void showWinMessage() {
            System.out.println();
            System.out.println(SUCCESS + "*******************************************************" + RESET);
            System.out.println(SUCCESS + "   Puzzle solved - nice work!" + RESET);
            System.out.println(SUCCESS + "   Elapsed: " + model.getFormattedTime() + RESET);
            System.out.println(SUCCESS + "*******************************************************" + RESET);
            System.out.println();
        }

        private void showRules() {
            clearScreen();
            System.out.println();
            System.out.println(BOLD + "+-------------------------------------------+" + RESET);
            System.out.println(BOLD + "|  Brief rules                              |" + RESET);
            System.out.println(BOLD + "+-------------------------------------------+" + RESET);
            System.out.println(BOLD + "|  Fill every cell with 1 through 9.        |" + RESET);
            System.out.println(BOLD + "|  Each row, column, and 3x3 block has      |" + RESET);
            System.out.println(BOLD + "|  no repeated digit.                       |" + RESET);
            System.out.println(BOLD + "+-------------------------------------------+" + RESET);
            System.out.println();
        }

        private void showError(String message) {
            System.out.println(ERROR + "[!] " + message + RESET);
        }

        private void showMessage(String message) {
            System.out.println(BOLD + "[OK] " + message + RESET);
        }

        private void clearScreen() {
            System.out.print(CLEAR_SCREEN);
            System.out.print(RESET_CURSOR);
        }

        private String padRight(String s, int n) {
            return String.format("%-" + n + "s", s);
        }

        private String readLine(String prompt) {
            System.out.print(prompt);
            try {
                return new BufferedReader(new InputStreamReader(System.in)).readLine();
            } catch (IOException e) {
                return null;
            }
        }

        private void waitForEnter() {
            System.out.print("\n[Enter] continue...");
            try {
                System.in.read();
                while (System.in.available() > 0) {
                    System.in.read();
                }
            } catch (IOException ignored) {
            }
        }
    }
}
