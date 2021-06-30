/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.sec.SECObjectIdentifiers;
import org.bouncycastle.asn1.teletrust.TeleTrusTObjectIdentifiers;

import java.util.Collections;
import java.util.List;

public class Constants {

    public static final int LENGTH_KEY_IDENTIFIER = 32;

    public static final List<ASN1ObjectIdentifier> ALLOWED_EC_NAMED_CURVES_DEVICE_SECURE_ELEMENT =
        Collections.singletonList(SECObjectIdentifiers.secp256r1);

    public static final List<ASN1ObjectIdentifier> ALLOWED_EC_NAMED_CURVES_AUTH_CARD =
        Collections.singletonList(TeleTrusTObjectIdentifiers.brainpoolP256r1.intern());

    private Constants() {
        // avoid instantiation
    }
}
