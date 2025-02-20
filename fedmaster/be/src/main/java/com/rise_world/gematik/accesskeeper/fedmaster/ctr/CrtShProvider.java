/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

import com.rise_world.gematik.accesskeeper.common.util.DigestUtils;
import com.rise_world.gematik.accesskeeper.crtsh.CrtShMonitor;
import com.rise_world.gematik.accesskeeper.crtsh.CrtShPageParser;
import com.rise_world.gematik.accesskeeper.crtsh.CrtShPageParsingException;
import com.rise_world.gematik.accesskeeper.crtsh.CrtShRecord;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.CtRecordDto;
import com.rise_world.gematik.accesskeeper.fedmaster.util.PemUtils;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Objects.isNull;

@Service
public class CrtShProvider implements CertificateTransparencyProvider {

    private static final Logger LOG = LoggerFactory.getLogger(CrtShProvider.class);

    private final CrtShMonitor client;
    private final CrtShPageParser parser;
    private final CircuitBreaker circuitBreaker;

    public CrtShProvider(CrtShMonitorFactory factory,
                  CrtShPageParser parser,
                  CircuitBreakerRegistry circuitBreakerRegistry) {
        this.parser = parser;
        client = factory.createMonitor();
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("crtShCtrProvider");
    }

    @Override
    public List<CtRecordDto> fetch(String domain) {

        try {
            return circuitBreaker.executeCallable(() -> client.fetch(domain, CrtShMonitor.EXCLUDE_VALUE, CrtShMonitor.MATCH_VALUE, CrtShMonitor.DEDUPLICATION_VALUE)
                .stream()
                .mapMulti(toRecord(domain))
                .toList());
        }
        catch (WebApplicationException e) {
            throw new CtrServiceException("Request to crt.sh failed for domain " + domain, e);
        }
        catch (CallNotPermittedException e) {
            throw new CertificateTransparencyProviderException("crt.sh is not available", e);
        }
        catch (ProcessingException e) {
            if ((e.getCause() instanceof SocketTimeoutException) || (e.getCause() instanceof ConnectException) ||
                (e.getCause() instanceof NoRouteToHostException) || (e.getCause() instanceof UnknownHostException)) {
                LOG.warn("crt.sh is not available", e);
                throw new CertificateTransparencyProviderException("crt.sh could not be reached", e);
            }
            else {
                LOG.error("Unexpected error on accessing crt.sh.", e);
                throw new CertificateTransparencyProviderException("crt.sh could not be accessed", e);
            }
        }
        catch (CertificateTransparencyProviderException e) {
            throw e;
        }
        catch (CrtShPageParsingException e) {
            throw new CtrServiceException("error parsing crt.sh page", e);
        }
        catch (Exception e) {
            throw new CertificateTransparencyProviderException("crt.sh could not be accessed", e);
        }
    }

    private BiConsumer<CrtShRecord, Consumer<CtRecordDto>> toRecord(String domain) {

        return (crtShRecord, consumer) -> {

            var hash = calculateHash(crtShRecord.getId(), domain);
            if (hash.isEmpty()) {
                return;
            }

            consumer.accept(new CtRecordDto(crtShRecord.getId(),
                List.of(domain),
                crtShRecord.getIssuer(),
                hash.get(),
                crtShRecord.getNotBefore(),
                crtShRecord.getNotAfter(),
                fetchRevokation(crtShRecord.getId())));
        };
    }

    private Optional<String> calculateHash(String id, String domain) {

        var certificate = client.certificate(id);
        if (isNull(certificate)) {
            return Optional.empty();
        }

        return PemUtils.parseCertificate(certificate)
            .flatMap(cert -> extractPublicKey(id, cert, domain))
            .map(DigestUtils::sha256)
            .map(Hex::toHexString);
    }

    private boolean fetchRevokation(String id) {
        var html = client.certificatePage(id, CrtShMonitor.OPTION_OCSP);
        return parser.parse(html).revoked();
    }

    private Optional<byte[]> extractPublicKey(String id, Certificate certificate, String domain) {
        try {
            return Optional.of(certificate.getSubjectPublicKeyInfo().getEncoded());
        }
        catch (IOException e) {
            throw new CertificateTransparencyProviderException("error extracting public key for certificate %s, domain: %s".formatted(id, domain), e);
        }
    }

}
