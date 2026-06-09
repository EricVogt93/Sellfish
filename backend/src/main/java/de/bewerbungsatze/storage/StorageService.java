package de.bewerbungsatze.storage;

import java.util.UUID;

/**
 * Abstraktion über den Datei-Storage (lokales FS oder MinIO/S3).
 */
public interface StorageService {

    void store(String key, byte[] content, String contentType);

    byte[] load(String key);

    void delete(String key);

    boolean exists(String key);

    /**
     * Erzeugt einen kollisionsfreien, nutzer-gescopten Storage-Key.
     */
    default String newKey(UUID userId, String category, String filename) {
        String safe = filename == null ? "file"
                : filename.replaceAll("[^A-Za-z0-9._-]", "_");
        return "%s/%s/%s-%s".formatted(userId, category, UUID.randomUUID(), safe);
    }
}
