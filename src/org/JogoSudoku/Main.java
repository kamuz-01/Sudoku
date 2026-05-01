package org.JogoSudoku;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;


public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("Sample.fxml"));
			Scene scene = new Scene(loader.load(), 1280, 780);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

			SampleController controller = loader.getController();
			controller.initializeGame(getParameters().getRaw());
			controller.installSceneShortcuts(scene);

			primaryStage.setTitle("Sudoku");
			// ÍCONE DA JANELA
            primaryStage.getIcons().add(
                new Image(getClass().getResourceAsStream("imagens/icone-sudoku.png"))
            );
			primaryStage.setMinWidth(1020);
			primaryStage.setMinHeight(560);
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
