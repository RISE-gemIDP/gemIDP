/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.token;

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
public class EntityStatementCreationStrategy implements TokenCreationStrategy {

    public static final String TYPE_ES_JWT = "entity-statement+jwt";
    public static final String TYPE_IDP_LIST_JWT = "idp-list+jwt";

    private final JwsSignatureProvider signatureProvider;

    public EntityStatementCreationStrategy(SignatureProviderFactory signatureProvider) {
        this.signatureProvider = signatureProvider.createSignatureProvider(Endpoint.FEDMASTER);
    }

    @Override
    public String toToken(JwtClaims claims) {
        return toToken(claims, TYPE_ES_JWT);
    }

    /**
     * Creates a token based on the provided claims with a specific type used in the JWT header
     * @param claims    to be represented in the token
     * @param type      preferred type to be used in JWT header
     * @return assembled token
     */
    public String toToken(JwtClaims claims, String type) {
        JwsCompactProducer producer = new JwsCompactProducer(createJWSHeader(type), JwtUtils.claimsToJson(claims));
        return producer.signWith(signatureProvider);
    }

    private JwsHeaders createJWSHeader(String headerType) {
        JwsHeaders headers = new JwsHeaders();
        headers.setHeader(JoseConstants.HEADER_TYPE, headerType);
        headers.setKeyId(KeyConstants.PUK_FEDMASTER_SIG);
        headers.setAlgorithm(signatureProvider.getAlgorithm().getJwaName());
        return headers;
    }
}
