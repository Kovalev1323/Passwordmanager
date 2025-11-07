package com.example.passmanager.service;

import com.example.passmanager.model.PasswordEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Хранилище записей на файловой системе пользователя в формате JSON.
 */
public final class PasswordRepository {

    private final Path defaultFile;
    private final Path baseDir;
    private final EncryptionService encryptionService;

    public PasswordRepository() {
        this.baseDir = Path.of(System.getProperty("user.home"), ".simple-password-manager");
        this.defaultFile = baseDir.resolve("vault.json");
        this.encryptionService = new EncryptionService(baseDir);
    }

    public List<PasswordEntry> load() {
        return loadFrom(defaultFile).orElseGet(ArrayList::new);
    }

    public Optional<List<PasswordEntry>> loadFrom(Path path) {
        if (path == null || !Files.exists(path)) {
            return Optional.empty();
        }
        
        // Проверяем, что файл имеет расширение .json
        String fileName = path.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".json")) {
            System.err.println("Ошибка: можно загружать только JSON файлы (.json)");
            return Optional.empty();
        }
        
        try {
            String jsonContent = Files.readString(path, StandardCharsets.UTF_8);
            List<PasswordEntry> entries = parseJson(jsonContent);
            return Optional.of(entries);
        } catch (IOException e) {
            System.err.println("Не удалось прочитать хранилище: " + e.getMessage());
            return Optional.empty();
        }
    }

    public void save(List<PasswordEntry> entries) {
        saveTo(defaultFile, entries);
    }

    public boolean saveTo(Path path, List<PasswordEntry> entries) {
        if (path == null) {
            return false;
        }
        
        // Проверяем, что файл имеет расширение .json
        String fileName = path.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".json")) {
            System.err.println("Ошибка: файл должен иметь расширение .json");
            return false;
        }
        
        try {
            // Создаем директорию, если её нет
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            
            String jsonContent = toJson(entries);
            
            // Проверяем, что JSON не пустой
            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                System.err.println("Ошибка: сгенерированный JSON пуст");
                return false;
            }
            
            // Сохраняем файл в формате JSON
            Files.writeString(path, jsonContent, StandardCharsets.UTF_8);
            
            // Проверяем, что файл действительно создан
            if (!Files.exists(path)) {
                System.err.println("Ошибка: файл не был создан по пути: " + path);
                return false;
            }
            
            return true;
        } catch (IOException e) {
            System.err.println("Не удалось сохранить хранилище: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("Неожиданная ошибка при сохранении: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public PasswordEntry create(String service,
                                String username,
                                String password,
                                String notes) {
        // Создаем запись с расшифрованным паролем (шифрование происходит при сохранении)
        return new PasswordEntry(generateId(), service, username, password, notes, LocalDateTime.now());
    }

    private String generateId() {
        return Long.toHexString(System.nanoTime());
    }

    private String toJson(List<PasswordEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "[]";
        }
        
        StringBuilder json = new StringBuilder("[\n");
        for (int i = 0; i < entries.size(); i++) {
            PasswordEntry entry = entries.get(i);
            if (entry == null) {
                continue;
            }
            
            // Всегда шифруем пароль перед сохранением в JSON
            String passwordToSave = entry.getPassword() != null && !entry.getPassword().isEmpty()
                    ? encryptionService.encrypt(entry.getPassword())
                    : entry.getPassword();
            
            json.append("  {\n");
            json.append("    \"id\": \"").append(escapeJson(entry.getId())).append("\",\n");
            json.append("    \"service\": \"").append(escapeJson(entry.getService())).append("\",\n");
            json.append("    \"username\": \"").append(escapeJson(entry.getUsername())).append("\",\n");
            json.append("    \"password\": \"").append(escapeJson(passwordToSave)).append("\",\n");
            json.append("    \"notes\": \"").append(escapeJson(entry.getNotes())).append("\",\n");
            json.append("    \"createdAt\": \"").append(entry.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\"\n");
            json.append("  }");
            if (i < entries.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("]");
        return json.toString();
    }

    private List<PasswordEntry> parseJson(String json) {
        List<PasswordEntry> entries = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) {
            return entries;
        }

        // Улучшенный парсер JSON для массива объектов
        // Ищем начало массива
        int startIdx = json.indexOf('[');
        if (startIdx == -1) {
            return entries;
        }

        int pos = startIdx + 1;
        while (pos < json.length()) {
            // Пропускаем пробелы и переносы строк
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
                pos++;
            }
            
            if (pos >= json.length() || json.charAt(pos) == ']') {
                break;
            }

            // Пропускаем запятую
            if (json.charAt(pos) == ',') {
                pos++;
                continue;
            }

            // Ищем начало объекта
            if (json.charAt(pos) == '{') {
                int objStart = pos;
                int braceCount = 0;
                boolean inString = false;
                boolean escaped = false;
                
                // Находим конец объекта, учитывая экранированные кавычки
                for (int i = objStart; i < json.length(); i++) {
                    char c = json.charAt(i);
                    if (escaped) {
                        escaped = false;
                        continue;
                    }
                    if (c == '\\') {
                        escaped = true;
                        continue;
                    }
                    if (c == '"') {
                        inString = !inString;
                        continue;
                    }
                    if (!inString) {
                        if (c == '{') {
                            braceCount++;
                        } else if (c == '}') {
                            braceCount--;
                            if (braceCount == 0) {
                                String entryJson = json.substring(objStart, i + 1);
                                pos = i + 1;
                                
                                try {
                                    PasswordEntry entry = parseEntry(entryJson);
                                    if (entry != null) {
                                        entries.add(entry);
                                    }
                                } catch (Exception e) {
                                    System.err.println("Ошибка при парсинге записи: " + e.getMessage());
                                }
                                break;
                            }
                        }
                    }
                }
            } else {
                pos++;
            }
        }

        return entries;
    }

    private PasswordEntry parseEntry(String entryJson) {
        String id = extractJsonValue(entryJson, "id");
        String service = extractJsonValue(entryJson, "service");
        String username = extractJsonValue(entryJson, "username");
        String password = extractJsonValue(entryJson, "password");
        String notes = extractJsonValue(entryJson, "notes");
        String createdAtStr = extractJsonValue(entryJson, "createdAt");

        if (id.isEmpty() || service.isEmpty()) {
            return null;
        }

        // Расшифровываем пароль (если он зашифрован)
        String decryptedPassword;
        try {
            decryptedPassword = encryptionService.decrypt(password);
        } catch (Exception e) {
            // Если расшифровка не удалась, возможно пароль уже расшифрован
            decryptedPassword = password;
        }

        LocalDateTime createdAt = LocalDateTime.parse(createdAtStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return new PasswordEntry(id, service, username, decryptedPassword, notes, createdAt);
    }

    private String extractJsonValue(String json, String key) {
        // Ищем ключ с кавычками
        String keyPattern = "\"" + Pattern.quote(key) + "\"\\s*:\\s*\"";
        Pattern pattern = Pattern.compile(keyPattern);
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            int start = matcher.end();
            StringBuilder value = new StringBuilder();
            boolean escaped = false;
            
            // Читаем значение до закрывающей кавычки, учитывая экранирование
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (escaped) {
                    if (c == 'n') {
                        value.append('\n');
                    } else if (c == 'r') {
                        value.append('\r');
                    } else if (c == 't') {
                        value.append('\t');
                    } else {
                        value.append(c);
                    }
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    break;
                } else {
                    value.append(c);
                }
            }
            return value.toString();
        }
        return "";
    }

    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}

