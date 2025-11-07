package com.example.passmanager.service;

import com.example.passmanager.model.PasswordStrength;

import java.util.regex.Pattern;

/**
 * Оценивает сложность паролей.
 */
public final class PasswordStrengthService {

    private static final Pattern UPPER = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWER = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern SYMBOL = Pattern.compile(".*[^A-Za-z0-9].*");

    public PasswordStrength evaluate(String password) {
        if (password == null || password.isBlank()) {
            return PasswordStrength.VERY_WEAK;
        }

        int score = 0;
        if (password.length() >= 8) {
            score++;
        }
        if (password.length() >= 12) {
            score++;
        }
        if (UPPER.matcher(password).matches() && LOWER.matcher(password).matches()) {
            score++;
        }
        if (DIGIT.matcher(password).matches()) {
            score++;
        }
        if (SYMBOL.matcher(password).matches()) {
            score++;
        }

        return switch (score) {
            case 0, 1 -> PasswordStrength.VERY_WEAK;
            case 2 -> PasswordStrength.WEAK;
            case 3 -> PasswordStrength.MEDIUM;
            case 4 -> PasswordStrength.STRONG;
            default -> PasswordStrength.VERY_STRONG;
        };
    }
}

