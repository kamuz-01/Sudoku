package org.JogoSudoku;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SudokuGame {
	public static final int SIZE = 9;
	private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+");
	private static final Pattern FRIENDLY_CELL_PATTERN = Pattern.compile(
		"(?i)(?:--?puzzle=|--?givens=|--?initial=)?(?:r|row|linha)\\s*(\\d{1,2})\\s*(?:c|col|coluna)\\s*(\\d{1,2})\\s*(?:=|:|->)\\s*(\\d)"
	);
	private static final Pattern FRIENDLY_PAIR_PATTERN = Pattern.compile(
		"(?i)(?:--?puzzle=|--?givens=|--?initial=)?(\\d{1,2})\\s*[,;/x]\\s*(\\d{1,2})\\s*(?:=|:|->)\\s*(\\d)"
	);

	public enum GameStatus {
		NOT_STARTED,
		INCOMPLETE,
		COMPLETE
	}

	public enum OperationResult {
		OK,
		NOT_STARTED,
		OUT_OF_RANGE,
		FIXED_CELL,
		OCCUPIED,
		EMPTY
	}

	private final int[][] initialBoard = new int[SIZE][SIZE];
	private final int[][] board = new int[SIZE][SIZE];
	private final boolean[][] fixedCells = new boolean[SIZE][SIZE];
	private final String[][] draftNotes = new String[SIZE][SIZE];
	private boolean started;

	public SudokuGame() {
		started = false;
	}

	public void loadPuzzle(int[][] puzzle) {
		copyInto(initialBoard, puzzle);
		copyInto(board, puzzle);
		clearDraftNotes();
		refreshFixedCells();
		started = true;
	}

	public void reset() {
		copyInto(board, initialBoard);
		clearDraftNotes();
		refreshFixedCells();
		started = true;
	}

	public void clearUserEntries() {
		if (!started) {
			return;
		}
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				if (!fixedCells[row][col]) {
					board[row][col] = 0;
					draftNotes[row][col] = "";
				}
			}
		}
	}

	public OperationResult placeNumber(int row, int col, int value) {
		if (!started) {
			return OperationResult.NOT_STARTED;
		}
		if (!isInsideBoard(row, col) || value < 1 || value > SIZE) {
			return OperationResult.OUT_OF_RANGE;
		}
		if (fixedCells[row][col]) {
			return OperationResult.FIXED_CELL;
		}
		if (board[row][col] != 0) {
			return OperationResult.OCCUPIED;
		}
		board[row][col] = value;
		draftNotes[row][col] = "";
		return OperationResult.OK;
	}

	public OperationResult removeNumber(int row, int col) {
		if (!started) {
			return OperationResult.NOT_STARTED;
		}
		if (!isInsideBoard(row, col)) {
			return OperationResult.OUT_OF_RANGE;
		}
		if (fixedCells[row][col]) {
			return OperationResult.FIXED_CELL;
		}
		if (board[row][col] == 0) {
			return OperationResult.EMPTY;
		}
		board[row][col] = 0;
		draftNotes[row][col] = "";
		return OperationResult.OK;
	}

	public OperationResult toggleDraftNote(int row, int col, int value) {
		if (!started) {
			return OperationResult.NOT_STARTED;
		}
		if (!isInsideBoard(row, col) || value < 1 || value > SIZE) {
			return OperationResult.OUT_OF_RANGE;
		}
		if (fixedCells[row][col]) {
			return OperationResult.FIXED_CELL;
		}
		if (board[row][col] != 0) {
			return OperationResult.OCCUPIED;
		}

		String digit = Integer.toString(value);
		String current = draftNotes[row][col] == null ? "" : draftNotes[row][col];
		if (current.contains(digit)) {
			draftNotes[row][col] = removeDigit(current, digit);
		} else {
			draftNotes[row][col] = addDigit(current, digit);
		}
		return OperationResult.OK;
	}

	public OperationResult clearDraftNote(int row, int col) {
		if (!started) {
			return OperationResult.NOT_STARTED;
		}
		if (!isInsideBoard(row, col)) {
			return OperationResult.OUT_OF_RANGE;
		}
		draftNotes[row][col] = "";
		return OperationResult.OK;
	}

	public int getValue(int row, int col) {
		if (!isInsideBoard(row, col)) {
			return 0;
		}
		return board[row][col];
	}

	public boolean isFixed(int row, int col) {
		return isInsideBoard(row, col) && fixedCells[row][col];
	}

	public String getDraftNote(int row, int col) {
		if (!isInsideBoard(row, col)) {
			return "";
		}
		return draftNotes[row][col] == null ? "" : draftNotes[row][col];
	}

	public GameStatus getStatus() {
		if (!started) {
			return GameStatus.NOT_STARTED;
		}
		return isComplete() ? GameStatus.COMPLETE : GameStatus.INCOMPLETE;
	}

	public boolean isComplete() {
		if (!started) {
			return false;
		}
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				if (board[row][col] == 0) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean isSolved() {
		return started && isComplete() && !hasErrors();
	}

	public int getFilledCellsCount() {
		int count = 0;
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				if (board[row][col] != 0) {
					count++;
				}
			}
		}
		return count;
	}

	public boolean hasErrors() {
		return hasAnyConflict(getConflictMap());
	}

	public boolean[][] getConflictMap() {
		boolean[][] conflicts = new boolean[SIZE][SIZE];
		markRowConflicts(conflicts);
		markColumnConflicts(conflicts);
		markBoxConflicts(conflicts);
		return conflicts;
	}

	public static int[][] copyBoard(int[][] source) {
		int[][] copy = new int[SIZE][SIZE];
		copyInto(copy, source);
		return copy;
	}

	public static boolean hasAnyFixedNumber(int[][] board) {
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				if (board[row][col] != 0) {
					return true;
				}
			}
		}
		return false;
	}

	public static int[][] decodeBoard(String encodedBoard) {
		String[] rows = encodedBoard.trim().split("[;\\n]");
		if (rows.length != SIZE) {
			throw new IllegalArgumentException("O tabuleiro precisa ter 9 linhas.");
		}
		int[][] board = new int[SIZE][SIZE];
		for (int row = 0; row < SIZE; row++) {
			String cleanedRow = rows[row].replaceAll("\\s+", "");
			if (cleanedRow.length() != SIZE) {
				throw new IllegalArgumentException("Cada linha do tabuleiro precisa ter 9 caracteres.");
			}
			for (int col = 0; col < SIZE; col++) {
				char symbol = cleanedRow.charAt(col);
				if (symbol == '.' || symbol == '0') {
					board[row][col] = 0;
				} else if (Character.isDigit(symbol)) {
					board[row][col] = Character.digit(symbol, 10);
				} else {
					throw new IllegalArgumentException("O tabuleiro contém um caractere inválido.");
				}
			}
		}
		return board;
	}

	public static int[][] boardFromRawArgs(List<String> rawArgs) {
		if (rawArgs == null || rawArgs.isEmpty()) {
			return null;
		}

		int[][] board = new int[SIZE][SIZE];
		boolean parsedAny = false;
		for (String argument : rawArgs) {
			String cleanedArgument = stripFriendlyPrefix(argument);

			Matcher friendlyMatcher = FRIENDLY_CELL_PATTERN.matcher(cleanedArgument);
			while (friendlyMatcher.find()) {
				int row = Integer.parseInt(friendlyMatcher.group(1)) - 1;
				int col = Integer.parseInt(friendlyMatcher.group(2)) - 1;
				int value = Integer.parseInt(friendlyMatcher.group(3));
				if (isValidPlacement(row, col, value)) {
					board[row][col] = value;
					parsedAny = true;
				}
			}

			Matcher pairMatcher = FRIENDLY_PAIR_PATTERN.matcher(cleanedArgument);
			while (pairMatcher.find()) {
				int row = Integer.parseInt(pairMatcher.group(1)) - 1;
				int col = Integer.parseInt(pairMatcher.group(2)) - 1;
				int value = Integer.parseInt(pairMatcher.group(3));
				if (isValidPlacement(row, col, value)) {
					board[row][col] = value;
					parsedAny = true;
				}
			}
		}

		if (parsedAny) {
			return board;
		}

		List<Integer> numbers = new java.util.ArrayList<>();
		for (String argument : rawArgs) {
			Matcher matcher = NUMBER_PATTERN.matcher(argument);
			while (matcher.find()) {
				numbers.add(Integer.parseInt(matcher.group()));
			}
		}

		if (numbers.size() < 3) {
			return null;
		}

		boolean zeroBased = numbers.stream().anyMatch(value -> value == 0);
		for (int index = 0; index + 2 < numbers.size(); index += 3) {
			int row = numbers.get(index);
			int col = numbers.get(index + 1);
			int value = numbers.get(index + 2);

			if (!zeroBased) {
				row--;
				col--;
			}

			if (isValidPlacement(row, col, value)) {
				board[row][col] = value;
			}
		}

		return hasAnyFixedNumber(board) ? board : null;
	}

	private static void copyInto(int[][] target, int[][] source) {
		for (int row = 0; row < SIZE; row++) {
			System.arraycopy(source[row], 0, target[row], 0, SIZE);
		}
	}

	private void refreshFixedCells() {
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				fixedCells[row][col] = initialBoard[row][col] != 0;
			}
		}
	}

	private void clearDraftNotes() {
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				draftNotes[row][col] = "";
			}
		}
	}

	private static String stripFriendlyPrefix(String argument) {
		return argument.replaceFirst("(?i)^--?(?:puzzle|givens|initial)=", "").trim();
	}

	private static boolean isValidPlacement(int row, int col, int value) {
		return row >= 0 && row < SIZE && col >= 0 && col < SIZE && value >= 1 && value <= SIZE;
	}

	private static String addDigit(String current, String digit) {
		if (current == null || current.isBlank()) {
			return digit;
		}
		if (current.contains(digit)) {
			return current;
		}
		List<Character> digits = new java.util.ArrayList<>();
		for (char character : current.toCharArray()) {
			if (Character.isDigit(character)) {
				digits.add(character);
			}
		}
		digits.add(digit.charAt(0));
		digits.sort(Character::compareTo);
		StringBuilder builder = new StringBuilder();
		for (int index = 0; index < digits.size(); index++) {
			if (index > 0) {
				builder.append(' ');
			}
			builder.append(digits.get(index));
		}
		return builder.toString();
	}

	private static String removeDigit(String current, String digit) {
		if (current == null || current.isBlank()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (String token : current.split("\\s+")) {
			if (!token.equals(digit) && !token.isBlank()) {
				if (builder.length() > 0) {
					builder.append(' ');
				}
				builder.append(token);
			}
		}
		return builder.toString();
	}

	private boolean isInsideBoard(int row, int col) {
		return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
	}

	private boolean hasAnyConflict(boolean[][] conflicts) {
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				if (conflicts[row][col]) {
					return true;
				}
			}
		}
		return false;
	}

	private void markRowConflicts(boolean[][] conflicts) {
		for (int row = 0; row < SIZE; row++) {
			int[] firstOccurrence = new int[SIZE + 1];
			Arrays.fill(firstOccurrence, -1);
			for (int col = 0; col < SIZE; col++) {
				int value = board[row][col];
				if (value == 0) {
					continue;
				}
				if (firstOccurrence[value] == -1) {
					firstOccurrence[value] = col;
				} else {
					conflicts[row][col] = true;
					conflicts[row][firstOccurrence[value]] = true;
				}
			}
		}
	}

	private void markColumnConflicts(boolean[][] conflicts) {
		for (int col = 0; col < SIZE; col++) {
			int[] firstOccurrence = new int[SIZE + 1];
			Arrays.fill(firstOccurrence, -1);
			for (int row = 0; row < SIZE; row++) {
				int value = board[row][col];
				if (value == 0) {
					continue;
				}
				if (firstOccurrence[value] == -1) {
					firstOccurrence[value] = row;
				} else {
					conflicts[row][col] = true;
					conflicts[firstOccurrence[value]][col] = true;
				}
			}
		}
	}

	private void markBoxConflicts(boolean[][] conflicts) {
		for (int boxRow = 0; boxRow < SIZE; boxRow += 3) {
			for (int boxCol = 0; boxCol < SIZE; boxCol += 3) {
				int[] firstRow = new int[SIZE + 1];
				int[] firstCol = new int[SIZE + 1];
				Arrays.fill(firstRow, -1);
				Arrays.fill(firstCol, -1);
				for (int row = boxRow; row < boxRow + 3; row++) {
					for (int col = boxCol; col < boxCol + 3; col++) {
						int value = board[row][col];
						if (value == 0) {
							continue;
						}
						if (firstRow[value] == -1) {
							firstRow[value] = row;
							firstCol[value] = col;
						} else {
							conflicts[row][col] = true;
							conflicts[firstRow[value]][firstCol[value]] = true;
						}
					}
				}
			}
		}
	}
}