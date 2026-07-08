package com.urlshortener.repository;

import com.urlshortener.domain.UrlEntity;
import java.util.Optional;

/**
 * Repository interface for URL persistence
 */
public interface UrlRepository {
    /**
     * Save or update URL entity
     */
    UrlEntity save(UrlEntity url);

    /**
     * Find by short code
     */
    Optional<UrlEntity> findByShortCode(String shortCode);

    /**
     * Find by custom alias
     */
    Optional<UrlEntity> findByCustomAlias(String alias);

    /**
     * Check if short code exists
     */
    boolean existsByShortCode(String shortCode);

    /**
     * Increment click count
     */
    void incrementClicks(String shortCode);

    /**
     * Update URL status
     */
    void updateStatus(String shortCode, boolean active);

    /**
     * Delete URL
     */
    void delete(String shortCode);
}
