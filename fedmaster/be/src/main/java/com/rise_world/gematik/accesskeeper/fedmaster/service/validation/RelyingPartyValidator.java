/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service.validation;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@code RelyingPartyValidator} validates the {@link ParticipantDto participant} and the fetched {@link JwtClaims entity statement}
 * using all {@link RelyingPartyClaimValidator} implementations found in the application.
 */
@Service
public class RelyingPartyValidator {

    private final List<RelyingPartyClaimValidator> validators;

    public RelyingPartyValidator(List<RelyingPartyClaimValidator> validators) {
        this.validators = validators;
    }

    /**
     * {@code validate} validates the given {@link ParticipantDto relying party} and its fetched {@link JwtClaims entity statement}.
     * If there is a discrepancy for the registered data and the {@link JwtClaims entity statement} a {@link RelyingPartyValidationResult}
     * with all error information will be returned.
     *
     * @param participant     {@link ParticipantDto} to check
     * @param entityStatement fetched {@link JwtClaims entity statement}
     * @return a {@link RelyingPartyValidationResult} containing the validation results,
     * including any errors or discrepancies found during validation.
     */
    public RelyingPartyValidationResult validate(ParticipantDto participant, JwtClaims entityStatement) {
        var result = new RelyingPartyValidationResult();
        validators.stream()
            .map(validator -> validator.validate(participant, entityStatement))
            .forEach(result::add);

        return result;
    }

}
