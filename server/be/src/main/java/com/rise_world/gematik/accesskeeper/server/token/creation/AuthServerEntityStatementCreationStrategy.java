/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.token.creation;

import com.rise_world.gematik.accesskeeper.common.crypt.KeyConstants;
import com.rise_world.gematik.accesskeeper.common.crypt.SignatureProviderFactory;
import com.rise_world.gematik.accesskeeper.common.dto.Endpoint;
import com.rise_world.gematik.accesskeeper.common.token.creation.TokenCreationStrategy;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtUtils;
import org.springframework.stereotype.Component;

@Component
public class AuthServerEntityStatementCreationStrategy implements TokenCreationStrategy {

    public static final String TYPE_ES_JWT = "entity-statement+jwt";

    private final JwsSignatureProvider signatureProvider;

    public AuthServerEntityStatementCreationStrategy(SignatureProviderFactory signatureProvider) {
        // @AFO: A_23196 - Delegieren der Signatur an den KMSSignatureProvider
        this.signatureProvider = signatureProvider.createSignatureProvider(Endpoint.EXT_AUTH);
    }

    @Override
    public String toToken(JwtClaims claims) {
        JwsCompactProducer producer = new JwsCompactProducer(createJWSHeader(), JwtUtils.claimsToJson(claims));
        return producer.signWith(signatureProvider);
    }

    private JwsHeaders createJWSHeader() {
        JwsHeaders headers = new JwsHeaders();
        headers.setHeader(JoseConstants.HEADER_TYPE, AuthServerEntityStatementCreationStrategy.TYPE_ES_JWT);
        headers.setKeyId(KeyConstants.PUK_IDP_SIG_SEK);
        // @AFO: A_23034 Setzen des Algorithmus des Providers
        headers.setAlgorithm(signatureProvider.getAlgorithm().getJwaName());
        return headers;
    }
}
