package in.gram.gov.app.egram_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Slf4j
public class TokenEncryptionService {

    @Value("${google.oauth.encryption-key:}")
    private String encryptionKey;

    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;

    private SecretKey getSecretKey() {
        try {
            if (encryptionKey == null || encryptionKey.isEmpty()) {
                // Generate a key if not configured (for development only)
                log.warn("Encryption key not configured. Using default key (NOT SECURE FOR PRODUCTION)");
                return generateDefaultKey();
            }
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (Exception e) {
            log.error("Error getting secret key", e);
            return generateDefaultKey();
        }
    }

    private SecretKey generateDefaultKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_SIZE);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            log.error("Error generating default key", e);
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }

    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("Error encrypting token", e);
            throw new RuntimeException("Failed to encrypt token", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error decrypting token", e);
            throw new RuntimeException("Failed to decrypt token", e);
        }
    }
}

