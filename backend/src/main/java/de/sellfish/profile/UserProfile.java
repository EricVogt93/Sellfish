package de.sellfish.profile;

import de.sellfish.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "user_profile")
public class UserProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "org_id")
    private UUID orgId;

    private String headline;

    @Column(columnDefinition = "text")
    private String summary;

    private String location;

    @Column(name = "willingness_to_relocate", nullable = false)
    private boolean willingnessToRelocate = false;

    @Column(name = "salary_min")
    private Integer salaryMin;

    @Column(name = "remote_pref", nullable = false)
    private String remotePref = "ANY";

    private String availability;

    /** Freie Meta-Informationen als JSON-String. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String meta = "{}";

    protected UserProfile() {
    }

    public UserProfile(UUID userId) {
        this.userId = userId;
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

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isWillingnessToRelocate() {
        return willingnessToRelocate;
    }

    public void setWillingnessToRelocate(boolean willingnessToRelocate) {
        this.willingnessToRelocate = willingnessToRelocate;
    }

    public Integer getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(Integer salaryMin) {
        this.salaryMin = salaryMin;
    }

    public String getRemotePref() {
        return remotePref;
    }

    public void setRemotePref(String remotePref) {
        this.remotePref = remotePref;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }
}
