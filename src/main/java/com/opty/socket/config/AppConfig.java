/**
 * Spring Boot configuration.
 */

package com.opty.socket.config;

/**
 * IMPORTS
 */
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;


/**
 * CODE
 */

/**
 * Configuration properties for Opty Server.
 *
 * Binds custom properties from application.yml.
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "opty.socket")
public class AppConfig {

    // --- ATRIBUTES ---
    // Maximum allowed concurrent WebSocket connections
    @Positive(message = "Max connections must be positive")
    @Max(value = 10000, message = "Max connections cannot exceed 10000")
    private int maxConnections = 100;
    private final MessageConfig message = new MessageConfig();
    private final CorsConfig cors = new CorsConfig();
    private final SessionConfig session = new SessionConfig();

    /**
     * Message-related configuration.
     */
    @Data
    public static class MessageConfig {
        // --- ATRIBUTES ---
        @Positive(message = "Max message size must be positive")
        @Max(value = 1048576, message = "Max message size cannot exceed 1MB")
        private int maxSize = 65536;

        @Positive(message = "Queue capacity must be positive")
        @Max(value = 1000, message = "Queue capacity cannot exceed 1000")
        private int queueCapacity = 100;
    }

    /**
     * Session-related configuration.
     */
    @Data
    public static class SessionConfig {
        // --- ATRIBUTES ---
        @Positive(message = "Session timeout must be positive")
        @Max(value = 1440, message = "Session timeout cannot exceed 24 hours")
        private int timeoutMinutes = 30;
    }

    /**
     * CORS configuration for WebSocket endpoints.
     */
    @Data
    public static class CorsConfig {
        // --- ATRIBUTES ---
        private String allowedOrigins = "*";
    }
}
