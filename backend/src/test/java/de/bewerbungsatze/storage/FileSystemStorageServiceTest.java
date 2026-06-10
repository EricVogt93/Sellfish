package de.bewerbungsatze.storage;
import de.bewerbungsatze.storage.port.StorageService;
import de.bewerbungsatze.storage.adapter.FileSystemStorageService;

import de.bewerbungsatze.common.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSystemStorageServiceTest {

    private StorageService newService(Path root) {
        return new FileSystemStorageService(
                new StorageProperties("fs", root.toString(), null));
    }

    @Test
    void storeLoadDeleteRoundTrip(@TempDir Path tmp) {
        StorageService storage = newService(tmp);
        byte[] data = "Lebenslauf".getBytes(StandardCharsets.UTF_8);
        String key = storage.newKey(UUID.randomUUID(), "cv", "cv.pdf");

        storage.store(key, data, "application/pdf");
        assertThat(storage.exists(key)).isTrue();
        assertThat(storage.load(key)).isEqualTo(data);

        storage.delete(key);
        assertThat(storage.exists(key)).isFalse();
    }

    @Test
    void newKeySanitizesFilename() {
        StorageService storage = newService(Path.of(System.getProperty("java.io.tmpdir")));
        String key = storage.newKey(UUID.randomUUID(), "cv", "mein lebenslauf!.pdf");
        assertThat(key).contains("mein_lebenslauf_.pdf");
    }

    @Test
    void rejectsPathTraversal(@TempDir Path tmp) {
        StorageService storage = newService(tmp);
        assertThatThrownBy(() -> storage.load("../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
