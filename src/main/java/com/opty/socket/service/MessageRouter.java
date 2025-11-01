/**
 * Message routing service.
 */

package com.opty.socket.service;


/**
 * IMPORTS
 */
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opty.socket.model.ConnectionInfo;
import com.opty.socket.model.Message;
import com.opty.socket.model.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;


/**
 * CODE
 */

/**
 * Routes messages between clients and supervisors.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageRouter {

    // --- ATTRIBUTES ---
    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;

    /**
     * Routes a message from sender to recipient.
     *
     * @param senderConnectionId the sender's connection ID
     * @param message           the message to route
     * @return true if routing successful, false otherwise
     */
    public boolean routeMessage(String senderConnectionId, Message message) {
        try {
            // Find sender's session
            Session session = sessionManager.getSessionByConnectionId(senderConnectionId)
                    .orElse(null);

            if (session == null) {
                log.warn("No session found for connection: connectionId={}", senderConnectionId);
                sendErrorToConnection(senderConnectionId, "Not in an active session");
                return false;
            }

            // Determine recipient connection ID
            String recipientConnectionId = session.getOtherPartyConnectionId(senderConnectionId);

            // If session is not paired, reject message
            if (recipientConnectionId == null) {
                log.warn("Session not paired yet: sessionId={}", session.sessionId());
                sendErrorToConnection(senderConnectionId, "Session not paired yet, waiting for other party");
                return false;
            }

            // Get recipient connection
            ConnectionInfo recipientConnection = sessionManager.getConnection(recipientConnectionId)
                    .orElse(null);

            if (recipientConnection == null) {
                // Recipient is offline
                log.warn("Recipient offline, message not delivered: connectionId={}, sessionId={}",
                        recipientConnectionId, session.sessionId());
                sendErrorToConnection(senderConnectionId, "Recipient is offline");
                return false;
            }

            // Send message to recipient
            boolean sent = sendMessage(recipientConnection.webSocketSession(), message);

            if (sent) {
                // Update session activity
                sessionManager.updateSessionActivity(session.sessionId());

                log.debug("Message routed: sessionId={}, from={}, to={}",
                        session.sessionId(), senderConnectionId, recipientConnectionId);
            }

            return sent;

        } catch (Exception e) {
            log.error("Error routing message: senderConnectionId={}, error={}",
                    senderConnectionId, e.getMessage(), e);
            sendErrorToConnection(senderConnectionId, "Failed to route message: " + e.getMessage());
            return false;
        }
    }

    /**
     * Sends a message to a WebSocket session.
     *
     * @param session the WebSocket session
     * @param message the message to send
     * @return true if sent successfully, false otherwise
     */
    public boolean sendMessage(WebSocketSession session, Message message) {
        if (session == null || !session.isOpen()) {
            log.warn("Cannot send message, session is null or closed");
            return false;
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
            return true;
        } catch (IOException e) {
            log.error("Failed to send message: sessionId={}, error={}",
                    session.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Sends an error message to a connection.
     *
     * @param connectionId  the connection ID
     * @param errorMessage the error message
     */
    public void sendErrorToConnection(String connectionId, String errorMessage) {
        sessionManager.getConnection(connectionId).ifPresent(conn -> {
            Session session = sessionManager.getSessionByConnectionId(connectionId).orElse(null);
            String sessionId = session != null ? session.sessionId() : null;
            Message error = Message.error(sessionId, errorMessage);
            sendMessage(conn.webSocketSession(), error);
        });
    }

    /**
     * Broadcasts a message to all connections in a session.
     *
     * @param sessionId the session ID
     * @param message   the message to broadcast
     * @return number of successful sends
     */
    public int broadcastToSession(String sessionId, Message message) {
        Session session = sessionManager.getSession(sessionId).orElse(null);
        if (session == null) {
            log.warn("Cannot broadcast, session not found: sessionId={}", sessionId);
            return 0;
        }

        int sentCount = 0;

        // Send to client
        ConnectionInfo clientConn = sessionManager.getConnection(session.clientConnectionId())
                .orElse(null);
        if (clientConn != null && sendMessage(clientConn.webSocketSession(), message)) {
            sentCount++;
        }

        // Send to supervisor if paired
        if (session.isPaired()) {
            ConnectionInfo supervisorConn = sessionManager.getConnection(session.supervisorConnectionId())
                    .orElse(null);
            if (supervisorConn != null && sendMessage(supervisorConn.webSocketSession(), message)) {
                sentCount++;
            }
        }

        log.debug("Broadcast to session: sessionId={}, recipients={}", sessionId, sentCount);
        return sentCount;
    }

    /**
     * Notifies the other party in a session about a disconnect.
     *
     * @param disconnectedConnectionId the connection ID that disconnected
     */
    public void notifyDisconnect(String disconnectedConnectionId) {
        sessionManager.getSessionByConnectionId(disconnectedConnectionId).ifPresent(session -> {
            String otherPartyId = session.getOtherPartyConnectionId(disconnectedConnectionId);
            if (otherPartyId != null) {
                sessionManager.getConnection(otherPartyId).ifPresent(conn -> {
                    Message disconnectMsg = new Message(
                            session.sessionId(),
                            "SERVER",
                            com.opty.socket.model.MessageType.DISCONNECT,
                            java.util.Map.of("message", "Other party disconnected")
                    );
                    sendMessage(conn.webSocketSession(), disconnectMsg);
                    log.info("Notified disconnect: sessionId={}, notified={}",
                            session.sessionId(), otherPartyId);
                });
            }
        });
    }

}
