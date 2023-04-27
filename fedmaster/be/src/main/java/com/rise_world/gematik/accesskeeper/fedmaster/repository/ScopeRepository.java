/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.repository;

import java.util.List;

/**
 * Repository to access openid-relying-party scopes
 */
public interface ScopeRepository {

    /**
     * Fetches all scopes associated to a participant
     * @param identifier of the participant
     * @return a list of scopes
     */
    List<String> findByParticipant(Long identifier);
}
