package com.urlshortener.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Request Record for creating shortened URLs
 * Modern Java feature: Record provides immutable data carrier with toString, equals, hashCode
 */
public record ShortenUrlRequest(
    String originalUrl,
    String customAlias,
    Instant expiresAt
) {
    public ShortenUrlRequest {
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new IllegalArgumentException("Original URL cannot be null or blank");
        }
    }

    /**
     * Constructor with defaults
     */
    public ShortenUrlRequest(String originalUrl) {
        this(originalUrl, null, null);
    }
}
