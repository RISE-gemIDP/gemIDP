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
public class OrganizationNameValidator implements RelyingPartyClaimValidator {

    @Override
    public RelyingPartyClaimValidationResult validate(ParticipantDto participant, JwtClaims entityStatement) {
        var fetchedName = getStringProperty(entityStatement, "metadata", RP.getType(), "organization_name").orElse(null);
        var registeredOrganizationName = participant.getOrganizationName();

        if (!Objects.equals(registeredOrganizationName, fetchedName)) {
            return error(
                requireNonNullElse(registeredOrganizationName, "<registered name null>"),
                requireNonNullElse(fetchedName, "<fetched name null>"));
        }

        return RelyingPartyClaimValidationResult.ok();
    }

    private static RelyingPartyClaimValidationResult error(String registeredOrganizationName, String fetchedName) {
        return RelyingPartyClaimValidationResult.error(
            "registered organization_name (%s) does not match fetched organization_name (%s)".formatted(registeredOrganizationName, fetchedName),
            Map.ofEntries(
                entry("registered_organization_name", registeredOrganizationName),
                entry("fetched_organization_name", fetchedName)
            ));
    }
}
