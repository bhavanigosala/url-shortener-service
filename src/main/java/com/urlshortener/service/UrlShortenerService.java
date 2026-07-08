package com.urlshortener.service;

import com.urlshortener.domain.ShortenUrlRequest;
import com.urlshortener.domain.ShortenUrlResponse;
import com.urlshortener.domain.UrlEntity;
import com.urlshortener.repository.UrlRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Core URL Shortener Service
 * 
 * Features:
 * - Short code generation using Base64 encoding
 * - Custom alias support
 * - Expiration tracking
 * - Click analytics
 * - Resilience patterns: circuit breaker, retry, rate limiting
 */
@Slf4j
@Service
public class UrlShortenerService {

    private final UrlRepository urlRepository;
    private final ClickAnalyticsService analyticsService;

    @Value("${url-shortener.base-url:http://localhost:8080/s}")
    private String baseUrl;

    @Value("${url-shortener.max-short-code-length:7}")
    private int shortCodeLength;

    public UrlShortenerService(UrlRepository urlRepository, ClickAnalyticsService analyticsService) {
        this.urlRepository = urlRepository;
        this.analyticsService = analyticsService;
    }

    /**
     * Shorten a URL with resilience patterns
     */
    @CircuitBreaker(name = "urlShortener", fallbackMethod = "shortenUrlFallback")
    @Retry(name = "urlShortener")
    @RateLimiter(name = "urlShortener")
    public ShortenUrlResponse shortenUrl(ShortenUrlRequest request) {
        log.info("Shortening URL: {}", request.originalUrl());

        // Use custom alias if provided, otherwise generate short code
        String shortCode = request.customAlias() != null && !request.customAlias().isBlank()
            ? request.customAlias()
            : generateShortCode();

        // Check if short code already exists
        if (urlRepository.existsByShortCode(shortCode)) {
            throw new IllegalArgumentException("Short code already exists: " + shortCode);
        }

        // Create entity
        UrlEntity entity = new UrlEntity(
            shortCode,
            request.originalUrl(),
            request.customAlias(),
            Instant.now(),
            request.expiresAt()
        );

        // Save to repository
        UrlEntity saved = urlRepository.save(entity);

        log.info("URL shortened successfully: {} -> {}", request.originalUrl(), shortCode);

        return new ShortenUrlResponse(
            saved.getShortCode(),
            saved.getOriginalUrl(),
            baseUrl + "/" + saved.getShortCode(),
            saved.getCreatedAt(),
            saved.getExpiresAt(),
            saved.getClicks()
        );
    }

    /**
     * Fallback method for shortenUrl if circuit breaker is open
     */
    public ShortenUrlResponse shortenUrlFallback(ShortenUrlRequest request, Exception ex) {
        log.warn("Circuit breaker open for shortenUrl, returning fallback response", ex);
        throw new RuntimeException("URL shortening service temporarily unavailable", ex);
    }

    /**
     * Resolve shortened URL with click tracking
     */
    @CircuitBreaker(name = "urlResolver", fallbackMethod = "resolveUrlFallback")
    public Optional<ShortenUrlResponse> resolveUrl(String shortCode) {
        log.debug("Resolving short code: {}", shortCode);

        return urlRepository.findByShortCode(shortCode)
            .filter(url -> !url.isExpired() && url.isActive())
            .map(url -> {
                // Increment click count asynchronously
                analyticsService.recordClickAsync(shortCode);
                
                return new ShortenUrlResponse(
                    url.getShortCode(),
                    url.getOriginalUrl(),
                    baseUrl + "/" + url.getShortCode(),
                    url.getCreatedAt(),
                    url.getExpiresAt(),
                    url.getClicks()
                );
            });
    }

    /**
     * Fallback for resolveUrl
     */
    public Optional<ShortenUrlResponse> resolveUrlFallback(String shortCode, Exception ex) {
        log.warn("Circuit breaker open for resolveUrl: {}", shortCode, ex);
        return Optional.empty();
    }

    /**
     * Get URL analytics
     */
    public Optional<ShortenUrlResponse> getUrlDetails(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
            .map(url -> new ShortenUrlResponse(
                url.getShortCode(),
                url.getOriginalUrl(),
                baseUrl + "/" + url.getShortCode(),
                url.getCreatedAt(),
                url.getExpiresAt(),
                url.getClicks()
            ));
    }

    /**
     * Delete a shortened URL
     */
    public void deleteUrl(String shortCode) {
        urlRepository.delete(shortCode);
        log.info("Deleted URL with short code: {}", shortCode);
    }

    /**
     * Generate short code from UUID
     * Uses Base64 encoding for compact representation
     */
    private String generateShortCode() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        byte[] encoded = Base64.getUrlEncoder()
            .encode(uuid.getBytes());
        byte[] sliced = Arrays.copyOfRange(encoded, 0, Math.min(shortCodeLength, encoded.length));
        
        return new String(sliced).toLowerCase();
    }
}
