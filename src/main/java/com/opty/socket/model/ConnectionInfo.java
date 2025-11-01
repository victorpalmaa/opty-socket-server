/**
 * Connection metadata.
 */

package com.opty.socket.model;


/**
 * IMPORTS
 */
import org.springframework.web.socket.WebSocketSession;


/**
 * CODE
 */

/**
 * Tracks active WebSocket connections (client or supervisor).
 */
public record ConnectionInfo(

        String connectionId,
        WebSocketSession webSocketSession,
        String connectionType,
        String sessionId
) {

    /**
     * Creates a copy with updated session ID.
     */
    public ConnectionInfo withSessionId(String newSessionId) {
        return new ConnectionInfo(
                connectionId,
                webSocketSession,
                connectionType,
                newSessionId
        );
    }

    /**
     * Checks if this connection is a client.
     */
    public boolean isClient() {
        return "CLIENT".equals(connectionType);
    }

    /**
     * Checks if this connection is a supervisor.
     */
    public boolean isSupervisor() {
        return "SUPERVISOR".equals(connectionType);
    }

    /**
     * Checks if this connection is paired (has a session ID).
     */
    public boolean isPaired() {
        return sessionId != null && !sessionId.isBlank();
    }
}
