/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.service.validation;

import com.rise_world.gematik.accesskeeper.common.util.LangUtils;
import com.rise_world.gematik.accesskeeper.pairingdienst.Constants;

import static com.rise_world.gematik.accesskeeper.pairingdienst.util.Utils.BASE64URL_DECODER;

public class Validations {

    public static final Validation<String> KEY_IDENTIFIER = keyIdentifier -> {
        try {
            if (keyIdentifier == null || BASE64URL_DECODER.decode(keyIdentifier).length != Constants.LENGTH_KEY_IDENTIFIER) {
                return false;
            }
        }
        catch (Exception e) {
            return false;
        }

        return true;
    };

    public static final Validation<String> BASE64URL_NOPADDING = base64Url(false);

    private Validations() {
        // avoid instantiation
    }

    public static Validation<String> expect(String expected) {
        return value -> value != null && value.equals(expected);
    }

    public static Validation<String> base64Url(boolean withPadding) {
        return value -> value != null && LangUtils.isBase64Url(value, withPadding);
    }

    public static Validation<String> maxLength(int max) {
        return value -> value != null && value.length() <= max;
    }

}
