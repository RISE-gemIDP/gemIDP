/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service.validation;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;

/**
 * {@code RelyingPartyClaimValidator} defines a method for validating registered {@link ParticipantDto relying party} data
 * with one or more claims of the related {@link JwtClaims entity statement}.
 * <p>
 * All implementations of this interface are used to collect all {@link RelyingPartyClaimValidationResult validation results} in
 * the {@link RelyingPartyValidator}.
 */
public interface RelyingPartyClaimValidator {

    /**
     * {@code validate} validates one or more claims of the given {@link ParticipantDto relying party} with the given {@link JwtClaims entity statement}.
     *
     * @param participant     {@link ParticipantDto relying party} registered data
     * @param entityStatement fetched {@link JwtClaims entity statement} for the provider
     * @return {@link RelyingPartyValidationResult} with error information if the validation failed
     */
    RelyingPartyClaimValidationResult validate(ParticipantDto participant, JwtClaims entityStatement);
}
