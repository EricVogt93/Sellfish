package de.sellfish.enterprise;

import static org.assertj.core.api.Assertions.assertThat;

import de.sellfish.enterprise.LicenseValidator.LicensePayload;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LicenseValidatorTest {

    private static KeyPair keyPair;
    private static LicenseValidator validator;

    @BeforeAll
    static void setup() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        keyPair = gen.generateKeyPair();
        validator = new LicenseValidator(
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
    }

    private String sign(String payload) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(keyPair.getPrivate());
        sig.update(payload.getBytes(StandardCharsets.UTF_8));
        return payload + "::" + Base64.getEncoder().encodeToString(sig.sign());
    }

    @Test
    void validatesCorrectlySignedLicense() throws Exception {
        String payload =
                """
                {"sub":"Acme Corp","exp":"2099-12-31T23:59:59Z","feat":["sso","audit","reports"]}""";
        LicensePayload result = validator.validate(sign(payload));

        assertThat(result.valid()).isTrue();
        assertThat(result.subject()).isEqualTo("Acme Corp");
        assertThat(result.features()).containsExactlyInAnyOrder("sso", "audit", "reports");
        assertThat(result.hasFeature("sso")).isTrue();
        assertThat(result.hasFeature("multi-tenant")).isFalse();
    }

    @Test
    void invalidForTamperedSignature() throws Exception {
        String payload = """
                {"sub":"Acme","exp":"2099-12-31T23:59:59Z","feat":["sso"]}""";
        String license = sign(payload);
        // flip a character in the signature
        String tampered = payload + "::" + "AAAA" + license.substring(license.lastIndexOf("::") + 5);
        assertThat(validator.validate(tampered).valid()).isFalse();
    }

    @Test
    void invalidForMissingSeparator() {
        assertThat(validator.validate("noseparatorhere").valid()).isFalse();
    }

    @Test
    void invalidForNullOrBlank() {
        assertThat(validator.validate(null).valid()).isFalse();
        assertThat(validator.validate("").valid()).isFalse();
        assertThat(validator.validate("   ").valid()).isFalse();
    }

    @Test
    void invalidForExpiredLicense() throws Exception {
        String payload = """
                {"sub":"Old","exp":"2000-01-01T00:00:00Z","feat":["sso"]}""";
        LicensePayload result = validator.validate(sign(payload));
        // signature valid, but expired -> valid() is false (payload parsed, but not current)
        assertThat(result.valid()).isFalse();
        assertThat(result.subject()).isEqualTo("Old");
    }

    @Test
    void licenseWithoutExpiryStaysValid() throws Exception {
        String payload = """
                {"sub":"NoExp","feat":["ha"]}""";
        LicensePayload result = validator.validate(sign(payload));
        assertThat(result.valid()).isTrue();
        assertThat(result.expires()).isNull();
    }

    @Test
    void hasFeatureFalseWhenInvalid() {
        assertThat(LicensePayload.INVALID.hasFeature("sso")).isFalse();
        assertThat(LicensePayload.INVALID.valid()).isFalse();
        assertThat(LicensePayload.INVALID.features()).isEmpty();
    }
}
