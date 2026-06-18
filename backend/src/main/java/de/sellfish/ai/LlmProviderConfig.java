package de.sellfish.ai;

import de.sellfish.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Provider-Konfiguration. {@code userId == null} ⇒ globale (Admin-)Konfiguration.
 */
@Entity
@Table(name = "llm_provider_config")
public class LlmProviderConfig extends BaseEntity {

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @Column(nullable = false)
    private String model;

    @Column(name = "base_url")
    private String baseUrl;

    /** Infisical-Secret-Pfad für zentrale Keys (z. B. {@code /llm/openai-key}). */
    @Column(name = "key_ref")
    private String keyRef;

    /** AES-GCM-verschlüsselter Per-User-Key. */
    @Column(name = "key_enc")
    private String keyEnc;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String params = "{}";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Purpose purpose = Purpose.CHAT;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(nullable = false)
    private boolean enabled = true;

    protected LlmProviderConfig() {
    }

    public LlmProviderConfig(UUID userId, Provider provider, String model, Purpose purpose) {
        this.userId = userId;
        this.provider = provider;
        this.model = model;
        this.purpose = purpose;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getKeyRef() {
        return keyRef;
    }

    public void setKeyRef(String keyRef) {
        this.keyRef = keyRef;
    }

    public String getKeyEnc() {
        return keyEnc;
    }

    public void setKeyEnc(String keyEnc) {
        this.keyEnc = keyEnc;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public Purpose getPurpose() {
        return purpose;
    }

    public void setPurpose(Purpose purpose) {
        this.purpose = purpose;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
