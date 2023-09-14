/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.token.extraction;

import com.rise_world.gematik.accesskeeper.common.crypt.CryptoConstants;
import com.rise_world.gematik.accesskeeper.common.crypt.DecryptionProviderFactory;
import com.rise_world.gematik.accesskeeper.common.dto.TokenType;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.CertReaderException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.service.CertificateReaderService;
import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import com.rise_world.gematik.accesskeeper.common.token.extraction.AbstractClaimExtractionStrategy;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.IdpJwsJwtCompactConsumer;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.JweTokenParser;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.EpkValidation;
import com.rise_world.gematik.accesskeeper.common.token.extraction.validation.HeaderExpiry;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.EcDsaJwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.PublicKeyJwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.security.cert.X509Certificate;
import java.time.Clock;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Qualifier("signedChallenge")
public class SignedChallengeExtractionStrategy extends AbstractClaimExtractionStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(SignedChallengeExtractionStrategy.class);

    private static final String[] RELEVANT_CLAIMS = {ClaimUtils.NESTED_TOKEN};

    private static final Set<String> ALLOWED_ALGOS = new HashSet<>(Arrays.asList(SignatureAlgorithm.PS256.getJwaName(), CryptoConstants.SIG_ALG_BRAINPOOL_P256_R1));

    private ChallengeExtractionStrategy challengeExtractionStrategy;
    private CertificateReaderService certificateReaderService;

    @Autowired
    public SignedChallengeExtractionStrategy(Clock clock,
            ChallengeExtractionStrategy challengeExtractionStrategy,
            CertificateReaderService certificateReaderService,
            DecryptionProviderFactory decryptionFactory) {
        // @AFO: A_20699-02 Entschlüsseln der signierten Challenge
        super(new JweTokenParser(decryptionFactory.createDecryptionProvider(TokenType.CHALLENGE),
                ErrorCodes.AUTH_INVALID_CHALLENGE,
                new HeaderExpiry(clock, ErrorCodes.AUTH_CHALLENGE_MISSING_EXPIRY, ErrorCodes.AUTH_CHALLENGE_EXPIRED),
                new EpkValidation(CryptoConstants.JWE_BRAINPOOL_CURVE, ErrorCodes.AUTH_INVALID_CHALLENGE)));
        this.challengeExtractionStrategy = challengeExtractionStrategy;
        this.certificateReaderService = certificateReaderService;
    }

    @Override
    protected JwtClaims extractInternal(IdpJwsJwtCompactConsumer consumer, Map<String, Object> context) {
        final JwsHeaders headers = consumer.getJwsHeaders();
        JwtClaims claims = consumer.getJwtClaims();

        validateHeaders(headers);

        if (!ClaimUtils.containsAllClaims(claims, RELEVANT_CLAIMS)) {
            LOG.error("Token doesn't contain all relevant claims");
            throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_CHALLENGE);
        }

        JwtClaims challenge = challengeExtractionStrategy.extractAndValidate(claims.getStringProperty(ClaimUtils.NESTED_TOKEN));

        // @AFO: A_20951-01 - Das AUT-Zertifikat wird aus den Headern extrahiert
        final String autCert = headers.getX509Chain().get(0);
        X509Certificate x509Certificate = null;
        try {
            x509Certificate = certificateReaderService.parseCertificate(autCert);
        }
        catch (CertReaderException c) {
            throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_X509_CERT, c);
        }
        validateClientSignature(consumer, x509Certificate);
        challenge.setProperty(ClaimUtils.CERTIFICATE, autCert);

        return challenge;
    }

    // @AFO: A_20951-01 - Public Key wird aus dem AUT-Zertifikat extrahiert und die Signatur des Challenge-Tokens damit &uuml;berpr&uuml;ft
    // @AFO: GS-A_4357 - RSA und ECDSA Client-Signaturen werden anhand der Algorithmen aus Tab_KRYPT_002 und Tab_KRYPT_002a gepr&uuml;ft.
    // @AFO: A_17207 Prüfung des alg-Headers und Signatur
    private void validateClientSignature(JwsJwtCompactConsumer challengeToken, X509Certificate x509Certificate) {
        if (StringUtils.isEmpty(challengeToken.getEncodedSignature())) {
            throw new AccessKeeperException(ErrorCodes.AUTH_MISSING_CLIENT_SIGNATURE);
        }

        JwsHeaders headers = challengeToken.getJwsHeaders();

        JwsSignatureVerifier jwsSignatureVerifier;
        if (SignatureAlgorithm.PS256.getJwaName().equals(headers.getAlgorithm())) {
            jwsSignatureVerifier = new PublicKeyJwsSignatureVerifier(x509Certificate.getPublicKey(), SignatureAlgorithm.PS256);
        }
        else {
            jwsSignatureVerifier = new EcDsaJwsSignatureVerifier(x509Certificate.getPublicKey(), SignatureAlgorithm.ES256);
        }

        if (!challengeToken.verifySignatureWith(jwsSignatureVerifier)) {
            throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_CLIENT_SIGNATURE);
        }
    }

    private void validateHeaders(JwsHeaders headers) {
        if (headers.getAlgorithm() == null || headers.getContentType() == null) {
            throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_CHALLENGE);
        }

        if (!ALLOWED_ALGOS.contains(headers.getAlgorithm())) {
            throw new AccessKeeperException(ErrorCodes.AUTH_WRONG_ALGO);
        }
        if (!ClaimUtils.NESTED_TOKEN_CTY_VALUE.equals(headers.getContentType())) {
            throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_CHALLENGE);
        }

        Object x509Chain = headers.getProperty(JoseConstants.HEADER_X509_CHAIN);
        if (!(x509Chain instanceof List)) {
            throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_X509_CERT);
        }
        List<?> x509List = (List<?>) x509Chain;
        if (x509List.size() != 1 || !(x509List.get(0) instanceof String)) {
            throw new AccessKeeperException(ErrorCodes.AUTH_INVALID_X509_CERT);
        }
    }
}
