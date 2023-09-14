/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.common.OAuth2Constants;
import com.rise_world.gematik.accesskeeper.common.crypt.CryptoConstants;
import com.rise_world.gematik.accesskeeper.server.dto.RequestSource;
import com.rise_world.gematik.accesskeeper.common.token.creation.TokenCreationStrategy;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DiscoveryServiceImpl implements DiscoveryService {

    private ConfigService configService;
    private Clock clock;
    private TokenCreationStrategy discStrategy;

    @Autowired
    public DiscoveryServiceImpl(ConfigService configService, Clock clock, @Qualifier("discStrategy") TokenCreationStrategy discStrategy) {
        this.configService = configService;
        this.clock = clock;
        this.discStrategy = discStrategy;
    }

    @Override
    // @AFO: A_20458-01 - Attribute laut Anforderung werden zu den Claims hinzugef&uuml;gt
    // @AFO: A_20732 - &Ouml;ffentlicher Schl&uuml;ssel zu prk_idp_sig wird mit absoluter URI als 'uri_puk_idp_sig' zum Discovery Document hinzugef&uuml;gt
    // @AFO: A_20732 - &Ouml;ffentlicher Schl&uuml;ssel zu prk_idp_enc wird mit absoluter URI als 'uri_puk_idp_enc' zum Discovery Document hinzugef&uuml;gt
    // @AFO: A_20457 - Die verwendeten Adressen von authorization-, sso- und token-Endpoint werden als URL im Discovery Document ver&ouml;ffentlicht
    // @AFO: A_20439 - Die anderen Endpunkte des IDPs werden als Claims zum Discovery Document hinzugef&uuml;gt
    public String getDiscoverDocument() {
        Instant now = clock.instant();
        long epochSecond = now.getEpochSecond();

        RequestSource requestSource = RequestContext.getRequestSource();
        String issuer = configService.getIssuer(requestSource);

        JwtClaims discoveryClaims = new JwtClaims();
        discoveryClaims.setIssuedAt(epochSecond);
        discoveryClaims.setExpiryTime(now.plus(24, ChronoUnit.HOURS).getEpochSecond());

        discoveryClaims.setProperty("issuer", issuer); // rfc8414 requires 'issuer' (not 'iss'!)
        discoveryClaims.setProperty("jwks_uri", issuer + "/certs");                            // @AFO: A_20458-01 jwks_uri
        discoveryClaims.setProperty("uri_disc", issuer + "/.well-known/openid-configuration"); // @AFO: A_20458-01 uri_disc
        discoveryClaims.setProperty("authorization_endpoint", issuer + "/auth");               // @AFO: A_20458-01 authorization_endpoint   @AFO: A_20457   @AFO: A_20439
        discoveryClaims.setProperty("sso_endpoint", issuer + "/auth/sso_response");            // @AFO: A_20458-01 sso_endpoint             @AFO: A_20457   @AFO: A_20439
        discoveryClaims.setProperty("token_endpoint", issuer + "/token");                      // @AFO: A_20458-01 token_endpoint           @AFO: A_20457   @AFO: A_20439

        // the external DD needs additional claims
        if (requestSource == RequestSource.INTERNET) {
            discoveryClaims.setProperty("auth_pair_endpoint", issuer + "/auth/alternative");
            discoveryClaims.setProperty("uri_pair", configService.getPairingEndpoint() + "/pairings");

            // external authentication claims
            discoveryClaims.setProperty("kk_app_list_uri", issuer + "/directory/kk_apps");
            discoveryClaims.setProperty("third_party_authorization_endpoint", issuer + "/extauth");
            discoveryClaims.setProperty("fed_idp_list_uri", issuer + "/directory/fed_idp_list");
            discoveryClaims.setProperty("federation_authorization_endpoint", issuer + "/fedauth");
        }

        discoveryClaims.setProperty("uri_puk_idp_enc", issuer + "/certs/puk_idp_enc");         // @AFO: A_20458-01 uri_puk_idp_enc          @AFO: A_20732
        discoveryClaims.setProperty("uri_puk_idp_sig", issuer + "/certs/puk_idp_sig");         // @AFO: A_20458-01 uri_puk_idp_sig          @AFO: A_20732
        discoveryClaims.setProperty("code_challenge_methods_supported", Collections.singletonList("S256"));
        discoveryClaims.setProperty("response_types_supported", Collections.singletonList("code"));
        discoveryClaims.setProperty("grant_types_supported", Collections.singletonList("authorization_code"));
        discoveryClaims.setProperty("id_token_signing_alg_values_supported", Collections.singletonList(CryptoConstants.SIG_ALG_BRAINPOOL_P256_R1));
        discoveryClaims.setProperty("acr_values_supported", Collections.singletonList(OAuth2Constants.ACR_LOA_HIGH));
        discoveryClaims.setProperty("response_modes_supported", Collections.singletonList("query"));
        discoveryClaims.setProperty("token_endpoint_auth_methods_supported", Collections.singletonList("none"));

        List<String> scopes = new ArrayList<>();
        scopes.add(OAuth2Constants.SCOPE_OPENID);
        scopes.addAll(configService.getFachdienstScopes());
        discoveryClaims.setProperty("scopes_supported", scopes);

        discoveryClaims.setProperty("subject_types_supported", Collections.singletonList("pairwise"));

        return discStrategy.toToken(discoveryClaims); // @AFO: A_20591-01 - Claims werden mittels TokenCreationStrategy in einen JWT in Compact Serialization serialisiert
    }

}
