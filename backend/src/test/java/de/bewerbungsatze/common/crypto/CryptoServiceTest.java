package de.bewerbungsatze.common.crypto;

import de.bewerbungsatze.common.config.CryptoProperties;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoServiceTest {

    private final CryptoService crypto = new CryptoService(
            new CryptoProperties(Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes())));

    @Test
    void roundTripReturnsOriginal() {
        String secret = "sk-very-secret-api-key-123";
        String encrypted = crypto.encrypt(secret);
        assertThat(encrypted).isNotEqualTo(secret);
        assertThat(crypto.decrypt(encrypted)).isEqualTo(secret);
    }

    @Test
    void encryptionIsNonDeterministic() {
        String secret = "same-input";
        assertThat(crypto.encrypt(secret)).isNotEqualTo(crypto.encrypt(secret));
    }

    @Test
    void nullPassesThrough() {
        assertThat(crypto.encrypt(null)).isNull();
        assertThat(crypto.decrypt(null)).isNull();
    }

    @Test
    void tamperedCiphertextFails() {
        String encrypted = crypto.encrypt("payload");
        byte[] raw = Base64.getDecoder().decode(encrypted);
        raw[raw.length - 1] ^= 0x01; // letztes Tag-Byte kippen
        String tampered = Base64.getEncoder().encodeToString(raw);
        assertThatThrownBy(() -> crypto.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsInvalidKeyLength() {
        CryptoProperties bad = new CryptoProperties(Base64.getEncoder().encodeToString("short".getBytes()));
        assertThatThrownBy(() -> new CryptoService(bad)).isInstanceOf(IllegalStateException.class);
    }
}
