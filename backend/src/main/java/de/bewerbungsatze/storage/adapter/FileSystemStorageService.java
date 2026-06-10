package de.bewerbungsatze.storage.adapter;
import de.bewerbungsatze.storage.port.StorageService;

import de.bewerbungsatze.common.config.StorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Lokaler Dateisystem-Storage. Default-Backend ({@code app.storage.backend=fs}).
 */
@Service
@ConditionalOnProperty(name = "app.storage.backend", havingValue = "fs", matchIfMissing = true)
public class FileSystemStorageService implements StorageService {

    private final Path root;

    public FileSystemStorageService(StorageProperties properties) {
        this.root = Paths.get(properties.fsRoot() == null ? "./data/storage" : properties.fsRoot())
                .toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Storage-Verzeichnis nicht anlegbar: " + root, e);
        }
    }

    private Path resolve(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Ungültiger Storage-Key: " + key);
        }
        return target;
    }

    @Override
    public void store(String key, byte[] content, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Speichern fehlgeschlagen: " + key, e);
        }
    }

    @Override
    public byte[] load(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Lesen fehlgeschlagen: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Löschen fehlgeschlagen: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolve(key));
    }
}
