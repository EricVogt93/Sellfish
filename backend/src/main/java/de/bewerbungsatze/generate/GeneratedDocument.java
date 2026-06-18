package de.bewerbungsatze.generate;

import de.bewerbungsatze.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "generated_documents")
public class GeneratedDocument extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "job_match_id")
    private UUID jobMatchId;

    @Column(name = "org_id")
    private UUID orgId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GenerationType type;

    @Column(columnDefinition = "text")
    private String content;

    private String model;

    @Column(name = "prompt_version")
    private String promptVersion;

    @Column(name = "storage_key")
    private String storageKey;

    @Column(nullable = false)
    private int version = 1;

    protected GeneratedDocument() {
    }

    public GeneratedDocument(UUID userId, UUID jobMatchId, GenerationType type) {
        this.userId = userId;
        this.jobMatchId = jobMatchId;
        this.type = type;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getJobMatchId() {
        return jobMatchId;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public GenerationType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
