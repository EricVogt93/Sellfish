package de.bewerbungsatze.jobs;

import de.bewerbungsatze.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job extends BaseEntity {

    @Column(name = "source_code", nullable = false)
    private String sourceCode;

    @Column(name = "external_ref")
    private String externalRef;

    @Column(name = "org_id")
    private UUID orgId;

    @Column(nullable = false, unique = true)
    private String fingerprint;

    @Column(nullable = false)
    private String title;

    private String company;

    private String location;

    private String remote;

    @Column(columnDefinition = "text")
    private String description;

    private String url;

    @Column(name = "salary_raw")
    private String salaryRaw;

    @Column(name = "posted_at")
    private Instant postedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String raw;

    protected Job() {
    }

    public Job(String sourceCode, String fingerprint, String title) {
        this.sourceCode = sourceCode;
        this.fingerprint = fingerprint;
        this.title = title;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getRemote() {
        return remote;
    }

    public void setRemote(String remote) {
        this.remote = remote;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSalaryRaw() {
        return salaryRaw;
    }

    public void setSalaryRaw(String salaryRaw) {
        this.salaryRaw = salaryRaw;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(Instant postedAt) {
        this.postedAt = postedAt;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }
}
