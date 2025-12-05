package com.outreach.lead.domain.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "icp_profiles")
public class ICPProfileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "target_industry", length = 150)
    private String targetIndustry;

    @Column(name = "target_titles", columnDefinition = "TEXT")
    private String targetTitles;

    @Column(name = "company_size_min")
    private Integer companySizeMin;

    @Column(name = "company_size_max")
    private Integer companySizeMax;

    @Column(name = "geo_region", length = 200)
    private String geoRegion;

    @Column(name = "pain_points", columnDefinition = "TEXT")
    private String painPoints;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTargetIndustry() { return targetIndustry; }
    public void setTargetIndustry(String targetIndustry) { this.targetIndustry = targetIndustry; }

    public String getTargetTitles() { return targetTitles; }
    public void setTargetTitles(String targetTitles) { this.targetTitles = targetTitles; }

    public Integer getCompanySizeMin() { return companySizeMin; }
    public void setCompanySizeMin(Integer companySizeMin) { this.companySizeMin = companySizeMin; }

    public Integer getCompanySizeMax() { return companySizeMax; }
    public void setCompanySizeMax(Integer companySizeMax) { this.companySizeMax = companySizeMax; }

    public String getGeoRegion() { return geoRegion; }
    public void setGeoRegion(String geoRegion) { this.geoRegion = geoRegion; }

    public String getPainPoints() { return painPoints; }
    public void setPainPoints(String painPoints) { this.painPoints = painPoints; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

