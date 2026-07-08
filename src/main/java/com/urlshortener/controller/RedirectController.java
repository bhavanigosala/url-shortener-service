package com.urlshortener.controller;

import com.urlshortener.domain.ClickEvent;
import com.urlshortener.domain.ShortenUrlResponse;
import com.urlshortener.service.UrlShortenerService;
import com.urlshortener.service.ClickAnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;

/**
 * Redirect Controller for shortened URLs
 * GET /s/{code} - Redirects to original URL and records analytics
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class RedirectController {

    private final UrlShortenerService urlShortenerService;
    private final ClickAnalyticsService analyticsService;

    public RedirectController(UrlShortenerService urlShortenerService,
                            ClickAnalyticsService analyticsService) {
        this.urlShortenerService = urlShortenerService;
        this.analyticsService = analyticsService;
    }

    /**
     * Redirect to original URL
     * GET /s/{code}
     */
    @GetMapping("/s/{code}")
    public ResponseEntity<Void> redirect(
            @PathVariable String code,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestHeader(value = "Referer", required = false) String referer) {
        
        log.info("Redirect request for short code: {}", code);

        Optional<ShortenUrlResponse> urlResponse = urlShortenerService.resolveUrl(code);

        if (urlResponse.isPresent()) {
            String originalUrl = urlResponse.get().originalUrl();
            
            // Record click event asynchronously
            recordClickEvent(code, userAgent, referer);

            log.info("Redirecting to: {}", originalUrl);
            
            // HTTP 301 Permanent Redirect
            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .header("Location", originalUrl)
                .build();
        } else {
            log.warn("Short code not found or expired: {}", code);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Record click event with metadata
     */
    private void recordClickEvent(String shortCode, String userAgent, String referer) {
        try {
            ClickEvent event = new ClickEvent(
                shortCode,
                Instant.now(),
                userAgent != null ? userAgent : "Unknown",
                referer != null ? referer : "Direct",
                "0.0.0.0",  // In production, get actual IP from request
                "Unknown",   // In production, use GeoIP service
                "Unknown"    // In production, use GeoIP service
            );
            
            analyticsService.recordClickEvent(event);
        } catch (Exception e) {
            log.error("Error recording click event", e);
            // Don't fail redirect on analytics error
        }
    }
}
