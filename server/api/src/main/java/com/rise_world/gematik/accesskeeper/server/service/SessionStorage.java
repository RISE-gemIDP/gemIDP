/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.server.entity.ExtSessionEntity;

/**
 * Provides methods for session management
 */
public interface SessionStorage {

    /**
     * Creates a new, unique session id
     *
     * @return the created id
     */
    String createSessionId();

    /**
     * Writes a session to the storage. A session is identified by its id ({@link ExtSessionEntity#getState()})
     *
     * @param session the session
     */
    void writeSession(ExtSessionEntity session);

    /**
     * Reads a session from the storage
     *
     * @param sessionId the session id
     * @return the read session or {@code null}
     */
    ExtSessionEntity getSession(String sessionId);

    /**
     * Removes a session from the storage.
     * <p>
     * If the session was not found, the method completes without an error.
     *
     * @param sessionId the session id
     */
    void destroySession(String sessionId);
}
