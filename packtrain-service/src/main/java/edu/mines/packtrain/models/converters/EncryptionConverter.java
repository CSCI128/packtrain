package edu.mines.packtrain.models.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Slf4j
@Component
@Converter
public class EncryptionConverter implements AttributeConverter<String, String> {
    private final Key key;
    private final Cipher cipher;

    public EncryptionConverter(@Value("${grading-admin.secret-key}") String secretKey)
            throws NoSuchPaddingException, NoSuchAlgorithmException
    {
        key = new SecretKeySpec(secretKey.getBytes(), "AES");
        cipher = Cipher.getInstance("AES");
    }

    @Override
    public String convertToDatabaseColumn(String s) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(s.getBytes()));
        } catch (IllegalBlockSizeException | InvalidKeyException | BadPaddingException e) {
            log.error("failed to encrypt database column", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String convertToEntityAttribute(String s) {
        try {
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.getDecoder().decode(s)));
        } catch (IllegalBlockSizeException | InvalidKeyException | BadPaddingException e) {
            log.error("failed to decrypt database column", e);
            throw new IllegalStateException(e);
        }
    }
}
