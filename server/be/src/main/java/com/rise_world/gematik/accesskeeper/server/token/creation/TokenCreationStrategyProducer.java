/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.token.creation;

import com.rise_world.gematik.accesskeeper.common.crypt.EncryptionProviderFactory;
import com.rise_world.gematik.accesskeeper.common.crypt.KeyConstants;
import com.rise_world.gematik.accesskeeper.common.crypt.KeyProvider;
import com.rise_world.gematik.accesskeeper.common.crypt.SignatureProviderFactory;
import com.rise_world.gematik.accesskeeper.common.dto.Endpoint;
import com.rise_world.gematik.accesskeeper.common.dto.TokenType;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.common.JoseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.function.Supplier;

@Configuration
public class TokenCreationStrategyProducer {

    private static final String ACCESS_TOKEN_TYPE = "at+JWT";

    @Autowired
    private SignatureProviderFactory sigProvFactory;

    @Autowired
    private EncryptionProviderFactory encProvFactory;

    /**
     * Creates a TokenCreationStrategy for the Challenge
     *
     * @return a suitable TokenCreationStrategy
     * @AFO: A_20521-02 - Signieren der Challenge mit passendem Schlüsselmaterial
     */
    @Bean
    @Qualifier("challengeStrategy")
    public TokenCreationStrategy challengeStrategy() {
        return new SignStrategy(sigProvFactory.createSignatureProvider(Endpoint.AUTH), KeyConstants.PUK_IDP_SIG);
    }

    /**
     * Creates a TokenCreationStrategy for the SSO Token
     *
     * @return a suitable TokenCreationStrategy
     * @AFO: A_20696 - Verschlüsselung des "SSO_TOKEN"
     * @AFO: A_20695-01 - Signieren des "SSO_TOKEN"
     */
    @Bean
    @Qualifier("ssoTokenStrategy")
    public TokenCreationStrategy ssoTokenStrategy() {
        return new EncryptAndSignStrategy(encProvFactory.createEncryptionProvider(TokenType.SSO),
            sigProvFactory.createSignatureProvider(Endpoint.AUTH),
            KeyConstants.PUK_IDP_SIG);
    }

    /**
     * Creates a AesTokenCreationStrategy for Access Token
     *
     * @return a suitable AesTokenCreationStrategy
     * @AFO: A_20327-02 - Signatur des "ACCESS_TOKEN"
     */
    @Bean
    @Qualifier("accessTokenStrategyFactory")
    public AesTokenCreationStrategy accessTokenStrategyFactory() {
        return new AesTokenCreationStrategyImpl(sigProvFactory.createSignatureProvider(Endpoint.TOKEN), ACCESS_TOKEN_TYPE);
    }

    /**
     * Creates a AesTokenCreationStrategy for ID Token
     *
     * @return a suitable AesTokenCreationStrategy
     * @AFO: A_20327-02 - Signatur des "ID_TOKEN"
     */
    @Bean
    @Qualifier("idTokenStrategyFactory")
    public AesTokenCreationStrategy idTokenStrategyFactory() {
        return new AesTokenCreationStrategyImpl(sigProvFactory.createSignatureProvider(Endpoint.TOKEN), JoseConstants.TYPE_JWT);
    }

    /**
     * Creates a TokenCreationStrategy for Authorization Code
     *
     * @return a suitable TokenCreationStrategy
     * @AFO: A_20521-01 - Inhalt der Challenge an das Authenticator-Modul
     * @AFO: A_20695 - Signieren des "SSO_TOKEN"
     * @AFO: A_20319 - Signatur des "AUTHORIZATION_CODE"
     * @AFO: A_21317 - Verschlüsselung des "AUTHORIZATION CODE"
     */
    @Bean
    @Qualifier("authCodeStrategy")
    public TokenCreationStrategy authCodeStrategy() {
        return new EncryptAndSignStrategy(encProvFactory.createEncryptionProvider(TokenType.AUTH_CODE),
            sigProvFactory.createSignatureProvider(Endpoint.AUTH),
            KeyConstants.PUK_IDP_SIG);
    }

    /**
     * Creates a TokenCreationStrategy for Discovery Document
     *
     * @param keyProvider provider to retrieve puk_disc_sig
     * @return a suitable TokenCreationStrategy
     * @AFO: A_20691-01 - Die SignStrategy f&uuml;r das Discovery Document wird mit disc_sig initialisiert.
     * @AFO: A_20591-01 - Die SignStrategy f&uuml;r das Discovery Document wird mit disc_sig initialisiert.
     * @AFO: A_20591-01 - Das Laden des Zertifikats im Zertifikats-Supplier wird an den KeyProvider delegiert
     * @AFO: A_20687-01 - puk_disc_sig wird mittels KeyProvider geladen
     */
    @Bean
    @Qualifier("discStrategy")
    public TokenCreationStrategy discStrategy(KeyProvider keyProvider) {
        Supplier<String> certSupplier = () -> getPublicKeyAsPem(keyProvider.getCertificate(KeyConstants.PUK_DISC_SIG));
        return new SignStrategy(sigProvFactory.createSignatureProvider(Endpoint.DISC), KeyConstants.PUK_DISC_SIG, JoseType.JWT.toString(), certSupplier);
    }

    @SuppressWarnings("squid:S00112") // CertificateEncodingException needs no special handling
    private String getPublicKeyAsPem(X509Certificate certificate) {
        try {
            return Base64.getEncoder().encodeToString(certificate.getEncoded());
        }
        catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
