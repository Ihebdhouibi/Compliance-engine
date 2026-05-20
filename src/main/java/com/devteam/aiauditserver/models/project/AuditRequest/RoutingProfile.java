package com.devteam.aiauditserver.models.project.AuditRequest;

import com.devteam.aiauditserver.enums.Project.Pathway;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.util.Date;

/**
 * Output of the Level-1 RoutingEngine for a single AuditRequest. Holds the
 * deterministic categorisation of the firm plus the recommended package,
 * active L2 modules, evidence depth and indicative price band.
 */
@Entity
@Table(name = "routing_profiles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_routing_profile_audit_request",
                columnNames = {"audit_request_id"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RoutingProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_request_id", nullable = false)
    private AuditRequest auditRequest;

    @Column(name = "firm_size_category", length = 40)
    private String firmSizeCategory;

    @Column(name = "sector_category", length = 120)
    private String sectorCategory;

    @Column(name = "ai_adoption_category", length = 40)
    private String aiAdoptionCategory;

    @Column(name = "ai_impact_category", length = 40)
    private String aiImpactCategory;

    @Column(name = "data_sensitivity_category", length = 40)
    private String dataSensitivityCategory;

    @Column(name = "regulatory_exposure_category", length = 40)
    private String regulatoryExposureCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "pathway", length = 30)
    private Pathway pathway;

    @Column(name = "recommended_package", length = 80)
    private String recommendedPackage;

    /** Comma-separated module names enabled for this audit. */
    @Column(name = "active_modules", columnDefinition = "TEXT")
    private String activeModules;

    @Column(name = "evidence_depth", length = 20)
    private String evidenceDepth;

    @Column(name = "indicative_price_min")
    private Integer indicativePriceMin;

    @Column(name = "indicative_price_max")
    private Integer indicativePriceMax;

    @Column(name = "price_currency", length = 8)
    private String priceCurrency = "GBP";

    @Column(name = "price_status", length = 20)
    private String priceStatus = "INDICATIVE";

    /** Free-text reason / explanation. */
    @Column(name = "rationale", columnDefinition = "TEXT")
    private String rationale;

    @Column(name = "computed_at")
    private Date computedAt = new Date();

    @Column(name = "confirmed_at")
    private Date confirmedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AuditRequest getAuditRequest() { return auditRequest; }
    public void setAuditRequest(AuditRequest auditRequest) { this.auditRequest = auditRequest; }

    public String getFirmSizeCategory() { return firmSizeCategory; }
    public void setFirmSizeCategory(String firmSizeCategory) { this.firmSizeCategory = firmSizeCategory; }

    public String getSectorCategory() { return sectorCategory; }
    public void setSectorCategory(String sectorCategory) { this.sectorCategory = sectorCategory; }

    public String getAiAdoptionCategory() { return aiAdoptionCategory; }
    public void setAiAdoptionCategory(String aiAdoptionCategory) { this.aiAdoptionCategory = aiAdoptionCategory; }

    public String getAiImpactCategory() { return aiImpactCategory; }
    public void setAiImpactCategory(String aiImpactCategory) { this.aiImpactCategory = aiImpactCategory; }

    public String getDataSensitivityCategory() { return dataSensitivityCategory; }
    public void setDataSensitivityCategory(String dataSensitivityCategory) { this.dataSensitivityCategory = dataSensitivityCategory; }

    public String getRegulatoryExposureCategory() { return regulatoryExposureCategory; }
    public void setRegulatoryExposureCategory(String regulatoryExposureCategory) { this.regulatoryExposureCategory = regulatoryExposureCategory; }

    public Pathway getPathway() { return pathway; }
    public void setPathway(Pathway pathway) { this.pathway = pathway; }

    public String getRecommendedPackage() { return recommendedPackage; }
    public void setRecommendedPackage(String recommendedPackage) { this.recommendedPackage = recommendedPackage; }

    public String getActiveModules() { return activeModules; }
    public void setActiveModules(String activeModules) { this.activeModules = activeModules; }

    public String getEvidenceDepth() { return evidenceDepth; }
    public void setEvidenceDepth(String evidenceDepth) { this.evidenceDepth = evidenceDepth; }

    public Integer getIndicativePriceMin() { return indicativePriceMin; }
    public void setIndicativePriceMin(Integer indicativePriceMin) { this.indicativePriceMin = indicativePriceMin; }

    public Integer getIndicativePriceMax() { return indicativePriceMax; }
    public void setIndicativePriceMax(Integer indicativePriceMax) { this.indicativePriceMax = indicativePriceMax; }

    public String getPriceCurrency() { return priceCurrency; }
    public void setPriceCurrency(String priceCurrency) { this.priceCurrency = priceCurrency; }

    public String getPriceStatus() { return priceStatus; }
    public void setPriceStatus(String priceStatus) { this.priceStatus = priceStatus; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public Date getComputedAt() { return computedAt; }
    public void setComputedAt(Date computedAt) { this.computedAt = computedAt; }

    public Date getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Date confirmedAt) { this.confirmedAt = confirmedAt; }
}
