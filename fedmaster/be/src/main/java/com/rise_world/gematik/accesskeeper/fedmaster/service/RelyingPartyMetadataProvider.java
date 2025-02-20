/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantType;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.ClaimRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.RedirectUriRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.ScopeRepository;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

@Service
class RelyingPartyMetadataProvider extends EntityStatementMetadataProvider {

    private final ScopeRepository scopeRepository;
    private final ClaimRepository claimRepository;
    private final RedirectUriRepository redirectUriRepository;

    RelyingPartyMetadataProvider(ScopeRepository scopeRepository, ClaimRepository claimRepository, RedirectUriRepository redirectUriRepository) {
        super(ParticipantType.RP);
        this.scopeRepository = scopeRepository;
        this.claimRepository = claimRepository;
        this.redirectUriRepository = redirectUriRepository;
    }

    @Override
    protected JwtClaims typeSpecificMetadata(ParticipantDto participant) {

        return new JwtClaims(Map.ofEntries(
            entry("client_registration_types", List.of("automatic")),
            entry("scope", scopeRepository.findByParticipant(participant.getId()).asString()),
            entry("claims", claimRepository.findByParticipant(participant.getId()).asList()),
            entry("redirect_uris", redirectUriRepository.findByParticipant(participant.getId()).asList())
        ));
    }
}
