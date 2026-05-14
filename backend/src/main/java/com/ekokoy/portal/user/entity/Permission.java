package com.ekokoy.portal.user.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }

    public void setId(UUID id) { this.id = id; }
    public void setCode(String code) { this.code = code; }
    public void setCategory(String category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }
}
