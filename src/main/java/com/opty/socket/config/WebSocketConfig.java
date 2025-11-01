/**
 * WebSocket configuration.
 */

package com.opty.socket.config;

/**
 * IMPORTS
 */
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.opty.socket.websocket.ClientWebSocketHandler;
import com.opty.socket.websocket.SupervisorWebSocketHandler;


/**
 * CODE
 */

/**
 * WebSocket configuration for Opty Socket Server.
 *
 * Registers WebSocket endpoints:
 * - /ws/client: Client connections
 * - /ws/supervisor: Supervisor connections
 */
@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    // --- ATRIBUTES ---
    private final ClientWebSocketHandler clientHandler;
    private final SupervisorWebSocketHandler supervisorHandler;
    private final AppConfig appConfig;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("Registering WebSocket handlers...");

        // Parse allowed origins from configuration
        String[] allowedOrigins = parseAllowedOrigins();

        // Register client endpoint
        registry.addHandler(clientHandler, "/ws/client").setAllowedOrigins(allowedOrigins);

        // Register supervisor endpoint
        registry.addHandler(supervisorHandler, "/ws/supervisor").setAllowedOrigins(allowedOrigins);

        log.info("WebSocket handlers registered: /ws/client, /ws/supervisor");
    }


    /**
     * Parse allowed origins CORS from configuration.
     */
    private String[] parseAllowedOrigins() {
        String origins = appConfig.getCors().getAllowedOrigins();

        // CORS not set: use wildcard
        if (origins == null || origins.trim().isEmpty()) {
            log.warn("No CORS origins configured, defaulting to wildcard (*)");
            return new String[]{"*"};
        }

        // Split by comma and trim whitespace
        return origins.split(",");
    }
}
