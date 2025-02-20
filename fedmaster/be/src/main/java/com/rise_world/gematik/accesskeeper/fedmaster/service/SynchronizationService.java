/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service;

import com.rise_world.gematik.accesskeeper.common.service.FederationEndpointProvider;
import com.rise_world.gematik.accesskeeper.common.service.SynchronizationConfiguration;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.IdpJwsJwtCompactConsumer;
import com.rise_world.gematik.accesskeeper.fedmaster.FederationMasterConfiguration;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDomainDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantKeyDto;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.DomainRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.ParticipantRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.PublicKeyRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.service.validation.RelyingPartyClaimValidationResult;
import com.rise_world.gematik.accesskeeper.fedmaster.service.validation.RelyingPartyValidationResult;
import com.rise_world.gematik.accesskeeper.fedmaster.service.validation.RelyingPartyValidator;
import com.rise_world.gematik.accesskeeper.fedmaster.util.HttpsEnforcer;
import com.rise_world.gematik.accesskeeper.fedmaster.util.PemUtils;
import com.rise_world.gematik.accesskeeper.fedmaster.util.SynchronizationLog;
import jakarta.ws.rs.WebApplicationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.rise_world.gematik.accesskeeper.common.token.ClaimUtils.getLongPropertyWithoutException;
import static com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantType.OP;
import static com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantType.RP;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.INVALID_SUB;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.KID_UNKNOWN;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.MAX_DOWNTIME_REACHED;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.NOT_REACHABLE;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.OK;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.REGISTRATION_DATA_INVALID;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.SIGNATURE_INVALID;
import static com.rise_world.gematik.accesskeeper.fedmaster.service.StatusCode.TOKEN_INVALID;
import static com.rise_world.gematik.accesskeeper.fedmaster.util.JwtUtils.getStringProperty;
import static com.rise_world.gematik.accesskeeper.fedmaster.util.MarkerUtils.appendParticipant;
import static java.util.Collections.singletonMap;
import static java.util.Map.entry;
import static java.util.Objects.isNull;
import static net.logstash.logback.marker.Markers.appendEntries;

@Service
@EnableTransactionManagement
public class SynchronizationService {

    private static final int MAX_URI_LENGTH = 2000;
    private static final int MAX_ORG_LENGTH = 128;
    private static final String[] OP_ENDPOINTS = {"authorization_endpoint", "token_endpoint", "pushed_authorization_request_endpoint"};
    private static final String METADATA = "metadata";

    private final SynchronizationConfiguration configuration;
    private final ParticipantRepository participantRepository;
    private final PublicKeyRepository keyRepository;
    private final DomainRepository domainRepository;
    private final RelyingPartyValidator relyingPartyValidator;
    private final FederationEndpointProvider endpointProvider;
    private final long iatLeeway;
    private final Clock clock;

    // use as a _final_ field, instead of accessing via the config object,
    // so the jit can optimize and remove dead code
    private final boolean lockRelyingParty;

    public SynchronizationService(Clock clock,
                                  SynchronizationConfiguration configuration,
                                  ParticipantRepository participantRepository,
                                  PublicKeyRepository keyRepository,
                                  DomainRepository domainRepository,
                                  RelyingPartyValidator relyingPartyValidator,
                                  FederationEndpointProvider endpointProvider,
                                  @Value("${token.iat.leeway}") long iatLeeway) {
        this.configuration = configuration;
        this.participantRepository = participantRepository;
        this.keyRepository = keyRepository;
        this.domainRepository = domainRepository;
        this.relyingPartyValidator = relyingPartyValidator;
        this.endpointProvider = endpointProvider;
        this.iatLeeway = iatLeeway;
        this.clock = clock;
        this.lockRelyingParty = configuration.lockRelyingParty();
    }

    @Transactional(noRollbackFor = SynchronizationException.class)
    public void synchronizeParticipant(ParticipantDto participant, Instant synchronizationTime) throws SynchronizationException {
        SynchronizationLog.log(OK, "synchronizing participant {}", participant.getSub());

        validateSub(participant);

        var entityStatement = fetchEntityStatement(participant);
        validateEntityStatement(participant, entityStatement);

        switch (participant.getType()) {
            case OP -> synchronizeOpenIdProvider(participant, entityStatement);
            case RP -> synchronizeRelyingParty(participant, entityStatement);
            default -> throw new IllegalArgumentException("unsupported participant type " + participant.getType());
        }

        participantRepository.synchronizeParticipant(participant, Timestamp.from(synchronizationTime));
        SynchronizationLog.log(OK, "participant {} synchronized", participant.getSub());
    }

    private void synchronizeOpenIdProvider(ParticipantDto participant, JwtClaims entityStatement) throws SynchronizationException {
        // updating domains for CTR check to detect potential certificate issues is more important
        // than orgName and logoUri updates
        updateDomains(participant, entityStatement);

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
    }

    private void updateDomains(ParticipantDto participant, JwtClaims entityStatement) throws SynchronizationException {
        Set<String> expectedDomainNames = getDomains(entityStatement);
        List<ParticipantDomainDto> domains = domainRepository.findByParticipant(participant.getId());

        removeUnusedDomains(expectedDomainNames, domains);
        addNewDomains(participant, expectedDomainNames, domains);
    }

    private void synchronizeRelyingParty(ParticipantDto participant, JwtClaims entityStatement) throws SynchronizationException {
        var result = relyingPartyValidator.validate(participant, entityStatement);
        var lockTimestamp = handleParticipantLock(participant, result);

        result.warnings()
            .ifPresent(warnings -> SynchronizationLog.log(OK,
                appendParticipant(participant)
                    .and(appendEntries(warnings.getRelated())),
                warnings.getMessage()));

        var errors = result.errors();
        if (errors.isPresent()) {
            var errorDetails = errors.get();
            var related = new HashMap<>(errorDetails.getRelated());
            lockTimestamp.ifPresent(lock -> related.put("participant_lock_timestamp", lock.toString()));
            throw new SynchronizationException(REGISTRATION_DATA_INVALID, related, errorDetails.getMessage());
        }
    }

    private Optional<Instant> handleParticipantLock(ParticipantDto participant, RelyingPartyValidationResult result) {
        if (!lockRelyingParty || result.lockParticipant() == participant.isLocked()) {
            return Optional.empty();
        }

        // negate -> lockParticipant: true means active: false
        participantRepository.setActive(participant.getId(), !result.lockParticipant());

        var timestamp = clock.instant();
        SynchronizationLog.log(OK, appendParticipant(participant), "set active {} for participant {} at {}", !result.lockParticipant(), participant.getSub(), timestamp);
        return Optional.of(timestamp);
    }

    private static void validateSub(ParticipantDto participant) throws SynchronizationException {
        if (!HttpsEnforcer.isHttps(participant.getSub())) {
            throw new SynchronizationException(INVALID_SUB, "unsupported sub uri '%s'".formatted(participant.getSub()));
        }
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
        return input.matches("^[ÄÖÜäöüß\\w \\-.&+*/]{1,128}$");
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
            token = endpointProvider.create(participant.getSub(), FederationMasterConfiguration.USER_AGENT).getFederationEntity();
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

        var validationError = validateSignature(participant, consumer);
        if (validationError.isPresent()) {
            handleKeyValidationError(participant, validationError.get());
        }

        return consumer.getJwtClaims();
    }

    private void handleKeyValidationError(ParticipantDto participant, KeyValidationError validation) throws SynchronizationException {
        var related = new HashMap<>(validation.related());
        if (participant.getType() == RP) {
            var relyingPartyValidation = new RelyingPartyValidationResult();
            relyingPartyValidation.add(RelyingPartyClaimValidationResult.lockingError(validation.message(), validation.related()));
            handleParticipantLock(participant, relyingPartyValidation)
                .ifPresent(lockTimestamp -> related.put("participant_lock_timestamp", lockTimestamp.toString()));
        }

        throw new SynchronizationException(validation.status(), related, validation.message());
    }

    private Optional<KeyValidationError> validateSignature(ParticipantDto participant, JwsJwtCompactConsumer consumer) {
        var pem = keyRepository.findKeyByParticipantAndKeyId(participant.getId(), consumer.getJwsHeaders().getKeyId())
            .map(ParticipantKeyDto::getPem)
            .orElse(null);
        if (isNull(pem)) {
            return Optional.of(new KeyValidationError(KID_UNKNOWN,
                singletonMap("kid_jwt_es", consumer.getJwsHeaders().getKeyId()),
                consumer.getJwsHeaders().getKeyId() + " kid not found for participant"));
        }

        var signatureAlgorithm = consumer.getJwsHeaders().getSignatureAlgorithm();
        if (!SupportedSignatureAlgorithms.isSupported(signatureAlgorithm)) {
            return Optional.of(new KeyValidationError(StatusCode.UNSUPPORTED_SIGNATURE_ALGORITHM,
                Map.ofEntries(
                    entry("kid_jwt_es", consumer.getJwsHeaders().getKeyId()),
                    entry("alg", String.valueOf(signatureAlgorithm))),
                "unsupported signature algorithm"));
        }

        var validSignature = PemUtils.readPublicKey(pem)
            .filter(key -> consumer.verifySignatureWith(key, signatureAlgorithm))
            .isPresent();

        if (!validSignature) {
            return Optional.of(new KeyValidationError(SIGNATURE_INVALID,
                singletonMap("kid_jwt_es", consumer.getJwsHeaders().getKeyId()),
                "signature could not be verified for entity statement of participant"));
        }

        return Optional.empty();
    }

    private Instant getNotReachableSince(ParticipantDto participant) {
        return participant.getSynchronizedAt().toInstant()
            .plus(configuration.getExpiration());
    }

    private record KeyValidationError(StatusCode status, Map<String, String> related, String message) {
    }
}
