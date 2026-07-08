package com.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * URL Shortener Service - Main Application
 *
 * Uses modern Java features:
 * - Records for immutable data
 * - Sealed classes for bounded polymorphism
 * - Pattern matching for exhaustive checks
 * - Virtual threads for scalable async processing
 * - Text blocks for SQL and multi-line strings
 */
@SpringBootApplication
@EnableAsync
@EnableAspectJAutoProxy
public class UrlShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}
