/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.token.extraction.validation;

import com.rise_world.gematik.accesskeeper.common.crypt.CryptoConstants;
import com.rise_world.gematik.accesskeeper.common.crypt.KeyConstants;
import com.rise_world.gematik.accesskeeper.common.crypt.KeyProvider;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorMessage;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.IdpJwsJwtCompactConsumer;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.EcDsaJwsSignatureVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;

/**
 * Verifies the (server) signature of a JWT
 */
public class ServerSignatureValidation implements ClaimValidation<IdpJwsJwtCompactConsumer> {

    private static final Logger LOG = LoggerFactory.getLogger(ServerSignatureValidation.class);

    private KeyProvider keyProvider;

    private ErrorMessage signatureMissingError;
    private ErrorMessage wrongAlgoError;
    private ErrorMessage invalidSignatureError;

    public ServerSignatureValidation(KeyProvider keyProvider, ErrorMessage error) {
        this(keyProvider, error, error, error);
    }

    public ServerSignatureValidation(KeyProvider keyProvider, ErrorMessage signatureMissingError, ErrorMessage wrongAlgoError, ErrorMessage invalidSignatureError) {
        this.keyProvider = keyProvider;
        this.signatureMissingError = signatureMissingError;
        this.wrongAlgoError = wrongAlgoError;
        this.invalidSignatureError = invalidSignatureError;
    }

    /**
     * Validate if the token has a valid EC brainpool256r1 signature, issued by the IDP
     *
     * @param consumer the signed token
     *
     * @AFO: GS-A_4357 - ECDSA Server-Signatur wird anhand der Algorithmen aus Tab_KRYPT_002a gepr&uuml;ft.
     * @AFO: A_20948-01 - Die Signatur der vom IDP ausgestellten Token wird anhand von puk_idp_sig gepr&uuml;ft
     * @AFO: A_17207 Prüfung des alg-Headers und Signatur
     */
    public void validate(IdpJwsJwtCompactConsumer consumer) {

        if (StringUtils.isEmpty(consumer.getEncodedSignature())) {
            LOG.warn("server signature is missing");
            throw new AccessKeeperException(signatureMissingError);
        }

        if (!CryptoConstants.SIG_ALG_BRAINPOOL_P256_R1.equals(consumer.getJwsHeaders().getAlgorithm())) {
            LOG.warn("signature algorithm {} is not supported", consumer.getJwsHeaders().getAlgorithm());
            throw new AccessKeeperException(wrongAlgoError);
        }

        X509Certificate certificate = keyProvider.getCertificate(KeyConstants.PUK_IDP_SIG);
        EcDsaJwsSignatureVerifier signatureVerifier = new EcDsaJwsSignatureVerifier(certificate.getPublicKey(), SignatureAlgorithm.ES256);

        boolean hasValidSignature = consumer.verifySignatureWith(signatureVerifier);

        if (!hasValidSignature) {
            // @AFO: A_20504 Token besitzt keine gültige Signatur, damit wird die Verarbeitung mit einer Exception abgebrochen
            throw new AccessKeeperException(invalidSignatureError);
        }
    }
}
