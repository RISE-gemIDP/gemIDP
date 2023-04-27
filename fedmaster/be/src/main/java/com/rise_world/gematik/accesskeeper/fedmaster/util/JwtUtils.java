/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.util;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantKeyDto;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObject;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jwk.PublicKeyUse;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utility class for JWT
 */
public class JwtUtils {

    private JwtUtils() {
        // avoid instantiation
    }

    /**
     * Safely extract a string property from a {@link JsonMapObject}. If multiple name parameters are set,
     * the method will try to navigate into a nested {@link JsonMapObject}. The last name parameter will be
     * used to extract a string property from the current part parameter using {@link JsonMapObject#getStringProperty(String)}.
     * If navigating to the nested part is not successful {@link Optional#empty()} will be returned.
     *
     * @param part the JsonMapObject where the property should be extracted
     * @param name the property names to be navigated. The last name represents the property with the string value
     * @return the string value of the property
     */
    public static Optional<String> getStringProperty(JsonMapObject part, String... name) {
        if (part == null || name.length == 0) {
            return Optional.empty();
        }
        if (name.length > 1) {
            Object child = part.getProperty(name[0]);
            if (child instanceof Map) {
                return getStringProperty(new JsonMapObject(CastUtils.cast((Map<?, ?>) child)), Arrays.copyOfRange(name, 1, name.length));
            }
            else {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(part.getStringProperty(name[0]));
    }

    /**
     * Safely extract a string property from a {@link JsonMapObject}. If multiple name parameters are set,
     * the method will try to navigate into a nested {@link JsonMapObject}. The last name parameter will be
     * used to extract a list property from the current part parameter using {@link JsonMapObject#getListStringProperty(String)}.
     * If navigating to the nested part is not successful {@link Optional#empty()} will be returned.
     *
     * @param part the JsonMapObject where the property should be extracted
     * @param name the property names to be navigated. The last name represents the property with the list value
     * @return the list of string value of the property
     */
    public static Optional<List<String>> getListStringProperty(JsonMapObject part, String... name) {
        if (part == null || name.length == 0) {
            return Optional.empty();
        }
        Object child = part.getProperty(name[0]);
        if (name.length > 1 && child instanceof Map) {
            return getListStringProperty(new JsonMapObject(CastUtils.cast((Map<?, ?>) child)), Arrays.copyOfRange(name, 1, name.length));
        }
        else if (name.length == 1 && child instanceof List) {
            return Optional.of(CastUtils.cast((List<?>) child));
        }
        else {
            return Optional.empty();
        }
    }

    /**
     * Creates a {@link JsonWebKey} from a {@link ParticipantKeyDto}. As key usage {@link PublicKeyUse#SIGN} will be set.
     * As algorithm {@link AlgorithmUtils#ES_SHA_256_ALGO} will be set. Key-id, curve,
     * x-coord and y-coord will be used from the provided key parameter.
     *
     * @param key to be converted
     * @return a jwk with the data used from the provided key
     */
    public static JsonWebKey toJsonWebKey(ParticipantKeyDto key) {
        if (key == null) {
            return new JsonWebKey();
        }

        if (key.getPem() == null) {
            return new JsonWebKey();
        }

        Optional<PublicKey> puk = PemUtils.readPublicKey(key.getPem());

        if (puk.isPresent() && puk.get() instanceof ECPublicKey ecPuk) {
            JsonWebKey jwk = JwkUtils.fromECPublicKey(ecPuk, JsonWebKey.EC_CURVE_P256, key.getKeyId());
            jwk.setPublicKeyUse(PublicKeyUse.SIGN);
            jwk.setAlgorithm(AlgorithmUtils.ES_SHA_256_ALGO);
            return jwk;
        }

        return new JsonWebKey();
    }
}
