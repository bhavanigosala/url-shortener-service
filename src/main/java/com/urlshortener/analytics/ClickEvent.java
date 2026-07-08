package com.urlshortener.domain;

import java.time.Instant;

/**
 * Analytics Records - demonstrates record usage for immutable data
 */
public record ClickEvent(
    String shortCode,
    Instant clickedAt,
    String userAgent,
    String referer,
    String ipAddress,
    String country,
    String city
) {}

