package com.urlshortener.repository;

import com.urlshortener.domain.UrlEntity;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of UrlRepository
 * Thread-safe using ConcurrentHashMap
 * Production would use Spring Data JPA with PostgreSQL
 */
@Repository
public class InMemoryUrlRepository implements UrlRepository {
    
    private final ConcurrentHashMap<String, UrlEntity> shortCodeIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UrlEntity> customAliasIndex = new ConcurrentHashMap<>();

    @Override
    public UrlEntity save(UrlEntity url) {
        if (url.getShortCode() == null || url.getShortCode().isBlank()) {
            throw new IllegalArgumentException("Short code cannot be null");
        }
        
        shortCodeIndex.put(url.getShortCode(), url);
        
        if (url.getCustomAlias() != null && !url.getCustomAlias().isBlank()) {
            customAliasIndex.put(url.getCustomAlias(), url);
        }
        
        return url;
    }

    @Override
    public Optional<UrlEntity> findByShortCode(String shortCode) {
        return Optional.ofNullable(shortCodeIndex.get(shortCode));
    }

    @Override
    public Optional<UrlEntity> findByCustomAlias(String alias) {
        return Optional.ofNullable(customAliasIndex.get(alias));
    }

    @Override
    public boolean existsByShortCode(String shortCode) {
        return shortCodeIndex.containsKey(shortCode);
    }

    @Override
    public void incrementClicks(String shortCode) {
        shortCodeIndex.computeIfPresent(shortCode, (key, url) -> {
            url.setClicks(url.getClicks() + 1);
            return url;
        });
    }

    @Override
    public void updateStatus(String shortCode, boolean active) {
        shortCodeIndex.computeIfPresent(shortCode, (key, url) -> {
            url.setActive(active);
            return url;
        });
    }

    @Override
    public void delete(String shortCode) {
        UrlEntity url = shortCodeIndex.remove(shortCode);
        if (url != null && url.getCustomAlias() != null) {
            customAliasIndex.remove(url.getCustomAlias());
        }
    }
}
