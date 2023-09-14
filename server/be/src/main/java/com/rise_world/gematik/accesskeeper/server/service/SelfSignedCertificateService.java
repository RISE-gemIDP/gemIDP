/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.common.crypt.CryptoConstants;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jwk.PublicKeyUse;
import org.apache.cxf.transport.http.HTTPConduit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
@SuppressWarnings("java:S112") // we don't need explicit handling
public class SelfSignedCertificateService {

    private static final Logger LOG = LoggerFactory.getLogger(SelfSignedCertificateService.class);

    private final String keystorePath;
    private final String keystorePw;
    private final Clock clock;
    private final AtomicReference<MtlsKeys> mtlsKeys;

    @Autowired
    public SelfSignedCertificateService(@Value("${federation.mtls.keystore}") String keystorePath,
                                        @Value("${federation.mtls.password}") String keystorePw,
                                        Clock clock) {
        this.keystorePath = keystorePath;
        this.keystorePw = keystorePw;
        this.clock = clock;

        this.mtlsKeys = new AtomicReference<>(new MtlsKeys(new KeyManager[]{}, new TrustManager[]{}, Collections.emptyList()));
    }

    @Scheduled(fixedDelayString = "${federation.mtls.timer.delay}")
    public void loadKeystore() {
        LOG.info("Reloading mTLS certificates");

        final Path keystoreFile = Path.of(this.keystorePath);
        if (!Files.exists(keystoreFile) || Files.isDirectory(keystoreFile)) {
            LOG.error("mTLS keystore '{}' is not a valid file", keystoreFile.toAbsolutePath());
            return;
        }

        char[] keystorePwChars = keystorePw.toCharArray();
        KeyStore mtlsKeystore;
        try {
            mtlsKeystore = KeyStore.getInstance("PKCS12", CryptoConstants.BOUNCY_CASTLE);
            mtlsKeystore.load(Files.newInputStream(keystoreFile), keystorePwChars);
        }
        catch (KeyStoreException | NoSuchProviderException | CertificateException | IOException | NoSuchAlgorithmException e) {
            LOG.error("Failed to read mTLS keystore: '{}'", keystoreFile.toAbsolutePath(), e);
            throw new RuntimeException(e);
        }

        List<KeyStore.PrivateKeyEntry> certificateList = new ArrayList<>();
        try {
            final Enumeration<String> aliases = mtlsKeystore.aliases();
            while (aliases.hasMoreElements()) {
                final String entryAlias = aliases.nextElement();
                final Certificate[] certificate = mtlsKeystore.getCertificateChain(entryAlias);
                final PrivateKey key = (PrivateKey) mtlsKeystore.getKey(entryAlias, keystorePwChars);
                final KeyStore.PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(key, certificate);
                certificateList.add(privateKeyEntry);
            }
        }
        catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new RuntimeException("failed to read keystore entries", e);
        }

        List<KeyStore.PrivateKeyEntry> filteredCertificates = certificateList.stream()
            .filter(c -> c.getCertificate() instanceof X509Certificate)
            .filter(c -> c.getCertificate().getPublicKey() instanceof ECPublicKey)
            .filter(c -> ((X509Certificate) c.getCertificate()).getNotAfter().after(Date.from(clock.instant())))
            .filter(c -> {
                Instant notAfter = ((X509Certificate) c.getCertificate()).getNotAfter().toInstant();
                Instant notBefore = ((X509Certificate) c.getCertificate()).getNotBefore().toInstant();
                // @AFO: A_23185 Zertifikate, die länger als 398 Tage gültig sind, werden entfernt
                return !notBefore.plus(398, ChronoUnit.DAYS).isBefore(notAfter);
            })
            .toList();

        KeyManager[] keyManagers = createKeyManagers(filteredCertificates);
        List<X509Certificate> tlsCertificates = filteredCertificates
            .stream()
            .map(e -> (X509Certificate) e.getCertificate())
            .toList();

        TrustManager[] trustManagers;
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            trustManagers = tmf.getTrustManagers();
        }
        catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException(e);
        }

        this.mtlsKeys.set(new MtlsKeys(keyManagers, trustManagers, tlsCertificates));
    }

    private JsonWebKey transformCertificate(X509Certificate x509Certificate) {
        JsonWebKey jsonWebKey = JwkUtils.fromECPublicKey((ECPublicKey) x509Certificate.getPublicKey(), JsonWebKey.EC_CURVE_P256);
        jsonWebKey.setPublicKeyUse(PublicKeyUse.SIGN);
        jsonWebKey.setAlgorithm(AlgorithmUtils.ES_SHA_256_ALGO);
        try {
            jsonWebKey.setX509Chain(List.of(Base64.getEncoder().encodeToString(x509Certificate.getEncoded())));
        }
        catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }

        String keyId = JwkUtils.getThumbprint(jsonWebKey);
        jsonWebKey.setKeyId(keyId);
        return jsonWebKey;
    }

    public List<JsonWebKey> getValidKeys() {
        return this.mtlsKeys.get().tlsCertificates().stream().map(this::transformCertificate).toList();
    }

    public KeyManager[] getKeyManagers() {
        return this.mtlsKeys.get().keyManagers;
    }

    public TrustManager[] getTrustManagers() {
        return this.mtlsKeys.get().trustManagers;
    }

    private static KeyManager[] createKeyManagers(List<KeyStore.PrivateKeyEntry> entries) {
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(null);
            char[] dummy = "dummy".toCharArray();
            int i = 1;
            for (KeyStore.PrivateKeyEntry entry : entries) {
                ks.setEntry("entry_" + i, entry, new KeyStore.PasswordProtection(dummy));
                i++;
            }

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X.509");
            keyManagerFactory.init(ks, dummy);
            return keyManagerFactory.getKeyManagers();
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot setup client-certificate authentication", e);
        }
    }

    public void secureWebClient(Client client) {
        ClientConfiguration config = WebClient.getConfig(client);
        HTTPConduit conduit = config.getHttpConduit();
        conduit.getTlsClientParameters().setKeyManagers(getKeyManagers());
        conduit.getTlsClientParameters().setTrustManagers(getTrustManagers());
    }

    private record MtlsKeys(KeyManager[] keyManagers, TrustManager[] trustManagers, List<X509Certificate> tlsCertificates) {
    }
}
