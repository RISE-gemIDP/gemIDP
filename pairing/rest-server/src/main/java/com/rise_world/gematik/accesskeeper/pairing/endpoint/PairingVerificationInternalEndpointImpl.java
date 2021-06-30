/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairing.endpoint;

import com.rise_world.gematik.accesskeeper.common.api.pairing.AuthenticationResponse;
import com.rise_world.gematik.accesskeeper.common.api.pairing.PairingVerificationInternalEndpoint;
import com.rise_world.gematik.accesskeeper.common.api.pairing.SignedAuthenticationDataRequest;
import com.rise_world.gematik.accesskeeper.pairingdienst.service.PairingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PairingVerificationInternalEndpointImpl implements PairingVerificationInternalEndpoint {

    private final PairingService pairingService;

    @Autowired
    public PairingVerificationInternalEndpointImpl(PairingService pairingService) {
        this.pairingService = pairingService;
    }

    @Override
    public AuthenticationResponse authenticateClientDevice(SignedAuthenticationDataRequest signedAuthenticationData) {
        String challengeToken = pairingService.verifyAlternativeAuthentication(signedAuthenticationData.getSignedAuthenticationData());

        AuthenticationResponse authenticationResponse = new AuthenticationResponse();
        authenticationResponse.setChallengeToken(challengeToken);
        return authenticationResponse;
    }
}
