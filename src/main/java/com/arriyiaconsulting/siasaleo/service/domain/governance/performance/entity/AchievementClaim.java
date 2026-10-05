package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for achievement_claim; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "achievement_claim")
public class AchievementClaim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "officeholder_id", nullable = false)
    private Long officeholderId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    public AchievementClaim() {}

    public Long getId() { return id; }

    public Long getOfficeholderId() { return officeholderId; }
    public void setOfficeholderId(Long value) { officeholderId = value; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { createdAt = value; }
}
