package com.urlshortener.analytics;

import java.time.Instant; /**
 * Analytics summary record
 */
public record UrlAnalytics(
    String shortCode,
    long totalClicks,
    long uniqueClicks,
    Instant lastClickAt,
    String topReferer,
    String topCountry,
    double averageClicksPerDay
) {}
