/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service;

/**
 * Synchronization service to reconciliate the entity statements
 * provided by the endpoints of the federation participants
 */
public interface EntityStatementSynchronization {

    String INSTANCE_NAME = "synchronization-entity-statements";

    /**
     * Method to start synchronization of all outdated entity statements
     */
    void synchronize();

    /**
     * Method to start synchronization of a specific entity statement
     * @param identifier    of the participant to be synced
     * @param dataSync      data synchronization needed
     * @param ctrCheck      certificate transparency check needed
     */
    void synchronizeParticipant(Long identifier, boolean dataSync, boolean ctrCheck);
}
