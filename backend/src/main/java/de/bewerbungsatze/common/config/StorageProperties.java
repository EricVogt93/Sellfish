package de.bewerbungsatze.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String backend,
        String fsRoot,
        Minio minio) {

    public record Minio(
            String endpoint,
            String accessKey,
            String secretKey,
            String bucket) {
    }
}
