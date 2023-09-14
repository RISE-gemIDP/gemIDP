/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common;

import com.rise_world.gematik.idp.server.api.discovery.JsonWebKey;

import static org.apache.cxf.rs.security.jose.jwk.JsonWebKey.EC_CURVE;
import static org.apache.cxf.rs.security.jose.jwk.JsonWebKey.EC_X_COORDINATE;
import static org.apache.cxf.rs.security.jose.jwk.JsonWebKey.EC_Y_COORDINATE;

public class JwkUtils {

    private JwkUtils() {
        // avoid instantiation
    }

    public static JsonWebKey transform(org.apache.cxf.rs.security.jose.jwk.JsonWebKey src) {
        JsonWebKey dst = new JsonWebKey();
        dst.setKid(src.getKeyId());
        if (src.getPublicKeyUse() != null) {
            dst.setUse(src.getPublicKeyUse().toString());
        }
        dst.setAlg(src.getAlgorithm());
        if (src.getKeyType() != null) {
            dst.setKty(src.getKeyType().toString());
        }
        dst.setCrv(src.getStringProperty(EC_CURVE));
        dst.setX(src.getStringProperty(EC_X_COORDINATE));
        dst.setY(src.getStringProperty(EC_Y_COORDINATE));
        dst.setX5c(src.getX509Chain());

        return dst;
    }
}
