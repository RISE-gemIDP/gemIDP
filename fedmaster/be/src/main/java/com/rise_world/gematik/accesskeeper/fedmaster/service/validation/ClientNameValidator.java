/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service.validation;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

import static com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantType.RP;
import static com.rise_world.gematik.accesskeeper.fedmaster.util.JwtUtils.getStringProperty;
import static java.util.Map.entry;
import static java.util.Objects.requireNonNullElse;

@Service
public class ClientNameValidator implements RelyingPartyClaimValidator {

    @Override
    public RelyingPartyClaimValidationResult validate(ParticipantDto participant, JwtClaims entityStatement) {
        var fetchedClientName = getStringProperty(entityStatement, "metadata", RP.getType(), "client_name").orElse(null);
        var fetchedFederationName = getStringProperty(entityStatement, "metadata", "federation_entity", "name").orElse(null);
        var registeredClientName = participant.getClientName();

        if (!Objects.equals(fetchedClientName, fetchedFederationName) || !Objects.equals(registeredClientName, fetchedClientName)) {
            return error(
                requireNonNullElse(registeredClientName, "<registered client_name null>"),
                requireNonNullElse(fetchedFederationName, "<fetched federation_name null>"),
                requireNonNullElse(fetchedClientName, "<fetched client_name null>"));
        }

        return RelyingPartyClaimValidationResult.ok();
    }

    private static RelyingPartyClaimValidationResult error(String registeredClientName, String fetchedFederationName, String fetchedClientName) {
        return RelyingPartyClaimValidationResult.error(
            "client name validation failed: registered client_name (%s) does not match fetched federation_name (%s) or client_name (%s)"
                .formatted(registeredClientName, fetchedFederationName, fetchedClientName),
            Map.ofEntries(
                entry("fetched_client_name", fetchedClientName),
                entry("fetched_federation_name", fetchedFederationName),
                entry("registered_client_name", registeredClientName))
            );
    }

}
