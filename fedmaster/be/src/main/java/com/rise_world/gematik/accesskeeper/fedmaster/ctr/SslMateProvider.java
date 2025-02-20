/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.ctr;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.rise_world.gematik.accesskeeper.common.util.LoggingInvocationHandler;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.CtRecordDto;
import com.rise_world.gematik.accesskeeper.sslmate.CertificateTransparencyRecord;
import com.rise_world.gematik.accesskeeper.sslmate.SslMateMonitor;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.rise_world.gematik.accesskeeper.fedmaster.FederationMasterConfiguration.USER_AGENT;

@Service
public class SslMateProvider implements CertificateTransparencyProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SslMateProvider.class);

    private static final List<String> EXPANDED_ATTR = Arrays.asList("issuer", "dns_names");

    private final JacksonJsonProvider jacksonJsonProvider;
    private final SslMateConfiguration configuration;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final SslMateRateLimiter rateLimiter;

    public SslMateProvider(SslMateConfiguration configuration,
                           JacksonJsonProvider jacksonJsonProvider,
                           CircuitBreakerRegistry circuitBreakerRegistry,
                           SslMateRateLimiter rateLimiter) {
        this.configuration = configuration;
        this.jacksonJsonProvider = jacksonJsonProvider;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public List<CtRecordDto> fetch(String domain) {
        List<CertificateTransparencyRecord> records = internalFetch(this.createClient(), domain);

        return records.stream()
            .map(this::toRecord)
            .toList();
    }

    private CtRecordDto toRecord(CertificateTransparencyRecord ctr) {
        return new CtRecordDto(ctr.id(),
            ctr.dnsNames(),
            ctr.issuer().name(),
            ctr.pubKeyHash(),
            ctr.notBefore(),
            ctr.notAfter(),
            ctr.revoked());
    }

    private List<CertificateTransparencyRecord> internalFetch(SslMateMonitor client, String domain) {
        LOG.debug("fetching domain {}", domain);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(configuration.getEndpoint());
        LinkedList<CertificateTransparencyRecord> records = new LinkedList<>();
        boolean foundRecords;
        int pageCount = 0;
        boolean pageLimitReached = false;
        try {
            do {
                String after = records.isEmpty() ? null : records.getLast().id();

                rateLimiter.acquire();

                List<CertificateTransparencyRecord> result = circuitBreaker.executeCallable(() -> client.issuances(domain,
                    false,
                    true,
                    after,
                    EXPANDED_ATTR));

                foundRecords = !result.isEmpty();

                if (foundRecords) {
                    LOG.debug("found {} record(s) for domain {}", result.size(), domain);
                    records.addAll(result);
                }

                if (++pageCount >= configuration.getPageLimit()) {
                    pageLimitReached = true;
                    LOG.warn("page limit for domain {} reached", domain);
                }

            } while (foundRecords && !pageLimitReached);
        }
        catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == HttpStatus.SC_FORBIDDEN) {
                // sslmate rejects requests for non-public suffix list/unknown TLDs
                throw new RejectedCtrRequestException("SslMate rejected request for domain " + domain, e);
            }
            throw new CtrServiceException("Request to SslMate failed for domain " + domain, e);
        }
        catch (CallNotPermittedException e) {
            // circuitbreaker denies access
            throw new CertificateTransparencyProviderException("SslMate is not available", e);
        }
        catch (ProcessingException e) {
            if ((e.getCause() instanceof SocketTimeoutException) || (e.getCause() instanceof ConnectException) ||
                (e.getCause() instanceof NoRouteToHostException) || (e.getCause() instanceof UnknownHostException)) {
                LOG.warn("SslMate is not available", e);
                throw new CertificateTransparencyProviderException("SslMate could not be reached", e);
            }
            else {
                LOG.error("Unexpected error on accessing SslMate.", e);
                throw new CertificateTransparencyProviderException("SslMate could not be accessed", e);
            }
        }
        catch (RequestLimitExceededException e) {
            // we want to rethrow the exception to give the caller a chance to decide how to deal with it
            throw e;
        }
        catch (Exception e) {
            throw new CertificateTransparencyProviderException("SslMate could not be accessed", e);
        }

        return records;
    }

    private SslMateMonitor createClient() {
        SslMateMonitor sslMateMonitor = JAXRSClientFactory.create(configuration.getEndpoint(), SslMateMonitor.class, Collections.singletonList(jacksonJsonProvider), false);
        Client restClient = WebClient.client(sslMateMonitor);

        if (StringUtils.isNotEmpty(configuration.getApiKey())) {
            restClient.authorization("Bearer " + configuration.getApiKey());
        }
        ClientConfiguration config = WebClient.getConfig(restClient);
        config.getResponseContext().put("buffer.proxy.response", Boolean.TRUE); // GEMIDP-1244 prevent connection leaks

        HTTPConduit conduit = config.getHttpConduit();
        HTTPClientPolicy httpClientPolicy = conduit.getClient();
        httpClientPolicy.setBrowserType(USER_AGENT);
        httpClientPolicy.setConnectionTimeout(configuration.getConnectionTimeout().toMillis());
        httpClientPolicy.setReceiveTimeout(configuration.getReceiveTimeout().toMillis());

        return LoggingInvocationHandler.createLoggingProxy("SslMateSearch", SslMateMonitor.class, sslMateMonitor);
    }
}
