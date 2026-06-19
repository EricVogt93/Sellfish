package de.sellfish.jobs.adapter.source;

import de.sellfish.jobs.port.RawJob;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Erzeugt stabile Fingerprints zur Deduplizierung von Stellen über Quellen hinweg.
 */
public final class Fingerprints {

    private Fingerprints() {}

    public static String of(RawJob job) {
        String basis;
        if (job.externalRef() != null && !job.externalRef().isBlank()) {
            basis = job.sourceCode() + "|" + job.externalRef();
        } else {
            basis = normalize(job.title()) + "|" + normalize(job.company()) + "|" + normalize(job.location());
        }
        return sha256(basis);
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
