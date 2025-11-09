package com.example.passmanager.service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Сервис для шифрования и расшифровки паролей.
 */
public final class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;
    private static final String KEY_FILE_NAME = "master.key";

    private final SecretKey secretKey;

    public EncryptionService(Path baseDir) {
        this.secretKey = loadOrGenerateKey(baseDir);
    }

    private SecretKey loadOrGenerateKey(Path baseDir) {
        Path keyFile = baseDir.resolve(KEY_FILE_NAME);
        try {
            if (Files.exists(keyFile)) {
                byte[] keyBytes = Files.readAllBytes(keyFile);
                return new SecretKeySpec(keyBytes, ALGORITHM);
            } else {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
                keyGenerator.init(KEY_SIZE, new SecureRandom());
                SecretKey key = keyGenerator.generateKey();
                Files.createDirectories(baseDir);
                Files.write(keyFile, key.getEncoded());
                return key;
            }
        } catch (Exception e) {
            throw new RuntimeException("Не удалось инициализировать ключ шифрования", e);
        }
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при шифровании", e);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при расшифровке", e);
        }
    }
}


