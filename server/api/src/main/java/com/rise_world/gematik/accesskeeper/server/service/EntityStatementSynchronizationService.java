/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.server.dto.EntityStatementDTO;
import com.rise_world.gematik.accesskeeper.server.dto.ReloadEntityStatementEvent;

import java.util.Collection;

/**
 * Service to synchronize and cache entity statement from other openid providers.
 */
public interface EntityStatementSynchronizationService {

    /**
     * Update the entity statement cache.
     * <p>
     * Get the entity statement of each openid provider from the federation master list.
     * Override cache entries if they already exist.
     */
    void updateEntityStatementCache();

    /**
     * Returns all active cache entries, expired entries will be removed from the cache in advance.
     *
     * @return active cache entries
     */
    Collection<EntityStatementDTO> getEntityStatementCache();

    /**
     * Returns the cache entry for the given idpIss, expired entries will be removed from the cache in advance.
     *
     * @param idpIss issuer
     * @return active cache entries
     * @throws AccessKeeperException if no entity statement with the given idpIss exists
     */
    EntityStatementDTO getEntityStatementCache(String idpIss);

    /**
     * {@code reloadEntityStatement} triggers an asynchronous reload of the
     * {@link EntityStatementDTO} for the given issuer.
     * <p>
     * The reload can only be triggered once during the scheduled update period.
     *
     * @param event {@link ReloadEntityStatementEvent event} containing issuer information
     */
    void reloadEntityStatement(ReloadEntityStatementEvent event);
}
