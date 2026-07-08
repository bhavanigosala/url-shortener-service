package com.urlshortener.domain;

import java.time.Instant;

/**
 * Response Record for shortened URL operations
 * Modern Java feature: Record with computed accessors
 */
public record ShortenUrlResponse(
    String shortCode,
    String originalUrl,
    String shortUrl,
    Instant createdAt,
    Instant expiresAt,
    long clicks
) {
    /**
     * Compact constructor for validation
     */
    public ShortenUrlResponse {
        if (shortCode == null || shortCode.isBlank()) {
            throw new IllegalArgumentException("Short code cannot be null or blank");
        }
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new IllegalArgumentException("Original URL cannot be null or blank");
        }
    }
}
