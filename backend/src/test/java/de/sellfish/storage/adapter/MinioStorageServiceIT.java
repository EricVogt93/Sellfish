package de.sellfish.storage.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import de.sellfish.common.config.StorageProperties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class MinioStorageServiceIT {

    @Container
    static final GenericContainer<?> MINIO = new GenericContainer<>("minio/minio:latest")
            .withCommand("server", "/data")
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withExposedPorts(9000);

    private MinioStorageService service() {
        String endpoint = "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
        StorageProperties props = new StorageProperties(
                "minio", null, new StorageProperties.Minio(endpoint, "minioadmin", "minioadmin", "sellfish-test"));
        MinioStorageService service = new MinioStorageService(props);
        service.ensureBucket(); // @PostConstruct equivalent
        return service;
    }

    @Test
    void storeLoadDeleteRoundTrip() {
        MinioStorageService storage = service();
        String key = "cv/" + UUID.randomUUID() + "/cv.pdf";
        byte[] content = "hello sellfish".getBytes();

        assertThat(storage.exists(key)).isFalse();
        storage.store(key, content, "application/pdf");
        assertThat(storage.exists(key)).isTrue();
        assertThat(storage.load(key)).isEqualTo(content);

        storage.delete(key);
        assertThat(storage.exists(key)).isFalse();
    }

    @Test
    void storeDefaultsContentTypeWhenNull() {
        MinioStorageService storage = service();
        String key = "doc/" + UUID.randomUUID() + "/f.bin";
        storage.store(key, "x".getBytes(), null);
        assertThat(storage.exists(key)).isTrue();
        assertThat(storage.load(key)).isEqualTo("x".getBytes());
    }
}
