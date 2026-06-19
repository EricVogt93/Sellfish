package de.sellfish.enterprise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

/**
 * Validiert einen RSA-signierten License key offline.
 *
 * <p>Format des License-Keys (Base64): {@code payload || "::" || signature}
 * wobei der Payload selbst eine JSON-Zeichenkette ist:
 * <pre>{@code
 *   {"sub":"Acme Corp","exp":"2026-12-31T23:59:59Z","feat":["sso","multi-tenant","audit","reports","ha"]}
 * }</pre>
 */
public class LicenseValidator {

    private static final Logger log = LoggerFactory.getLogger(LicenseValidator.class);

    private final byte[] publicKeyBytes;

    public LicenseValidator(String publicKeyBase64) {
        this.publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64.strip());
    }

    public LicensePayload validate(String licenseKey) {
        if (licenseKey == null || licenseKey.isBlank()) {
            return LicensePayload.INVALID;
        }
        int sep = licenseKey.lastIndexOf("::");
        if (sep < 0) {
            return LicensePayload.INVALID;
        }
        String payload = licenseKey.substring(0, sep);
        String signatureB64 = licenseKey.substring(sep + 2);
        try {
            if (!verify(payload, signatureB64)) {
                return LicensePayload.INVALID;
            }
            return LicensePayload.parse(payload);
        } catch (Exception e) {
            log.warn("License-Validierung failed: {}", e.getMessage());
            return LicensePayload.INVALID;
        }
    }

    private boolean verify(String payload, String signatureB64) throws Exception {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(kf.generatePublic(spec));
        sig.update(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return sig.verify(Base64.getDecoder().decode(signatureB64));
    }

    /**
     * Geparste License-Nutzdaten nach erfolgreicher Signatur-Prüfung.
     */
    public static class LicensePayload {
        public static final LicensePayload INVALID = new LicensePayload(false, null, null, Set.of());

        private final boolean valid;
        private final String subject;
        private final Instant expires;
        private final Set<String> features;

        LicensePayload(boolean valid, String subject, Instant expires, Set<String> features) {
            this.valid = valid;
            this.subject = subject;
            this.expires = expires;
            this.features = features;
        }

        public boolean valid() { return valid && (expires == null || expires.isAfter(Instant.now())); }
        public String subject() { return subject; }
        public Instant expires() { return expires; }
        public Set<String> features() { return features; }
        public boolean hasFeature(String feature) { return valid() && features.contains(feature); }

        static LicensePayload parse(String json) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(json);
                String sub = node.has("sub") ? node.get("sub").asText() : null;
                Instant exp = node.has("exp") ? Instant.parse(node.get("exp").asText()) : null;
                Set<String> feats = new java.util.LinkedHashSet<>();
                if (node.has("feat")) {
                    for (var f : node.get("feat")) {
                        feats.add(f.asText());
                    }
                }
                return new LicensePayload(true, sub, exp, feats);
            } catch (Exception e) {
                return INVALID;
            }
        }
    }
}
