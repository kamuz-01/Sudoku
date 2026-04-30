module Sudoku {
	requires javafx.controls;
	requires javafx.fxml;
	
	opens org.JogoSudoku to javafx.graphics, javafx.fxml;
}
