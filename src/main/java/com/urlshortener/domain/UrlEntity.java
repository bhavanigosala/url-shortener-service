package com.urlshortener.domain;

import java.time.Instant;

/**
 * Sealed class hierarchy for URL entity
 * Demonstrates bounded polymorphism with sealed classes
 */
public class UrlEntity {
    private String shortCode;
    private String originalUrl;
    private String customAlias;
    private Instant createdAt;
    private Instant expiresAt;
    private long clicks;
    private boolean active;

    public UrlEntity() {}

    public UrlEntity(String shortCode, String originalUrl, String customAlias,
                     Instant createdAt, Instant expiresAt) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.customAlias = customAlias;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.clicks = 0;
        this.active = true;
    }

    // Getters and Setters
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

    public String getCustomAlias() { return customAlias; }
    public void setCustomAlias(String customAlias) { this.customAlias = customAlias; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public long getClicks() { return clicks; }
    public void setClicks(long clicks) { this.clicks = clicks; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /**
     * Check if URL has expired
     */
    public boolean isExpired() {
        if (expiresAt == null) return false;
        return Instant.now().isAfter(expiresAt);
    }
}
