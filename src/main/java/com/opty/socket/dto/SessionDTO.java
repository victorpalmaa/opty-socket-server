/**
 * Session DTO.
 */

package com.opty.socket.dto;


/**
 * IMPORTS
 */
import com.opty.socket.model.Session;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;


/**
 * CODE
 */

/**
 * Session data transfer object for REST API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionDTO {

    // --- ATTRIBUTES ---
    private String sessionId;
    private boolean paired;
    private String createdAt;
    private long waitingTimeMinutes;
    private long waitingTimeSeconds;

    /**
     * Creates a DTO from a Session entity.
     */
    public static SessionDTO fromSession(Session session) {
        
        // Calculate waiting time
        Duration waitingTime = Duration.between(session.createdAt(), Instant.now());

        // Create and return DTO
        return new SessionDTO(
                session.sessionId(),
                session.isPaired(),
                session.createdAt().toString(),
                waitingTime.toMinutes(),
                waitingTime.getSeconds() % 60
        );
    }
}
