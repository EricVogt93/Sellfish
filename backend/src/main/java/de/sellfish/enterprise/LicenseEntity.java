package de.sellfish.enterprise;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "license")
public class LicenseEntity {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "license_key", nullable = false)
    private String licenseKey;

    @Column(name = "issued_to")
    private String issuedTo;

    @Column(name = "valid_until")
    private Instant validUntil;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String features = "{}";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected LicenseEntity() {}

    public LicenseEntity(String licenseKey, String issuedTo, Instant validUntil, String features) {
        this.licenseKey = licenseKey;
        this.issuedTo = issuedTo;
        this.validUntil = validUntil;
        this.features = features;
    }

    public UUID getId() { return id; }
    public String getLicenseKey() { return licenseKey; }
    public String getIssuedTo() { return issuedTo; }
    public Instant getValidUntil() { return validUntil; }
    public String getFeatures() { return features; }
    public Instant getCreatedAt() { return createdAt; }
}
