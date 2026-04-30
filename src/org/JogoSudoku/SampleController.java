package org.JogoSudoku;

import java.util.List;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

public class SampleController {
	private static final String[] PRESET_NAMES = {
		"Desafio A",
		"Desafio B",
		"Desafio C"
	};

	private static final String[] PRESET_BOARDS = {
		"530070000;600195000;098000060;800060003;400803001;700020006;060000280;000419005;000080079",
		"200080300;060070084;030500209;000105408;000000000;402706000;301007040;720040060;004010003",
		"900002000;060000080;007090000;000900020;020050090;050008000;000060500;080000040;000700001"
	};

	@FXML private BorderPane rootPane;
	@FXML private GridPane boardGrid;
	@FXML private Label statusLabel;
	@FXML private Label subtitleLabel;
	@FXML private Label puzzleNameLabel;
	@FXML private Label selectionLabel;
	@FXML private TextField rowField;
	@FXML private TextField colField;
	@FXML private TextField valueField;
	@FXML private CheckBox draftModeCheck;
	@FXML private Button placeButton;
	@FXML private Button removeButton;
	@FXML private Button verifyButton;
	@FXML private Button statusButton;
	@FXML private Button clearButton;
	@FXML private Button finishButton;
	@FXML private Button restartButton;
	@FXML private Button newGameButton;

	private final Button[][] boardButtons = new Button[SudokuGame.SIZE][SudokuGame.SIZE];
	private final Label[][] valueLabels = new Label[SudokuGame.SIZE][SudokuGame.SIZE];
	private final Label[][] draftLabels = new Label[SudokuGame.SIZE][SudokuGame.SIZE];
	private final SudokuGame game = new SudokuGame();
	private int selectedRow = -1;
	private int selectedCol = -1;
	private Scene scene;
	private int presetIndex;
	private int[][] currentInitialBoard;
	private String currentPuzzleName = "Jogo inicial";

	@FXML
	private void initialize() {
		buildBoard();
		applyNumericFilters();
		subtitleLabel.setText("Use o menu para trocar de partida e os controles laterais para jogar.");
		selectionLabel.setText("Nenhuma célula selecionada.");
		draftModeCheck.setSelected(false);
		draftModeCheck.selectedProperty().addListener((observable, oldValue, newValue) -> updateDraftModeHint(newValue));
		updateDraftModeHint(false);
		updateStatusPanel();
	}

	public void installSceneShortcuts(Scene scene) {
		this.scene = scene;
		scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleSceneKeyPressed);
	}

	public void initializeGame(List<String> rawArgs) {
		int[][] parsedBoard = SudokuGame.boardFromRawArgs(rawArgs);
		if (parsedBoard != null && SudokuGame.hasAnyFixedNumber(parsedBoard)) {
			currentPuzzleName = "Tabuleiro inicial informado nos argumentos";
			loadBoard(parsedBoard, currentPuzzleName, false);
			return;
		}

		presetIndex = 0;
		loadPreset(presetIndex);
	}

	@FXML
	private void handleNewGame() {
		presetIndex = (presetIndex + 1) % PRESET_BOARDS.length;
		loadPreset(presetIndex);
	}

	@FXML
	private void handleRestart() {
		if (currentInitialBoard == null) {
			return;
		}
		loadBoard(currentInitialBoard, currentPuzzleName, false);
		showInfo("Reiniciado", "O tabuleiro atual foi restaurado para a configuração inicial.");
	}

	@FXML
	private void handleExit() {
		Platform.exit();
	}

	@FXML
	private void handlePlace() {
		ParsedInputs inputs = readInputs(true);
		if (inputs == null) {
			return;
		}

		SudokuGame.OperationResult result = draftModeCheck.isSelected()
			? game.toggleDraftNote(inputs.row, inputs.col, inputs.value)
			: game.placeNumber(inputs.row, inputs.col, inputs.value);
		if (result != SudokuGame.OperationResult.OK) {
			showOperationError(result, inputs.row, inputs.col, inputs.value);
			return;
		}

		selectCell(inputs.row, inputs.col);
		updateStatusPanel();
	}

	@FXML
	private void handleRemove() {
		ParsedInputs inputs = readInputs(false);
		if (inputs == null) {
			return;
		}

		SudokuGame.OperationResult result = draftModeCheck.isSelected()
			? game.clearDraftNote(inputs.row, inputs.col)
			: game.removeNumber(inputs.row, inputs.col);
		if (result != SudokuGame.OperationResult.OK) {
			showOperationError(result, inputs.row, inputs.col, 0);
			return;
		}

		selectCell(inputs.row, inputs.col);
		updateStatusPanel();
	}

	@FXML
	private void handleVerify() {
		refreshBoard();
		String summary = buildSummaryMessage();
		showInfo("Verificação do tabuleiro", summary);
	}

	@FXML
	private void handleStatus() {
		updateStatusPanel();
		showInfo("Status do jogo", buildSummaryMessage());
	}

	@FXML
	private void handleClear() {
		game.clearUserEntries();
		selectedRow = -1;
		selectedCol = -1;
		rowField.clear();
		colField.clear();
		valueField.clear();
		selectionLabel.setText("Os números informados pelo jogador foram removidos.");
		refreshBoard();
		updateStatusPanel();
	}

	@FXML
	private void handleFinish() {
		if (!game.isSolved()) {
			showError("Ainda não é possível finalizar", "Preencha todos os espaços com números válidos antes de encerrar o jogo.");
			return;
		}

		showInfo("Jogo concluído", "Parabéns. O tabuleiro foi preenchido corretamente e o jogo será encerrado.");
		Platform.exit();
	}

	private void loadPreset(int index) {
		currentPuzzleName = PRESET_NAMES[index];
		loadBoard(SudokuGame.decodeBoard(PRESET_BOARDS[index]), currentPuzzleName, false);
	}

	private void loadBoard(int[][] board, String puzzleName, boolean preserveSelection) {
		currentInitialBoard = SudokuGame.copyBoard(board);
		game.loadPuzzle(board);
		currentPuzzleName = puzzleName;
		puzzleNameLabel.setText(currentPuzzleName);
		subtitleLabel.setText("Use os controles laterais para preencher, remover e verificar o Sudoku.");
		if (!preserveSelection) {
			selectedRow = -1;
			selectedCol = -1;
			rowField.clear();
			colField.clear();
			valueField.clear();
			selectionLabel.setText("Nenhuma célula selecionada.");
		}
		refreshBoard();
		updateStatusPanel();
	}

	private void buildBoard() {
		boardGrid.getChildren().clear();
		for (int row = 0; row < SudokuGame.SIZE; row++) {
			for (int col = 0; col < SudokuGame.SIZE; col++) {
				int cellRow = row;
				int cellCol = col;
				Button cell = new Button();
				Label valueLabel = new Label();
				Label draftLabel = new Label();
				StackPane graphic = new StackPane(valueLabel, draftLabel);
				graphic.getStyleClass().add("cell-graphic");
				StackPane.setAlignment(valueLabel, javafx.geometry.Pos.CENTER);
				StackPane.setAlignment(draftLabel, javafx.geometry.Pos.TOP_LEFT);
				valueLabel.getStyleClass().add("cell-value");
				draftLabel.getStyleClass().add("cell-draft");
				cell.setMinSize(56, 56);
				cell.setPrefSize(56, 56);
				cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
				cell.setFocusTraversable(false);
				cell.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
				cell.setGraphic(graphic);
				cell.getStyleClass().add("sudoku-cell");
				cell.setOnAction(event -> selectCell(cellRow, cellCol));
				boardButtons[row][col] = cell;
				valueLabels[row][col] = valueLabel;
				draftLabels[row][col] = draftLabel;
				boardGrid.add(cell, col, row);
			}
		}
	}

	private void applyNumericFilters() {
		rowField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("[0-9]*") ? change : null));
		colField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("[0-9]*") ? change : null));
		valueField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("[0-9]*") ? change : null));
		rowField.setPromptText("1 a 9");
		colField.setPromptText("1 a 9");
		valueField.setPromptText("1 a 9");
	}

	private void selectCell(int row, int col) {
		selectedRow = row;
		selectedCol = col;
		rowField.setText(Integer.toString(row + 1));
		colField.setText(Integer.toString(col + 1));
		int currentValue = game.getValue(row, col);
		valueField.setText(currentValue == 0 ? "" : Integer.toString(currentValue));
		selectionLabel.setText(String.format("Célula selecionada: linha %d, coluna %d.", row + 1, col + 1));
		refreshBoard();
	}

	private void handleSceneKeyPressed(KeyEvent event) {
		if (selectedRow < 0 || selectedCol < 0) {
			return;
		}

		String typedText = event.getText();
		if (typedText != null && typedText.length() == 1 && Character.isDigit(typedText.charAt(0))) {
			int digit = Character.digit(typedText.charAt(0), 10);
			if (digit >= 1 && digit <= SudokuGame.SIZE) {
				applyDirectInput(digit);
				event.consume();
			}
			return;
		}

		if (event.getCode() == KeyCode.BACK_SPACE || event.getCode() == KeyCode.DELETE) {
			clearSelectedCell();
			event.consume();
		}
	}

	private void applyDirectInput(int value) {
		SudokuGame.OperationResult result = draftModeCheck.isSelected()
			? game.toggleDraftNote(selectedRow, selectedCol, value)
			: game.placeNumber(selectedRow, selectedCol, value);
		if (result != SudokuGame.OperationResult.OK) {
			showOperationError(result, selectedRow, selectedCol, value);
			return;
		}

		rowField.setText(Integer.toString(selectedRow + 1));
		colField.setText(Integer.toString(selectedCol + 1));
		valueField.setText(Integer.toString(value));
		selectionLabel.setText(String.format("Célula selecionada: linha %d, coluna %d.", selectedRow + 1, selectedCol + 1));
		updateStatusPanel();
	}

	private void clearSelectedCell() {
		if (selectedRow < 0 || selectedCol < 0) {
			return;
		}

		SudokuGame.OperationResult result = draftModeCheck.isSelected()
			? game.clearDraftNote(selectedRow, selectedCol)
			: game.removeNumber(selectedRow, selectedCol);
		if (result != SudokuGame.OperationResult.OK) {
			showOperationError(result, selectedRow, selectedCol, 0);
			return;
		}

		valueField.clear();
		refreshBoard();
		updateStatusPanel();
	}

	private void refreshBoard() {
		boolean[][] conflicts = game.getConflictMap();
		for (int row = 0; row < SudokuGame.SIZE; row++) {
			for (int col = 0; col < SudokuGame.SIZE; col++) {
				Button cell = boardButtons[row][col];
				Label valueLabel = valueLabels[row][col];
				Label draftLabel = draftLabels[row][col];
				int value = game.getValue(row, col);
				String draftNote = game.getDraftNote(row, col);
				valueLabel.setText(value == 0 ? "" : Integer.toString(value));
				draftLabel.setText(value == 0 && !draftNote.isBlank() ? draftNote : "");
				cell.getStyleClass().removeAll("fixed-cell", "user-cell", "empty-cell", "selected-cell", "error-cell");
				if (game.isFixed(row, col)) {
					cell.getStyleClass().add("fixed-cell");
				} else if (value == 0) {
					cell.getStyleClass().add("empty-cell");
				} else {
					cell.getStyleClass().add("user-cell");
				}
				if (row == selectedRow && col == selectedCol) {
					cell.getStyleClass().add("selected-cell");
				}
				if (conflicts[row][col]) {
					cell.getStyleClass().add("error-cell");
				}
				cell.setStyle(buildCellBorderStyle(row, col));
			}
		}
	}

	private String buildCellBorderStyle(int row, int col) {
		int top = row == 0 ? 3 : 1;
		int right = col == SudokuGame.SIZE - 1 ? 3 : (col == 2 || col == 5 ? 5 : 1);
		int bottom = row == SudokuGame.SIZE - 1 ? 3 : (row == 2 || row == 5 ? 5 : 1);
		int left = col == 0 ? 3 : 1;
		String thinColor = "rgba(255,255,255,0.10)";
		String thickColor = "rgba(56,189,248,0.95)";
		String topColor = row == 0 ? thickColor : thinColor;
		String rightColor = col == SudokuGame.SIZE - 1 ? thickColor : (col == 2 || col == 5 ? thickColor : thinColor);
		String bottomColor = row == SudokuGame.SIZE - 1 ? thickColor : (row == 2 || row == 5 ? thickColor : thinColor);
		String leftColor = col == 0 ? thickColor : thinColor;
		return "-fx-border-width: " + top + " " + right + " " + bottom + " " + left + ";"
			+ "-fx-border-color: " + topColor + ", " + rightColor + ", " + bottomColor + ", " + leftColor + ";";
	}

	private void updateStatusPanel() {
		SudokuGame.GameStatus status = game.getStatus();
		boolean hasErrors = game.hasErrors();
		String text;
		switch (status) {
			case COMPLETE -> text = hasErrors ? "Completo com erros" : "Completo";
			case INCOMPLETE -> text = hasErrors ? "Incompleto com erros" : "Incompleto";
			default -> text = "Não iniciado";
		}
		statusLabel.setText(text + " • " + game.getFilledCellsCount() + "/81 preenchidos");
		statusLabel.getStyleClass().removeAll("status-muted", "status-warning", "status-success", "status-danger");
		if (status == SudokuGame.GameStatus.NOT_STARTED) {
			statusLabel.getStyleClass().add("status-muted");
		} else if (hasErrors) {
			statusLabel.getStyleClass().add("status-danger");
		} else if (status == SudokuGame.GameStatus.COMPLETE) {
			statusLabel.getStyleClass().add("status-success");
		} else {
			statusLabel.getStyleClass().add("status-warning");
		}
		refreshBoard();
	}

	private void updateDraftModeHint(boolean draftModeEnabled) {
		if (draftModeEnabled) {
			subtitleLabel.setText("Modo rascunho ativo: digite números no teclado para alternar candidatos no quadrinho selecionado. Backspace limpa o rascunho.");
		} else {
			subtitleLabel.setText("Modo número ativo: digite um número ou use Colocar para definir o valor final da célula. Backspace apaga o número informado.");
		}
	}

	private ParsedInputs readInputs(boolean includeValue) {
		Integer row = parseField(rowField, "linha");
		Integer col = parseField(colField, "coluna");
		if (row == null || col == null) {
			return null;
		}

		int value = 0;
		if (includeValue) {
			Integer parsedValue = parseField(valueField, "número");
			if (parsedValue == null) {
				return null;
			}
			value = parsedValue;
		}

		return new ParsedInputs(row - 1, col - 1, value);
	}

	private Integer parseField(TextField field, String label) {
		String text = field.getText() == null ? "" : field.getText().trim();
		if (text.isEmpty()) {
			showError("Campo obrigatório", "Informe a " + label + ".");
			return null;
		}

		try {
			int value = Integer.parseInt(text);
			if (value < 1 || value > SudokuGame.SIZE) {
				showError("Valor fora do intervalo", "A " + label + " deve estar entre 1 e 9.");
				return null;
			}
			return value;
		} catch (NumberFormatException ex) {
			showError("Valor inválido", "A " + label + " precisa ser numérica.");
			return null;
		}
	}

	private void showOperationError(SudokuGame.OperationResult result, int row, int col, int value) {
		String location = String.format("linha %d, coluna %d", row + 1, col + 1);
		switch (result) {
			case FIXED_CELL -> showError("Posição bloqueada", "O número fixo em " + location + " não pode ser alterado.");
			case OCCUPIED -> showError("Posição ocupada", "Já existe um número em " + location + ".");
			case EMPTY -> showError("Nada para remover", "Não há número nessa posição.");
			case OUT_OF_RANGE -> showError("Valor inválido", value > 0 ? "O número deve ficar entre 1 e 9." : "Os índices devem ficar entre 1 e 9.");
			case NOT_STARTED -> showError("Jogo não iniciado", "Carregue um tabuleiro antes de jogar.");
			default -> { }
		}
	}

	private String buildSummaryMessage() {
		StringBuilder builder = new StringBuilder();
		builder.append("Puzzle atual: ").append(currentPuzzleName).append('\n');
		builder.append("Status: ").append(describeStatus()).append('\n');
		builder.append("Erros: ").append(game.hasErrors() ? "sim" : "não").append('\n');
		builder.append("Casas preenchidas: ").append(game.getFilledCellsCount()).append(" de 81").append('\n');
		if (game.isSolved()) {
			builder.append("O tabuleiro está completo e válido.");
		} else if (game.hasErrors()) {
			builder.append("Há números em conflito no tabuleiro.");
		} else {
			builder.append("Ainda faltam casas para preencher.");
		}
		return builder.toString();
	}

	private String describeStatus() {
		return switch (game.getStatus()) {
			case COMPLETE -> "Completo";
			case INCOMPLETE -> "Incompleto";
			default -> "Não iniciado";
		};
	}

	private void showInfo(String title, String message) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
		alert.initOwner(rootPane.getScene().getWindow());
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.showAndWait();
	}

	private void showError(String title, String message) {
		Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
		alert.initOwner(rootPane.getScene().getWindow());
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.showAndWait();
	}

	private static final class ParsedInputs {
		private final int row;
		private final int col;
		private final int value;

		private ParsedInputs(int row, int col, int value) {
			this.row = row;
			this.col = col;
			this.value = value;
		}
	}
}
