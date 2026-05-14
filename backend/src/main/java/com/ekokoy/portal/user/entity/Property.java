package com.ekokoy.portal.user.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "properties")
@EntityListeners(AuditingEntityListener.class)
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "unit_number", nullable = false, unique = true, length = 20)
    private String unitNumber;

    @Column(length = 20)
    private String block;

    @Column
    private Integer floor;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public String getUnitNumber() { return unitNumber; }
    public String getBlock() { return block; }
    public Integer getFloor() { return floor; }
    public boolean isActive() { return isActive; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(UUID id) { this.id = id; }
    public void setUnitNumber(String unitNumber) { this.unitNumber = unitNumber; }
    public void setBlock(String block) { this.block = block; }
    public void setFloor(Integer floor) { this.floor = floor; }
    public void setActive(boolean active) { isActive = active; }
}
