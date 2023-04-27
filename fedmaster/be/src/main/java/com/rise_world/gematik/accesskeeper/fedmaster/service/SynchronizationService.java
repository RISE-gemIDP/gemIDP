/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service;

import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.IdpJwsJwtCompactConsumer;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDomainDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantKeyDto;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.DomainRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.ParticipantRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.PublicKeyRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.ScopeRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.util.PemUtils;
import com.rise_world.gematik.accesskeeper.fedmaster.util.SynchronizationLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.WebApplicationException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.rise_world.gematik.accesskeeper.common.token.ClaimUtils.getLongPropertyWithoutException;
import static com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantType.OP;
import static com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantType.RP;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.KID_UNKNOWN;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.MAX_DOWNTIME_REACHED;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.NOT_REACHABLE;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.OK;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.SCOPES_INVALID;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.SIGNATURE_INVALID;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.TOKEN_INVALID;
import static com.rise_world.gematik.accesskeeper.fedmaster.util.JwtUtils.getListStringProperty;
import static com.rise_world.gematik.accesskeeper.fedmaster.util.JwtUtils.getStringProperty;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.singletonMap;
import static java.util.Map.of;

@Service
@EnableTransactionManagement
public class SynchronizationService {

    private static final int MAX_URI_LENGTH = 2000;
    private static final int MAX_ORG_LENGTH = 128;
    private static final String[] OP_ENDPOINTS = { "authorization_endpoint", "token_endpoint", "pushed_authorization_request_endpoint"};
    private static final String METADATA = "metadata";

    private final SynchronizationConfiguration configuration;
    private final ParticipantRepository participantRepository;
    private final PublicKeyRepository keyRepository;
    private final DomainRepository domainRepository;
    private final ScopeRepository scopeRepository;
    private final FederationEndpointProvider endpointProvider;
    private final long iatLeeway;
    private final Clock clock;

    public SynchronizationService(Clock clock,
                                  SynchronizationConfiguration configuration,
                                  ParticipantRepository participantRepository,
                                  PublicKeyRepository keyRepository,
                                  DomainRepository domainRepository,
                                  ScopeRepository scopeRepository,
                                  FederationEndpointProvider endpointProvider,
                                  @Value("${token.iat.leeway}") long iatLeeway) {
        this.configuration = configuration;
        this.participantRepository = participantRepository;
        this.keyRepository = keyRepository;
        this.domainRepository = domainRepository;
        this.scopeRepository = scopeRepository;
        this.endpointProvider = endpointProvider;
        this.iatLeeway = iatLeeway;
        this.clock = clock;
    }

    @Transactional
    public void synchronizeParticipant(ParticipantDto participant, Instant synchronizationTime) throws SynchronizationException {
        SynchronizationLog.log(OK, "synchronizing participant {}", participant.getSub());
        JwtClaims entityStatement = fetchEntityStatement(participant);

        validateEntityStatement(participant, entityStatement);

        if (participant.getType() == OP) {
            // update orgName and logo_uri
            Optional<String> orgName = getStringProperty(entityStatement, METADATA, OP.getType(), "organization_name");

            if (orgName.isPresent() && isValidOrganizationName(orgName.get())) {
                participant.setOrganizationName(orgName.get());
            }
            else {
                throw new SynchronizationException(TOKEN_INVALID, "organization name not valid");
            }

            Optional<String> logoUri = getStringProperty(entityStatement, METADATA, OP.getType(), "logo_uri");
            if (logoUri.isPresent() && isValidUri(logoUri.get())) {
                participant.setLogoUri(logoUri.get());
            }
            else {
                throw new SynchronizationException(TOKEN_INVALID, "logo uri not valid");
            }

            // update domain list for CTR
            Set<String> expectedDomainNames = getDomains(entityStatement);
            List<ParticipantDomainDto> domains = domainRepository.findByParticipant(participant.getId());

            removeUnusedDomains(expectedDomainNames, domains);
            addNewDomains(participant, expectedDomainNames, domains);
        }
        else if (participant.getType() == RP) {
            // check scopes
            Set<String> registeredScopes = new HashSet<>(scopeRepository.findByParticipant(participant.getId()));
            Set<String> scopes = getListStringProperty(entityStatement, METADATA, RP.getType(), "scope").map(HashSet::new).orElseGet(HashSet::new);

            if (!Objects.equals(registeredScopes, scopes)) {
                String regScope = join(" ", registeredScopes);
                String fetchedScopes = join(" ", scopes);
                throw new SynchronizationException(SCOPES_INVALID,
                    of("registered_scopes", regScope, "fetched_scopes", fetchedScopes),
                    format("registered scopes (%s) do not match fetched scopes (%s)", regScope, fetchedScopes));
            }
        }

        participantRepository.synchronizeParticipant(participant, Timestamp.from(synchronizationTime));
        SynchronizationLog.log(OK, "participant {} synchronized", participant.getSub());
    }

    private void validateEntityStatement(ParticipantDto participant, JwtClaims entityStatement) throws SynchronizationException {
        Long iat = getLongPropertyWithoutException(entityStatement, JwtConstants.CLAIM_ISSUED_AT);
        if (iat == null) {
            throw new SynchronizationException(TOKEN_INVALID, "iat is missing");
        }

        Instant issuedAtInstantWithLeeway = Instant.ofEpochSecond(iat).minus(this.iatLeeway, ChronoUnit.MILLIS);

        Instant now = Instant.now(clock);
        if (issuedAtInstantWithLeeway.isAfter(now)) {
            throw new SynchronizationException(TOKEN_INVALID, "iat is invalid");
        }

        Long expiryTime = getLongPropertyWithoutException(entityStatement, JwtConstants.CLAIM_EXPIRY);
        if (expiryTime == null) {
            throw new SynchronizationException(TOKEN_INVALID, "exp is missing");
        }

        Instant expires = Instant.ofEpochMilli(expiryTime * 1000L);
        if (expires.isBefore(now)) {
            throw new SynchronizationException(TOKEN_INVALID, "token expired");
        }

        if (!participant.getSub().equals(entityStatement.getSubject())) {
            throw new SynchronizationException(TOKEN_INVALID, "subject of token does not match participant");
        }
        if (!Objects.equals(entityStatement.getIssuer(), entityStatement.getSubject())) {
            throw new SynchronizationException(TOKEN_INVALID, "token not issued by participant");
        }
    }

    private boolean isValidUri(String uri) {
        if (uri.length() > MAX_URI_LENGTH) {
            return false;
        }
        try {
            URI toBeTested = new URI(uri);

            if (!toBeTested.isAbsolute() ||
                toBeTested.getFragment() != null ||
                !("http".equalsIgnoreCase(toBeTested.getScheme()) || "https".equalsIgnoreCase(toBeTested.getScheme()))) {
                return false;
            }
        }
        catch (URISyntaxException e) {
            return false;
        }

        return true;
    }

    private boolean isValidOrganizationName(String input) {
        if (input.length() > MAX_ORG_LENGTH) {
            return false;
        }
        return input.matches("[\\x20-\\x7E]+");
    }

    @Transactional
    public void logSynchronizationRun(ParticipantDto participant, Instant synchronizationTime) {
        participantRepository.setLastRun(participant.getId(), Timestamp.from(synchronizationTime));
    }

    private void addNewDomains(ParticipantDto participant, Set<String> expectedDomainNames, List<ParticipantDomainDto> domains) {
        for (String dns : expectedDomainNames) {
            if (domains.stream().noneMatch(participantDomain -> StringUtils.equalsIgnoreCase(dns, participantDomain.getName()))) {
                ParticipantDomainDto domain = new ParticipantDomainDto();
                domain.setParticipantId(participant.getId());
                domain.setName(dns);

                domainRepository.save(domain);
            }
        }
    }

    private void removeUnusedDomains(Set<String> expectedDomainNames, List<ParticipantDomainDto> domains) {
        domains.stream()
            .filter(participantDomain -> !expectedDomainNames.contains(participantDomain.getName()))
            .map(ParticipantDomainDto::getId)
            .forEach(domainRepository::delete);
    }

    private Set<String> getDomains(JwtClaims entityStatement) throws SynchronizationException {
        Set<String> domains = new HashSet<>();

        domains.add(extractDomain(entityStatement.getSubject()));
        for (String endpoint : OP_ENDPOINTS) {
            String endpointUri = getStringProperty(entityStatement, METADATA, OP.getType(), endpoint)
                .orElseThrow(() -> new SynchronizationException(TOKEN_INVALID, endpoint + " not found"));
            domains.add(extractDomain(endpointUri));
        }
        return domains;
    }

    private String extractDomain(String uriString) throws SynchronizationException {
        try {
            if (uriString.length() > MAX_URI_LENGTH) {
                throw new SynchronizationException(TOKEN_INVALID, uriString + " exceeds allowed uri length");
            }
            URI endpointUri = new URI(uriString);
            if ("https".equalsIgnoreCase(endpointUri.getScheme())) {
                return endpointUri.getHost();
            }
            else {
                throw new SynchronizationException(TOKEN_INVALID, uriString + " expected to use a https scheme");
            }
        }
        catch (URISyntaxException e) {
            throw new SynchronizationException(TOKEN_INVALID, uriString + " expected to be a valid URI, but was not", e);
        }
    }

    private JwtClaims fetchEntityStatement(ParticipantDto participant) throws SynchronizationException {
        String token;
        try {
            token = endpointProvider.create(participant.getSub()).getFederationEntity();
        }
        catch (WebApplicationException ex) {
            Instant notReachable = getNotReachableSince(participant);
            if (notReachable.plus(configuration.getMaxDowntime()).isBefore(Instant.now(clock))) {
                throw new SynchronizationException(MAX_DOWNTIME_REACHED, "participant not reachable since " + notReachable + " and allowed downtime exceeded", ex);
            }
            else {
                throw new SynchronizationException(NOT_REACHABLE, "participant not reachable since " + notReachable, ex);
            }

        }

        JwsJwtCompactConsumer consumer;
        try {
            consumer = new IdpJwsJwtCompactConsumer(token.trim());
            consumer.getJwtClaims();  // trigger token parsing
        }
        catch (Exception e) {
            throw new SynchronizationException(TOKEN_INVALID, "token does not conform to JWS compact serialization format", e);
        }

        ParticipantKeyDto participantKey = keyRepository.findKeyByParticipantAndKeyId(participant.getId(), consumer.getJwsHeaders().getKeyId())
            .orElseThrow(() -> new SynchronizationException(KID_UNKNOWN,
                singletonMap("kid_jwt_es", consumer.getJwsHeaders().getKeyId()),
                consumer.getJwsHeaders().getKeyId() + " kid not found for participant"));

        try {
            Optional<PublicKey> publicKey = PemUtils.readPublicKey(participantKey.getPem());
            if (publicKey.isEmpty() || !consumer.verifySignatureWith(publicKey.get(), SignatureAlgorithm.ES256)) {
                throw new SynchronizationException(SIGNATURE_INVALID,
                    singletonMap("kid_jwt_es", consumer.getJwsHeaders().getKeyId()),
                    "signature could not be verified for entity statement of participant");
            }
        }
        catch (Exception e) {
            throw new SynchronizationException(SIGNATURE_INVALID,
                singletonMap("kid_jwt_es", consumer.getJwsHeaders().getKeyId()),
                "signature could not be verified for entity statement of participant", e);
        }

        return consumer.getJwtClaims();
    }

    private Instant getNotReachableSince(ParticipantDto participant) {
        return participant.getSynchronizedAt().toInstant()
            .plus(configuration.getExpiration());
    }

}
