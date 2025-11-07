package com.example.passmanager.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Модель одной записи в менеджере паролей.
 */
public final class PasswordEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String service;
    private final String username;
    private final String password;
    private final String notes;
    private final LocalDateTime createdAt;

    public PasswordEntry(String id,
                         String service,
                         String username,
                         String password,
                         String notes,
                         LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.service = Objects.requireNonNull(service, "service");
        this.username = username == null ? "" : username;
        this.password = Objects.requireNonNull(password, "password");
        this.notes = notes == null ? "" : notes;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public String getId() {
        return id;
    }

    public String getService() {
        return service;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

