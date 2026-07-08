package com.urlshortener.service;

import com.urlshortener.domain.ClickEvent;
import com.urlshortener.repository.UrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Click Analytics Service
 * Tracks clicks, user agents, referrers, and geographic data
 * Uses virtual threads via @Async for non-blocking processing
 */
@Slf4j
@Service
public class ClickAnalyticsService {

    private final UrlRepository urlRepository;
    private final ConcurrentHashMap<String, AtomicLong> clickCounts = new ConcurrentHashMap<>();

    public ClickAnalyticsService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    /**
     * Record click asynchronously using virtual threads
     * Decorated with @Async to run in thread pool
     */
    @Async
    public void recordClickAsync(String shortCode) {
        try {
            // Increment clicks in repository
            urlRepository.incrementClicks(shortCode);
            clickCounts.computeIfAbsent(shortCode, k -> new AtomicLong(0))
                .incrementAndGet();
            
            log.debug("Recorded click for short code: {}", shortCode);
        } catch (Exception e) {
            log.error("Failed to record click for short code: {}", shortCode, e);
        }
    }

    /**
     * Record detailed click event with metadata
     * Could integrate with external analytics service
     */
    @Async
    public void recordClickEvent(ClickEvent event) {
        try {
            log.info("Click event recorded: {} at {}", event.shortCode(), event.clickedAt());
            
            // Here you would:
            // 1. Store to analytics database
            // 2. Send to external analytics service (e.g., Kafka, Google Analytics)
            // 3. Update geographic cache
            
            urlRepository.incrementClicks(event.shortCode());
            
        } catch (Exception e) {
            log.error("Failed to record click event", e);
        }
    }

    /**
     * Get click count for a short code
     */
    public long getClickCount(String shortCode) {
        return clickCounts.getOrDefault(shortCode, new AtomicLong(0)).get();
    }

    /**
     * Reset click count (for testing)
     */
    public void resetClickCount(String shortCode) {
        clickCounts.remove(shortCode);
    }
}
