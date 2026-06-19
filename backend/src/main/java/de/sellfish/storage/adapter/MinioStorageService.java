package de.sellfish.storage.adapter;
import de.sellfish.storage.port.StorageService;

import de.sellfish.common.config.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * MinIO-/S3-kompatibler Storage ({@code app.storage.backend=minio}).
 */
@Service
@ConditionalOnProperty(name = "app.storage.backend", havingValue = "minio")
public class MinioStorageService implements StorageService {

    private final MinioClient client;
    private final String bucket;

    public MinioStorageService(StorageProperties properties) {
        StorageProperties.Minio cfg = properties.minio();
        this.bucket = cfg.bucket();
        this.client = MinioClient.builder()
                .endpoint(cfg.endpoint())
                .credentials(cfg.accessKey(), cfg.secretKey())
                .build();
    }

    @PostConstruct
    void ensureBucket() {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new IllegalStateException("MinIO bucket not initializable: " + bucket, e);
        }
    }

    @Override
    public void store(String key, byte[] content, String contentType) {
        try (InputStream in = new ByteArrayInputStream(content)) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(in, content.length, -1)
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("MinIO-Save failed: " + key, e);
        }
    }

    @Override
    public byte[] load(String key) {
        try (InputStream in = client.getObject(GetObjectArgs.builder()
                .bucket(bucket).object(key).build())) {
            return in.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("MinIO-Read failed: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            throw new IllegalStateException("MinIO-Delete failed: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            client.statObject(StatObjectArgs.builder().bucket(bucket).object(key).build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        } catch (Exception e) {
            throw new IllegalStateException("MinIO-Status check failed: " + key, e);
        }
    }
}
