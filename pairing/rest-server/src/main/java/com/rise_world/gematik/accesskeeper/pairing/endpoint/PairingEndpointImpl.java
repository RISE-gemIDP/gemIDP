/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairing.endpoint;

import com.rise_world.gematik.accesskeeper.pairing.filter.AuthorizationContext;
import com.rise_world.gematik.accesskeeper.pairingdienst.dto.AccessTokenDTO;
import com.rise_world.gematik.accesskeeper.pairingdienst.entity.PairingEntryEntity;
import com.rise_world.gematik.accesskeeper.pairingdienst.service.PairingService;
import com.rise_world.gematik.idp.server.api.pairing.PairingEndpoint;
import com.rise_world.gematik.idp.server.api.pairing.PairingEntries;
import com.rise_world.gematik.idp.server.api.pairing.PairingEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.ForbiddenException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Pairing Endpoint implementation.
 */
@RestController
public class PairingEndpointImpl implements PairingEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(PairingEndpointImpl.class);

    private final PairingService pairingService;

    @Autowired
    public PairingEndpointImpl(PairingService pairingService) {
        this.pairingService = pairingService;
    }

    @Override
    public PairingEntry registerPairing(String encryptedRegistrationData) {
        PairingEntryEntity pairingEntryEntity = pairingService.registerPairing(getAccessToken(), encryptedRegistrationData);
        return mapPairingEntry(pairingEntryEntity);
    }

    @Override
    public void deregisterPairing(String keyIdentifier) {
        pairingService.deregisterPairing(getAccessToken(), keyIdentifier);
    }

    @Override
    public PairingEntries inspectPairings() {
        List<PairingEntryEntity> pairingEntryEntities = pairingService.inspectPairings(getAccessToken());

        return mapPairingEntries(pairingEntryEntities);
    }

    private AccessTokenDTO getAccessToken() {
        AccessTokenDTO accessToken = AuthorizationContext.getAccessToken();

        if (accessToken == null) {
            LOG.warn("access token is null");
            throw new ForbiddenException("missing access token");
        }
        return accessToken;
    }

    private PairingEntry mapPairingEntry(PairingEntryEntity entity) {
        PairingEntry pairingEntry = new PairingEntry();
        pairingEntry.setPairingEntryVersion(entity.getPairingEntryVersion());
        pairingEntry.setName(entity.getDeviceName());
        pairingEntry.setCreationTime(entity.getCreationTimeAsInstant().getEpochSecond());
        pairingEntry.setSignedPairingData(entity.getSignedPairingData());
        return pairingEntry;
    }

    private PairingEntries mapPairingEntries(List<PairingEntryEntity> entities) {
        PairingEntries pairingEntries = new PairingEntries();
        pairingEntries.setPairingEntries(entities.stream().map(this::mapPairingEntry).collect(Collectors.toList()));
        return pairingEntries;
    }
}
