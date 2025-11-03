/**
 * Supervisor queue service.
 */

package com.opty.socket.service;


/**
 * IMPORTS
 */
import com.opty.socket.dto.SessionDTO;
import com.opty.socket.model.Message;
import com.opty.socket.model.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * CODE
 */

/**
 * Manages supervisor queue and broadcasts session updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupervisorQueueService {

    // --- ATTRIBUTES ---
    private final SessionManager sessionManager;
    private final MessageRouter messageRouter;
    private final Set<String> queueListeners = ConcurrentHashMap.newKeySet();

    
    /**
     * Registers a supervisor as a queue listener.
     *
     * @param connectionId the supervisor connection ID
     */
    public void registerQueueListener(String connectionId) {
        boolean added = queueListeners.add(connectionId);
        if (added) {
            log.info("Supervisor registered as queue listener: connectionId={}, totalListeners={}",
                    connectionId, queueListeners.size());
        }
    }


    /**
     * Removes a supervisor from queue listeners (when accepting session or disconnecting).
     *
     * @param connectionId the supervisor connection ID
     */
    public void removeQueueListener(String connectionId) {
        boolean removed = queueListeners.remove(connectionId);
        if (removed) {
            log.info("Supervisor removed from queue listeners: connectionId={}, totalListeners={}",
                    connectionId, queueListeners.size());
        }
    }


    /**
     * Checks if a supervisor is a queue listener.
     *
     * @param connectionId the supervisor connection ID
     * @return true if listening to queue, false otherwise
     */
    public boolean isQueueListener(String connectionId) {
        return queueListeners.contains(connectionId);
    }


    /**
     * Broadcasts session queue update to all listening supervisors.
     * Called when:
     * - New session is created (client connects)
     * - Session is accepted by supervisor (becomes paired)
     * - Session is closed (client disconnects)
     */
    public void broadcastQueueUpdate() {
        if (queueListeners.isEmpty()) {
            log.debug("No queue listeners to broadcast to");
            return;
        }

        // Get all unpaired sessions
        List<SessionDTO> availableSessions = sessionManager.getUnpairedSessions()
                .stream()
                .map(SessionDTO::fromSession)
                .collect(Collectors.toList());

        // Create broadcast message
        Message queueUpdate = new Message(
                null,
                "SERVER",
                MessageType.SESSION_QUEUE_UPDATE,
                Map.of("sessions", availableSessions)
        );

        // Send to all queue listeners
        int successCount = 0;
        for (String listenerId : queueListeners) {
            sessionManager.getConnection(listenerId).ifPresent(conn -> {
                messageRouter.sendMessage(conn.webSocketSession(), queueUpdate);
            });
            successCount++;
        }

        log.info("Broadcast queue update to {} supervisors: {} available sessions",
                successCount, availableSessions.size());
    }


    /**
     * Sends initial queue state to a newly connected supervisor.
     *
     * @param session the supervisor WebSocket session
     */
    public void sendInitialQueue(WebSocketSession session) {
        List<SessionDTO> availableSessions = sessionManager.getUnpairedSessions()
                .stream()
                .map(SessionDTO::fromSession)
                .collect(Collectors.toList());

        Message queueUpdate = new Message(
                null,
                "SERVER",
                MessageType.SESSION_QUEUE_UPDATE,
                Map.of("sessions", availableSessions)
        );

        messageRouter.sendMessage(session, queueUpdate);
        log.info("Sent initial queue to supervisor: connectionId={}, sessions={}",
                session.getId(), availableSessions.size());
    }


    /**
     * Gets the number of supervisors currently listening to the queue.
     *
     * @return number of queue listeners
     */
    public int getQueueListenerCount() {
        return queueListeners.size();
    }
}
