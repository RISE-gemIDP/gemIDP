/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service.validation;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantRedirectUris;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.RedirectUriRepository;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantType.RP;
import static com.rise_world.gematik.accesskeeper.fedmaster.util.JwtUtils.getListStringProperty;
import static java.util.Map.entry;

@Service
public class RedirectUrisValidator implements RelyingPartyClaimValidator {

    private final RedirectUriRepository redirectUriRepository;

    public RedirectUrisValidator(RedirectUriRepository redirectUriRepository) {
        this.redirectUriRepository = redirectUriRepository;
    }

    @Override
    public RelyingPartyClaimValidationResult validate(ParticipantDto participant, JwtClaims entityStatement) {
        var fetchedRedirectUris = getListStringProperty(entityStatement, "metadata", RP.getType(), "redirect_uris")
            .map(ParticipantRedirectUris::new)
            .orElseGet(() -> new ParticipantRedirectUris(List.of()));

        var registeredRedirectUris = redirectUriRepository.findByParticipant(participant.getId());

        if (!fetchedRedirectUris.equals(registeredRedirectUris)) {
            return RelyingPartyClaimValidationResult.error(
                "registered redirect_uris (%s) do not match fetched redirect_uris (%s)".formatted(registeredRedirectUris, fetchedRedirectUris),
                Map.ofEntries(
                    entry("registered_redirect_uris", registeredRedirectUris.toString()),
                    entry("fetched_redirect_uris", fetchedRedirectUris.toString())
                ));
        }

        return RelyingPartyClaimValidationResult.ok();
    }
}
