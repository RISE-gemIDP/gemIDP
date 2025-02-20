/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service.validation;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.rise_world.gematik.accesskeeper.fedmaster.util.JwtUtils.getListStringProperty;

@Service
public class AuthorityHintsValidator implements RelyingPartyClaimValidator {

    private final String authority;

    public AuthorityHintsValidator(@Value("${federation.issuer}") String authority) {
        this.authority = authority;
    }

    @Override
    public RelyingPartyClaimValidationResult validate(ParticipantDto participant, JwtClaims entityStatement) {
        var listStringProperty = getListStringProperty(entityStatement, "authority_hints")
            .orElseGet(List::of);

        if (listStringProperty.size() != 1 || !listStringProperty.get(0).equals(authority)) {
            return RelyingPartyClaimValidationResult.lockingError(
                "invalid authority_hints (%s)".formatted(listStringProperty),
                Map.of("fetched_authority_hints", listStringProperty.toString()));
        }

        return RelyingPartyClaimValidationResult.ok();
    }
}
