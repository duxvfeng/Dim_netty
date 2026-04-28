package com.example.chat.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatClientApp extends Application {

    private ChatController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        log.info("Starting Chat Client Application...");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root, 800, 600);

        primaryStage.setTitle("WebSocket Chat Client");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(400);

        primaryStage.setOnCloseRequest(event -> {
            log.info("Closing application...");
            if (controller != null) {
                controller.shutdown();
            }
            Platform.exit();
        });

        primaryStage.show();
        log.info("Chat Client Application started successfully");
    }

    @Override
    public void stop() throws Exception {
        log.info("Application stopped");
        if (controller != null) {
            controller.shutdown();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
