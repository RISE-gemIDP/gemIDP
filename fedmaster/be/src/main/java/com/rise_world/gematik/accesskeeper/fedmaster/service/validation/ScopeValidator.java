/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service.validation;

import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantScopes;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.ScopeRepository;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantType.RP;
import static com.rise_world.gematik.accesskeeper.fedmaster.util.JwtUtils.getStringProperty;
import static java.util.Map.entry;

@Service
public class ScopeValidator implements RelyingPartyClaimValidator {

    private final ScopeRepository scopeRepository;

    public ScopeValidator(ScopeRepository scopeRepository) {
        this.scopeRepository = scopeRepository;
    }

    @Override
    public RelyingPartyClaimValidationResult validate(ParticipantDto participant, JwtClaims entityStatement) {
        var registeredScopes = scopeRepository.findByParticipant(participant.getId());
        var fetchedScopes = getStringProperty(entityStatement, "metadata", RP.getType(), "scope")
            .map(ClaimUtils::getScopes)
            .map(ParticipantScopes::new)
            .orElseGet(ParticipantScopes::new);

        // error if a fetched scope is not registered
        if (!registeredScopes.hasAllScopes(fetchedScopes)) {
            var message = "registered scopes (%s) do not match fetched scopes (%s)".formatted(registeredScopes.asString(), fetchedScopes.asString());
            return RelyingPartyClaimValidationResult.lockingError(message,
                Map.ofEntries(
                    entry("registered_scopes", registeredScopes.asString()),
                    entry("fetched_scopes", fetchedScopes.asString())));
        }

        // warning if less scopes are request
        if (!registeredScopes.equals(fetchedScopes)) {
            return RelyingPartyClaimValidationResult.warning("fewer scopes requested than registered", Map.ofEntries(
                entry("registered_scopes", registeredScopes.asString()),
                entry("fetched_scopes", fetchedScopes.asString())
            ));
        }

        return RelyingPartyClaimValidationResult.ok();
    }
}
