package de.sellfish.profile;

import de.sellfish.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_preferences")
public class UserPreferences extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "org_id")
    private UUID orgId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "desired_titles", columnDefinition = "text[]")
    private String[] desiredTitles = new String[0];

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private String[] industries = new String[0];

    @Column(name = "company_size")
    private String companySize;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "contract_types", columnDefinition = "text[]")
    private String[] contractTypes = new String[0];

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "excluded_companies", columnDefinition = "text[]")
    private String[] excludedCompanies = new String[0];

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private String[] keywords = new String[0];

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hard_filters", columnDefinition = "jsonb", nullable = false)
    private String hardFilters = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "soft_weights", columnDefinition = "jsonb", nullable = false)
    private String softWeights = "{}";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "preferred_countries", columnDefinition = "text[]")
    private String[] preferredCountries = new String[0];

    protected UserPreferences() {}

    public UserPreferences(UUID userId) {
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

    public String[] getDesiredTitles() {
        return desiredTitles;
    }

    public void setDesiredTitles(String[] desiredTitles) {
        this.desiredTitles = desiredTitles;
    }

    public String[] getIndustries() {
        return industries;
    }

    public void setIndustries(String[] industries) {
        this.industries = industries;
    }

    public String getCompanySize() {
        return companySize;
    }

    public void setCompanySize(String companySize) {
        this.companySize = companySize;
    }

    public String[] getContractTypes() {
        return contractTypes;
    }

    public void setContractTypes(String[] contractTypes) {
        this.contractTypes = contractTypes;
    }

    public String[] getExcludedCompanies() {
        return excludedCompanies;
    }

    public void setExcludedCompanies(String[] excludedCompanies) {
        this.excludedCompanies = excludedCompanies;
    }

    public String[] getKeywords() {
        return keywords;
    }

    public void setKeywords(String[] keywords) {
        this.keywords = keywords;
    }

    public String getHardFilters() {
        return hardFilters;
    }

    public void setHardFilters(String hardFilters) {
        this.hardFilters = hardFilters;
    }

    public String getSoftWeights() {
        return softWeights;
    }

    public void setSoftWeights(String softWeights) {
        this.softWeights = softWeights;
    }

    public String[] getPreferredCountries() {
        return preferredCountries;
    }

    public void setPreferredCountries(String[] preferredCountries) {
        this.preferredCountries = preferredCountries;
    }
}
