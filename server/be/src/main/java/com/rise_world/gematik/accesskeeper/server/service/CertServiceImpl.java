/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.common.crypt.CryptoConstants;
import com.rise_world.gematik.accesskeeper.common.crypt.KeyConstants;
import com.rise_world.gematik.accesskeeper.common.crypt.KeyProvider;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.common.KeyManagementUtils;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jwk.PublicKeyUse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Properties;
import java.util.function.Supplier;

@Service
public class CertServiceImpl implements CertService {

    private final KeyProvider keyProvider;

    @Autowired
    public CertServiceImpl(KeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    private JsonWebKey getJwkFromX509TiCert(String kid, X509Certificate cert, PublicKeyUse keyUse) {
        JsonWebKey key = getJwkFromPublicKey(kid, cert.getPublicKey(), keyUse, this::createBpAlgorithmProperties);
        key.setX509Chain(KeyManagementUtils.encodeX509CertificateChain(Collections.singletonList(cert)));
        return key;
    }

    private JsonWebKey getJwkFromPublicKey(String kid, PublicKey pubKey, PublicKeyUse keyUse, Supplier<Properties> keyPropertySupplier) {
        final JsonWebKey jsonWebKey = JwkUtils.fromPublicKey(pubKey, keyPropertySupplier.get(), JsonWebKey.KEY_ALGO);
        jsonWebKey.setKeyId(kid);
        jsonWebKey.setPublicKeyUse(keyUse);
        return jsonWebKey;
    }

    private Properties createBpAlgorithmProperties() {
        Properties algorithmProperties = new Properties();
        algorithmProperties.setProperty(JoseConstants.RSSEC_EC_CURVE, CryptoConstants.JWE_BRAINPOOL_CURVE);
        algorithmProperties.setProperty(JsonWebKey.KEY_ALGO, AlgorithmUtils.ES_SHA_256_ALGO);
        return algorithmProperties;
    }

    private Properties createEs256AlgorithmProperties() {
        Properties algorithmProperties = new Properties();
        algorithmProperties.setProperty(JoseConstants.RSSEC_EC_CURVE, JsonWebKey.EC_CURVE_P256);
        algorithmProperties.setProperty(JsonWebKey.KEY_ALGO, AlgorithmUtils.ES_SHA_256_ALGO);
        return algorithmProperties;
    }

    @Override
    public JsonWebKey getDiscoveryCert() {
        return getJwkFromX509TiCert(KeyConstants.PUK_DISC_SIG, keyProvider.getCertificate(KeyConstants.PUK_DISC_SIG), PublicKeyUse.SIGN);
    }

    @Override
    public JsonWebKey getSignatureCert() {
        return getJwkFromX509TiCert(KeyConstants.PUK_IDP_SIG, keyProvider.getCertificate(KeyConstants.PUK_IDP_SIG), PublicKeyUse.SIGN);
    }

    @Override
    public JsonWebKey getEncryptionKey() {
        return getJwkFromPublicKey(KeyConstants.PUK_IDP_ENC, keyProvider.getKey(KeyConstants.PUK_IDP_ENC), PublicKeyUse.ENCRYPT, this::createBpAlgorithmProperties);
    }

    @Override
    public JsonWebKey getSekIdpSignatureKey() {
        JsonWebKey jwk = getJwkFromPublicKey(KeyConstants.PUK_IDP_SIG_SEK,
            keyProvider.getKey(KeyConstants.PUK_IDP_SIG_SEK), PublicKeyUse.SIGN, this::createEs256AlgorithmProperties);
        jwk.setAlgorithm(AlgorithmUtils.ES_SHA_256_ALGO);
        return jwk;
    }
}
