/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service.validation;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantClaims;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.ClaimRepository;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantType.RP;
import static com.rise_world.gematik.accesskeeper.fedmaster.util.JwtUtils.getListStringProperty;
import static java.util.Map.entry;
import static java.util.Objects.isNull;

@Service
public class ClaimsValidator implements RelyingPartyClaimValidator {

    private final ClaimRepository claimRepository;

    public ClaimsValidator(ClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    @Override
    public RelyingPartyClaimValidationResult validate(ParticipantDto participant, JwtClaims entityStatement) {
        var fetchedClaims = getListStringProperty(entityStatement, "metadata", RP.getType(), "claims")
            .map(ParticipantClaims::new)
            .orElse(null);

        // only validate claims if they are present in the entity statement
        if (isNull(fetchedClaims)) {
            return RelyingPartyClaimValidationResult.ok();
        }

        var registeredClaims = claimRepository.findByParticipant(participant.getId());
        if (!registeredClaims.hasAllClaims(fetchedClaims)) {
            var message = "registered claims (%s) do not match fetched claims (%s)".formatted(registeredClaims.toString(), fetchedClaims.toString());
            return RelyingPartyClaimValidationResult.lockingError(message,
                Map.ofEntries(
                    entry("registered_claims", registeredClaims.toString()),
                    entry("fetched_claims", fetchedClaims.toString())));
        }

        if (!registeredClaims.equals(fetchedClaims)) {
            return RelyingPartyClaimValidationResult.warning("fewer claims requested than registered",
                Map.ofEntries(
                    entry("registered_claims", registeredClaims.toString()),
                    entry("fetched_claims", fetchedClaims.toString())
                ));
        }

        return RelyingPartyClaimValidationResult.ok();
    }
}
