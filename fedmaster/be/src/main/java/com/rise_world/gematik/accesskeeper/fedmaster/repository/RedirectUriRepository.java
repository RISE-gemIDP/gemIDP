/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.repository;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantRedirectUris;

/**
 * Repository to access redirect uris
 */
public interface RedirectUriRepository {

    /**
     * Fetches all {@code redirect_uri}s configured for a {@code participant}.
     *
     * @param identifier of the {@code participant}
     * @return configured {@link ParticipantRedirectUris}
     */
    ParticipantRedirectUris findByParticipant(Long identifier);
}
