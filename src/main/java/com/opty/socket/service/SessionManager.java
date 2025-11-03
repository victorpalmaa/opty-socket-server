/**
 * Session and connection manager.
 */

package com.opty.socket.service;


/**
 * IMPORTS
 */
import com.opty.socket.config.AppConfig;
import com.opty.socket.model.ConnectionInfo;
import com.opty.socket.model.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * CODE
 */

/**
 * Manages sessions and WebSocket connections (thread-safe).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionManager {

    // --- ATTRIBUTES ---
    private final AppConfig appConfig;
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> connectionIdToSessionId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConnectionInfo> connections = new ConcurrentHashMap<>();


    /**
     * Creates a new session for a client.
     *
     * @param clientConnectionId the client connection ID
     * @return the newly created session
     */
    public Session createSession(String clientConnectionId) {
        String sessionId = UUID.randomUUID().toString();
        Session session = new Session(sessionId, clientConnectionId);

        sessions.put(sessionId, session);
        connectionIdToSessionId.put(clientConnectionId, sessionId);

        log.info("Session created: sessionId={}, clientConnectionId={}, total={}",
                sessionId, clientConnectionId, sessions.size());

        return session;
    }


    /**
     * Pairs a supervisor with an existing session.
     *
     * @param sessionId             the session ID to join
     * @param supervisorConnectionId the supervisor connection ID
     * @return the updated session if successful, empty if session not found
     */
    public Optional<Session> pairSupervisor(String sessionId, String supervisorConnectionId) {
        Session updatedSession = sessions.computeIfPresent(sessionId, (id, session) -> {
            if (session.isPaired()) {
                log.warn("Session already paired: sessionId={}, existing={}, attempted={}",
                        sessionId, session.supervisorConnectionId(), supervisorConnectionId);
                return session; // Don't allow re-pairing
            }
            return session.withSupervisor(supervisorConnectionId);
        });

        if (updatedSession != null && updatedSession.isPaired()) {
            connectionIdToSessionId.put(supervisorConnectionId, sessionId);
            log.info("Supervisor paired: sessionId={}, supervisorConnectionId={}",
                    sessionId, supervisorConnectionId);
            return Optional.of(updatedSession);
        }

        return Optional.empty();
    }


    /**
     * Unpairs the supervisor from a session, keeping the session active for the client.
     *
     * @param sessionId the session ID
     * @return the unpaired session if successful, empty if session not found
     */
    public Optional<Session> unpairSupervisor(String sessionId) {
        Session unpairedSession = sessions.computeIfPresent(sessionId, (id, session) -> {
            if (!session.isPaired()) {
                log.warn("Session not paired, cannot unpair: sessionId={}", sessionId);
                return session;
            }

            // Remove supervisor connection ID mapping
            connectionIdToSessionId.remove(session.supervisorConnectionId());

            // Create new session without supervisor
            return new Session(session.sessionId(), session.clientConnectionId());
        });

        if (unpairedSession != null && !unpairedSession.isPaired()) {
            log.info("Supervisor unpaired from session: sessionId={}", sessionId);
            return Optional.of(unpairedSession);
        }

        return Optional.empty();
    }


    /**
     * Gets a session by session ID.
     *
     * @param sessionId the session ID
     * @return the session, or empty if not found
     */
    public Optional<Session> getSession(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }


    /**
     * Gets a session by connection ID (client or supervisor).
     *
     * @param connectionId the connection ID
     * @return the session, or empty if not found
     */
    public Optional<Session> getSessionByConnectionId(String connectionId) {
        String sessionId = connectionIdToSessionId.get(connectionId);
        if (sessionId == null) {
            return Optional.empty();
        }
        return getSession(sessionId);
    }

    /**
     * Updates the last activity time for a session.
     *
     * @param sessionId the session ID
     * @return true if updated successfully, false if session not found
     */
    public boolean updateSessionActivity(String sessionId) {
        return sessions.computeIfPresent(sessionId, (id, session) ->
                session.withLastActivity()
        ) != null;
    }


    /**
     * Removes a session.
     *
     * @param sessionId the session ID to remove
     * @return the removed session, or empty if not found
     */
    public Optional<Session> removeSession(String sessionId) {
        Session removed = sessions.remove(sessionId);
        if (removed != null) {
            // Clean up connection mappings
            connectionIdToSessionId.remove(removed.clientConnectionId());
            if (removed.supervisorConnectionId() != null) {
                connectionIdToSessionId.remove(removed.supervisorConnectionId());
            }

            log.info("Session removed: sessionId={}, total={}",
                    sessionId, sessions.size());
        }
        return Optional.ofNullable(removed);
    }


    /**
     * Removes a session by connection ID.
     *
     * @param connectionId the connection ID (client or supervisor)
     * @return the removed session, or empty if not found
     */
    public Optional<Session> removeSessionByConnectionId(String connectionId) {
        String sessionId = connectionIdToSessionId.get(connectionId);
        if (sessionId == null) {
            return Optional.empty();
        }
        return removeSession(sessionId);
    }


    /**
     * Gets all active sessions.
     *
     * @return list of all sessions
     */
    public List<Session> getAllSessions() {
        return List.copyOf(sessions.values());
    }

    /**
     * Gets all paired sessions.
     *
     * @return list of paired sessions
     */
    public List<Session> getPairedSessions() {
        return sessions.values().stream()
                .filter(Session::isPaired)
                .collect(Collectors.toList());
    }


    /**
     * Gets all unpaired sessions (waiting for supervisor).
     *
     * @return list of unpaired sessions
     */
    public List<Session> getUnpairedSessions() {
        return sessions.values().stream()
                .filter(session -> !session.isPaired())
                .collect(Collectors.toList());
    }

    
    /**
     * Cleans up expired sessions.
     *
     * @return number of sessions removed
     */
    public int cleanupExpiredSessions() {
        int timeoutMinutes = appConfig.getSession().getTimeoutMinutes();
        List<String> expiredSessionIds = sessions.values().stream()
                .filter(session -> session.isExpired(timeoutMinutes))
                .map(Session::sessionId)
                .collect(Collectors.toList());

        expiredSessionIds.forEach(this::removeSession);

        if (!expiredSessionIds.isEmpty()) {
            log.info("Cleaned up {} expired sessions", expiredSessionIds.size());
        }

        return expiredSessionIds.size();
    }


    /**
     * Gets the total number of active sessions.
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }


    /**
     * Gets the number of paired sessions.
     */
    public int getPairedSessionCount() {
        return (int) sessions.values().stream()
                .filter(Session::isPaired)
                .count();
    }


    // ========== Connection Management Methods ==========

    /**
     * Registers a new connection.
     *
     * @param connectionInfo the connection information
     * @return true if registered successfully, false if max connections reached
     */
    public boolean registerConnection(ConnectionInfo connectionInfo) {
        if (connections.size() >= appConfig.getMaxConnections()) {
            log.warn("Max connections reached: {}/{}", connections.size(), appConfig.getMaxConnections());
            return false;
        }

        connections.put(connectionInfo.connectionId(), connectionInfo);
        log.info("Connection registered: connectionId={}, type={}, total={}",
                connectionInfo.connectionId(),
                connectionInfo.connectionType(),
                connections.size());

        return true;
    }


    /**
     * Removes a connection.
     *
     * @param connectionId the connection ID to remove
     * @return the removed connection info, or empty if not found
     */
    public Optional<ConnectionInfo> removeConnection(String connectionId) {
        ConnectionInfo removed = connections.remove(connectionId);
        if (removed != null) {
            log.info("Connection removed: connectionId={}, type={}, total={}",
                    connectionId,
                    removed.connectionType(),
                    connections.size());
        }
        return Optional.ofNullable(removed);
    }


    /**
     * Gets connection information by ID.
     *
     * @param connectionId the connection ID
     * @return the connection info, or empty if not found
     */
    public Optional<ConnectionInfo> getConnection(String connectionId) {
        return Optional.ofNullable(connections.get(connectionId));
    }


    /**
     * Updates the session ID for a connection.
     *
     * @param connectionId the connection ID
     * @param sessionId    the session ID to associate
     * @return true if updated successfully, false if connection not found
     */
    public boolean updateSessionId(String connectionId, String sessionId) {
        return connections.computeIfPresent(connectionId, (id, info) -> {
            log.debug("Updated sessionId for connection: connectionId={}, sessionId={}",
                    connectionId, sessionId);
            return info.withSessionId(sessionId);
        }) != null;
    }


    /**
     * Gets all connections of a specific type.
     *
     * @param connectionType CLIENT or SUPERVISOR
     * @return list of connections of the specified type
     */
    public List<ConnectionInfo> getConnectionsByType(String connectionType) {
        return connections.values().stream()
                .filter(conn -> connectionType.equals(conn.connectionType()))
                .collect(Collectors.toList());
    }


    /**
     * Gets all connections associated with a session.
     *
     * @param sessionId the session ID
     * @return list of connections in this session
     */
    public List<ConnectionInfo> getConnectionsBySession(String sessionId) {
        return connections.values().stream()
                .filter(conn -> sessionId.equals(conn.sessionId()))
                .collect(Collectors.toList());
    }


    /**
     * Gets the total number of active connections.
     */
    public int getActiveConnectionCount() {
        return connections.size();
    }


    /**
     * Gets the number of client connections.
     */
    public int getClientConnectionCount() {
        return (int) connections.values().stream()
                .filter(ConnectionInfo::isClient)
                .count();
    }


    /**
     * Gets the number of supervisor connections.
     */
    public int getSupervisorConnectionCount() {
        return (int) connections.values().stream()
                .filter(ConnectionInfo::isSupervisor)
                .count();
    }

    
    /**
     * Checks if max connections limit has been reached.
     */
    public boolean isMaxConnectionsReached() {
        return connections.size() >= appConfig.getMaxConnections();
    }
}
