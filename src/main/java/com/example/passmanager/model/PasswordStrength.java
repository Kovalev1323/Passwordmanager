package com.example.passmanager.model;

/**
 * Уровни надёжности пароля.
 */
public enum PasswordStrength {
    VERY_WEAK("Очень слабый", "#d32f2f"),
    WEAK("Слабый", "#f57c00"),
    MEDIUM("Средний", "#fbc02d"),
    STRONG("Сильный", "#388e3c"),
    VERY_STRONG("Очень сильный", "#2e7d32");

    private final String label;
    private final String color;

    PasswordStrength(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }
}

