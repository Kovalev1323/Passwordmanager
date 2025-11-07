package com.example.passmanager.controller;

import com.example.passmanager.model.PasswordEntry;
import com.example.passmanager.model.PasswordStrength;
import com.example.passmanager.service.PasswordGenerator;
import com.example.passmanager.service.PasswordRepository;
import com.example.passmanager.service.PasswordStrengthService;
import com.example.passmanager.view.PasswordManagerView;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Связывает представление с моделью.
 */
public final class PasswordManagerController {

    private final PasswordRepository repository;
    private final PasswordManagerView view;
    private final List<PasswordEntry> entries;
    private final PasswordStrengthService strengthService;

    public PasswordManagerController(PasswordRepository repository,
                                     PasswordStrengthService strengthService,
                                     PasswordManagerView view) {
        this.repository = repository;
        this.view = view;
        this.strengthService = strengthService;
        this.entries = new ArrayList<>(repository.load());
        refreshView();
        attachHandlers();
    }

    private void attachHandlers() {
        view.onAdd(this::handleAdd);
        view.onDelete(this::handleDelete);
        view.onGenerate(this::handleGenerate);
        view.onRefresh(this::refreshView);
        view.onPasswordInput(this::handlePasswordInput);
        view.onImport(this::handleImport);
        view.onExport(this::handleExport);
    }

    private void handleAdd(String service, String login, String password, String notes) {
        if (service.isBlank() || password.isBlank()) {
            view.showError("Название и пароль не могут быть пустыми");
            return;
        }
        PasswordEntry entry = repository.create(service, login, password, notes);
        entries.add(entry);
        repository.save(entries);
        refreshView();
        view.clearForm();
        view.showInfo("Запись сохранена");
    }

    private void handleDelete(String entryId) {
        if (entries.removeIf(entry -> entry.getId().equals(entryId))) {
            repository.save(entries);
            refreshView();
            view.showInfo("Запись удалена");
        } else {
            view.showError("Запись с указанным ID не найдена");
        }
    }

    private void handleGenerate(int length) {
        String generated = PasswordGenerator.generate(length);
        view.setGeneratedPassword(generated);
        handlePasswordInput(generated);
    }

    private void refreshView() {
        view.setEntries(entries);
    }

    private void handlePasswordInput(String password) {
        PasswordStrength strength = strengthService.evaluate(password);
        view.updatePasswordStrength(strength);
    }

    private void handleImport(Path path) {
        if (path == null) {
            return;
        }
        
        // Проверяем расширение файла
        String fileName = path.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".json")) {
            view.showError("Можно загружать только JSON файлы (.json)");
            return;
        }
        
        repository.loadFrom(path).ifPresentOrElse(loaded -> {
            // Объединяем загруженные записи с существующими (новые записи не пропадают)
            for (PasswordEntry loadedEntry : loaded) {
                // Проверяем, нет ли уже записи с таким ID
                boolean exists = entries.stream()
                        .anyMatch(entry -> entry.getId().equals(loadedEntry.getId()));
                if (!exists) {
                    entries.add(loadedEntry);
                }
            }
            repository.save(entries);
            refreshView();
            view.showInfo("Записи загружены и объединены с существующими");
        }, () -> view.showError("Не удалось загрузить JSON файл. Убедитесь, что файл имеет формат JSON."));
    }

    private void handleExport(Path path) {
        if (path == null) {
            return;
        }
        
        // Проверяем расширение файла
        String fileName = path.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".json")) {
            view.showError("Файл должен иметь расширение .json");
            return;
        }
        
        boolean success = repository.saveTo(path, entries);
        if (success) {
            view.showInfo("Записи сохранены в JSON файл: " + path.getFileName());
        } else {
            view.showError("Не удалось сохранить JSON файл. Проверьте права доступа и формат файла.");
        }
    }
}


