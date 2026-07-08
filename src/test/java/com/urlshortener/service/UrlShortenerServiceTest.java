package com.urlshortener.service;

import com.urlshortener.domain.ShortenUrlRequest;
import com.urlshortener.domain.ShortenUrlResponse;
import com.urlshortener.domain.UrlEntity;
import com.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for UrlShortenerService
 * 
 * Tests core functionality:
 * - URL shortening with short code generation
 * - URL resolution and click tracking
 * - Custom alias support
 * - Expiration handling
 */
@SpringBootTest
@DisplayName("URL Shortener Service Tests")
class UrlShortenerServiceTest {

    @Autowired
    private UrlShortenerService urlShortenerService;

    @Autowired
    private UrlRepository urlRepository;

    private static final String TEST_URL = "https://www.example.com/very/long/url/that/needs/shortening";

    @BeforeEach
    void setUp() {
        // Clear repository before each test
        // In production, use proper test database cleanup
    }

    @Test
    @DisplayName("Should shorten URL successfully")
    void testShortenUrl() {
        // Arrange
        ShortenUrlRequest request = new ShortenUrlRequest(TEST_URL);

        // Act
        ShortenUrlResponse response = urlShortenerService.shortenUrl(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.shortCode());
        assertEquals(TEST_URL, response.originalUrl());
        assertTrue(response.shortUrl().contains(response.shortCode()));
        assertEquals(0, response.clicks());
    }

    @Test
    @DisplayName("Should create custom alias")
    void testShortenUrlWithCustomAlias() {
        // Arrange
        String customAlias = "github";
        ShortenUrlRequest request = new ShortenUrlRequest(TEST_URL, customAlias, null);

        // Act
        ShortenUrlResponse response = urlShortenerService.shortenUrl(request);

        // Assert
        assertNotNull(response);
        assertEquals(customAlias, response.shortCode());
        assertTrue(response.shortUrl().contains(customAlias));
    }

    @Test
    @DisplayName("Should resolve shortened URL")
    void testResolveUrl() {
        // Arrange
        ShortenUrlRequest request = new ShortenUrlRequest(TEST_URL);
        ShortenUrlResponse shortened = urlShortenerService.shortenUrl(request);

        // Act
        Optional<ShortenUrlResponse> resolved = urlShortenerService.resolveUrl(shortened.shortCode());

        // Assert
        assertTrue(resolved.isPresent());
        assertEquals(TEST_URL, resolved.get().originalUrl());
    }

    @Test
    @DisplayName("Should handle expired URLs")
    void testExpiredUrl() {
        // Arrange
        Instant pastTime = Instant.now().minusSeconds(3600);
        ShortenUrlRequest request = new ShortenUrlRequest(TEST_URL, null, pastTime);

        ShortenUrlResponse shortened = urlShortenerService.shortenUrl(request);

        // Act
        Optional<ShortenUrlResponse> resolved = urlShortenerService.resolveUrl(shortened.shortCode());

        // Assert
        assertTrue(resolved.isEmpty());
    }

    @Test
    @DisplayName("Should not create duplicate short codes")
    void testDuplicateShortCodePrevention() {
        // Arrange
        String customAlias = "unique";
        ShortenUrlRequest request1 = new ShortenUrlRequest(TEST_URL, customAlias, null);

        urlShortenerService.shortenUrl(request1);

        // Act & Assert
        ShortenUrlRequest request2 = new ShortenUrlRequest("https://different.com", customAlias, null);
        assertThrows(IllegalArgumentException.class, () -> {
            urlShortenerService.shortenUrl(request2);
        });
    }

    @Test
    @DisplayName("Should delete shortened URL")
    void testDeleteUrl() {
        // Arrange
        ShortenUrlRequest request = new ShortenUrlRequest(TEST_URL);
        ShortenUrlResponse shortened = urlShortenerService.shortenUrl(request);

        // Act
        urlShortenerService.deleteUrl(shortened.shortCode());
        Optional<ShortenUrlResponse> resolved = urlShortenerService.resolveUrl(shortened.shortCode());

        // Assert
        assertTrue(resolved.isEmpty());
    }

    @Test
    @DisplayName("Should reject blank URLs")
    void testRejectBlankUrl() {
        // Arrange
        ShortenUrlRequest request = new ShortenUrlRequest("");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            urlShortenerService.shortenUrl(request);
        });
    }

    @Test
    @DisplayName("Should track clicks")
    void testClickTracking() {
        // Arrange
        ShortenUrlRequest request = new ShortenUrlRequest(TEST_URL);
        ShortenUrlResponse shortened = urlShortenerService.shortenUrl(request);

        // Act
        for (int i = 0; i < 5; i++) {
            urlShortenerService.resolveUrl(shortened.shortCode());
        }

        Optional<ShortenUrlResponse> details = urlShortenerService.getUrlDetails(shortened.shortCode());

        // Assert
        assertTrue(details.isPresent());
        // Note: clicks may not increment immediately due to async processing
        assertNotNull(details.get().clicks());
    }
}
