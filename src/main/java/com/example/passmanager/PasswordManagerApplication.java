package com.example.passmanager;

import com.example.passmanager.controller.PasswordManagerController;
import com.example.passmanager.service.PasswordRepository;
import com.example.passmanager.service.PasswordStrengthService;
import com.example.passmanager.view.PasswordManagerView;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX приложение менеджера паролей.
 */
public class PasswordManagerApplication extends Application {

    public static void launchApp(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        PasswordManagerView view = new PasswordManagerView(primaryStage);
        PasswordRepository repository = new PasswordRepository();
        PasswordStrengthService strengthService = new PasswordStrengthService();
        new PasswordManagerController(repository, strengthService, view);
    }
}



