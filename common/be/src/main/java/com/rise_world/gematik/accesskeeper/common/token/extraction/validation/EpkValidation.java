/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.token.extraction.validation;

import com.rise_world.gematik.accesskeeper.common.crypt.CryptoConstants;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorMessage;
import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import com.rise_world.gematik.accesskeeper.common.util.LangUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class EpkValidation implements ClaimValidation<JweHeaders> {

    private static final int COORDINATE_MAX_LENGTH = 44;
    private static final Logger LOG = LoggerFactory.getLogger(EpkValidation.class);
    private ErrorMessage errorMessage;

    public EpkValidation(ErrorMessage errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public void validate(JweHeaders claims) {
        if (!KeyAlgorithm.ECDH_ES_DIRECT.getJwaName().equals(claims.getAlgorithm())) {
            LOG.warn("Key algorithm not supported");
            throw new AccessKeeperException(errorMessage);
        }

        if (!ContentAlgorithm.A256GCM.getJwaName().equals(claims.getHeader(JoseConstants.JWE_HEADER_CONTENT_ENC_ALGORITHM))) {
            LOG.warn("Content encryption not supported");
            throw new AccessKeeperException(errorMessage);
        }

        if (claims.containsHeader(ClaimUtils.HEADER_APU)) {
            LOG.warn("APU is not expected");
            throw new AccessKeeperException(errorMessage);
        }

        if (claims.containsHeader(ClaimUtils.HEADER_APV)) {
            LOG.warn("APV is not expected");
            throw new AccessKeeperException(errorMessage);
        }

        JsonWebKey epk = extractWebKey(claims);

        if (epk == null) {
            LOG.warn("Ephemeral public key is missing");
            throw new AccessKeeperException(errorMessage);
        }

        if (!Objects.equals(JsonWebKey.KEY_TYPE_ELLIPTIC, epk.getProperty(JsonWebKey.KEY_TYPE)) ||
            !CryptoConstants.JWE_BRAINPOOL_CURVE.equals(epk.getProperty(JsonWebKey.EC_CURVE)) ||
            !epk.containsProperty(JsonWebKey.EC_X_COORDINATE) ||
            !epk.containsProperty(JsonWebKey.EC_Y_COORDINATE) ||
            !isBase64WithMaxLength(epk.getStringProperty(JsonWebKey.EC_X_COORDINATE), COORDINATE_MAX_LENGTH) ||
            !isBase64WithMaxLength(epk.getStringProperty(JsonWebKey.EC_Y_COORDINATE), COORDINATE_MAX_LENGTH)) {
            LOG.warn("Ephemeral public key is invalid");
            throw new AccessKeeperException(errorMessage);
        }
    }

    private JsonWebKey extractWebKey(JweHeaders claims) {
        try {
            return claims.getJsonWebKey(ClaimUtils.HEADER_EPK);
        }
        catch (Exception e) {
            LOG.warn("Ephemeral public key format is not valid");
            return null;
        }
    }

    private boolean isBase64WithMaxLength(String property, int length) {
        return property != null &&  LangUtils.isBase64Url(property, false) && StringUtils.length(property) <= length;
    }
}
