/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.token.extraction.validation;

import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorMessage;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.ClaimValidation;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AESValidation implements ClaimValidation<JweHeaders> {

    private static final Logger LOG = LoggerFactory.getLogger(AESValidation.class);
    private ErrorMessage errorMessage;

    public AESValidation(ErrorMessage errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public void validate(JweHeaders claims) {
        if (!KeyAlgorithm.DIRECT.getJwaName().equals(claims.getAlgorithm())) {
            LOG.warn("Key algorithm not supported");
            throw new AccessKeeperException(errorMessage);
        }

        if (!ContentAlgorithm.A256GCM.getJwaName().equals(claims.getHeader(JoseConstants.JWE_HEADER_CONTENT_ENC_ALGORITHM))) {
            LOG.warn("Content encryption not supported");
            throw new AccessKeeperException(errorMessage);
        }

        if (claims.getKeyId() == null ||
            StringUtils.length(claims.getKeyId()) != 4 ||
            !StringUtils.isNumeric(claims.getKeyId())) {
            LOG.warn("kid is invalid");
            throw new AccessKeeperException(errorMessage);
        }
    }

}
