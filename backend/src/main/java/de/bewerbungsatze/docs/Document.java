package de.bewerbungsatze.docs;

import de.bewerbungsatze.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "org_id")
    private UUID orgId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType type;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(nullable = false)
    private String filename;

    private String mime;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "parsed_text", columnDefinition = "text")
    private String parsedText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parsed_struct", columnDefinition = "jsonb")
    private String parsedStruct;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    protected Document() {
    }

    public Document(UUID userId, DocumentType type, String storageKey, String filename) {
        this.userId = userId;
        this.type = type;
        this.storageKey = storageKey;
        this.filename = filename;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public DocumentType getType() {
        return type;
    }

    public void setType(DocumentType type) {
        this.type = type;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getFilename() {
        return filename;
    }

    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getParsedText() {
        return parsedText;
    }

    public void setParsedText(String parsedText) {
        this.parsedText = parsedText;
    }

    public String getParsedStruct() {
        return parsedStruct;
    }

    public void setParsedStruct(String parsedStruct) {
        this.parsedStruct = parsedStruct;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}
