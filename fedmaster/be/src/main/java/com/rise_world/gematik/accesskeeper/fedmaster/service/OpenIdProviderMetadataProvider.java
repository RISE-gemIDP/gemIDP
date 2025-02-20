/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantType;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
class OpenIdProviderMetadataProvider extends EntityStatementMetadataProvider {

    OpenIdProviderMetadataProvider() {
        super(ParticipantType.OP);
    }

    @Override
    protected JwtClaims typeSpecificMetadata(ParticipantDto participant) {
        return new JwtClaims(Map.of("client_registration_types_supported", List.of("automatic")));
    }
}
