/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service;

import com.rise_world.gematik.accesskeeper.common.crypt.KeyProvider;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ConfigurationException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorMessage;
import com.rise_world.gematik.accesskeeper.fedmaster.FederationMasterConfiguration;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantKeyDto;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.ParticipantRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.repository.PublicKeyRepository;
import com.rise_world.gematik.accesskeeper.fedmaster.token.EntityStatementCreationStrategy;
import com.rise_world.gematik.accesskeeper.fedmaster.util.JwtUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jwk.PublicKeyUse;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.rise_world.gematik.accesskeeper.common.crypt.KeyConstants.PUK_FEDMASTER_SIG;
import static com.rise_world.gematik.accesskeeper.fedmaster.exception.FederationMasterErrorCodes.FED_INVALID_AUD;
import static com.rise_world.gematik.accesskeeper.fedmaster.exception.FederationMasterErrorCodes.FED_INVALID_ISS;
import static com.rise_world.gematik.accesskeeper.fedmaster.exception.FederationMasterErrorCodes.FED_INVALID_SUB;
import static com.rise_world.gematik.accesskeeper.fedmaster.exception.FederationMasterErrorCodes.FED_UNKNOWN_ISS;
import static com.rise_world.gematik.accesskeeper.fedmaster.exception.FederationMasterErrorCodes.FED_UNKNOWN_SUB;
import static com.rise_world.gematik.accesskeeper.fedmaster.token.EntityStatementCreationStrategy.TYPE_IDP_LIST_JWT;

@Service
public class EntityStatementServiceImpl implements EntityStatementService {

    private static final Logger LOG = LoggerFactory.getLogger(EntityStatementServiceImpl.class);
    private static final int MAX_LENGTH_URI = 2000;

    private final FederationMasterConfiguration config;
    private final Clock clock;
    private final EntityStatementCreationStrategy tokenStrategy;
    private final KeyProvider keyProvider;
    private final ParticipantRepository participantRepository;
    private final PublicKeyRepository keyRepository;

    public EntityStatementServiceImpl(FederationMasterConfiguration config,
                                      Clock clock,
                                      EntityStatementCreationStrategy tokenStrategy,
                                      KeyProvider keyProvider,
                                      ParticipantRepository participantRepository,
                                      PublicKeyRepository keyRepository) {
        this.config = config;
        this.clock = clock;
        this.tokenStrategy = tokenStrategy;
        this.keyProvider = keyProvider;
        this.participantRepository = participantRepository;
        this.keyRepository = keyRepository;
    }

    @Override
    public String fetchMasterEntityStatement() {
        Instant now = clock.instant();
        long epochSecond = now.getEpochSecond();
        String issuer = getFederationMasterIssuer();

        // create claims
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(issuer);
        claims.setSubject(issuer);
        claims.setIssuedAt(epochSecond);
        claims.setExpiryTime(now.plus(config.getTokenTimeout()).getEpochSecond());

        claims.setClaim("jwks", new JsonWebKeys(createJsonWebKeyForSignature()));

        JwtClaims federationEntity = new JwtClaims();
        federationEntity.setClaim("federation_fetch_endpoint", issuer + "/federation/fetch");
        federationEntity.setClaim("federation_list_endpoint", issuer + "/federation/list");
        federationEntity.setClaim("idp_list_endpoint", issuer + "/federation/listidps");
        claims.setClaim("metadata", new JwtClaims().setClaim("federation_entity", federationEntity));

        return tokenStrategy.toToken(claims);
    }

    @Override
    public String fetchEntityStatement(String iss, String sub, String aud) {
        String federationMasterIssuer = getFederationMasterIssuer();

        String searchSub = StringUtils.defaultIfEmpty(sub, federationMasterIssuer);

        // guards start
        if (iss == null) {
            LOG.warn("no issuer provided");
            throw new AccessKeeperException(FED_INVALID_ISS);
        }
        validateUri(iss, FED_INVALID_ISS);
        validateUri(searchSub, FED_INVALID_SUB);
        if (!federationMasterIssuer.equals(iss)) {
            LOG.warn("unexpected federationMasterIssuer");
            throw new AccessKeeperException(FED_UNKNOWN_ISS);
        }
        if (aud != null) {
            validateUri(aud, FED_INVALID_AUD);
            if (participantRepository.findByIdentifier(aud).isEmpty()) {
                LOG.error("audience '{}' not a registered entity", aud);
                throw new AccessKeeperException(FED_INVALID_AUD);
            }
        }
        // guards end

        if (Objects.equals(iss, searchSub)) {
            return fetchMasterEntityStatement();
        }

        ParticipantDto entity = this.participantRepository.findByIdentifier(searchSub).orElseThrow(() -> new AccessKeeperException(FED_UNKNOWN_SUB));
        List<ParticipantKeyDto> keys = keyRepository.findByParticipant(entity.getId());

        if (!keys.isEmpty()) {
            return toEntityStatement(entity, aud, keys);
        }

        throw new AccessKeeperException(FED_UNKNOWN_SUB);
    }

    private static void validateUri(String expectedUri, ErrorMessage errorMessage) {
        if (expectedUri.length() > MAX_LENGTH_URI) {
            LOG.warn("ErrorMessage: {} - Reason: too long", errorMessage.getText());
            throw new AccessKeeperException(errorMessage);
        }
        try {
            new URI(expectedUri);
        }
        catch (URISyntaxException e) {
            LOG.warn("ErrorMessage: {} - Reason: not a valid URI", errorMessage.getText(), e);
            throw new AccessKeeperException(errorMessage);
        }
    }

    @Override
    public List<String> getSubEntityIds() {
        return this.participantRepository.findAllIdentifiers();
    }

    @Override
    public String getIdpList() {
        Instant now = clock.instant();
        long epochSecond = now.getEpochSecond();

        // create claims
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(getFederationMasterIssuer());
        claims.setIssuedAt(epochSecond);
        claims.setExpiryTime(now.plus(config.getTokenTimeout()).getEpochSecond());

        List<JwtClaims> sekIdpList = new ArrayList<>();
        for (ParticipantDto idp : this.participantRepository.findAllOpenIdProviders()) {
            JwtClaims idpClaims = new JwtClaims();
            idpClaims.setIssuer(idp.getSub());
            idpClaims.setClaim("organization_name", idp.getOrganizationName());
            idpClaims.setClaim("logo_uri", idp.getLogoUri());
            idpClaims.setClaim("user_type_supported", idp.getUserTypeSupported());
            idpClaims.setClaim("pkv", idp.isPkv());
            sekIdpList.add(idpClaims);
        }
        claims.setClaim("idp_entity", sekIdpList);

        return tokenStrategy.toToken(claims, TYPE_IDP_LIST_JWT);
    }

    private String getFederationMasterIssuer() {
        String issuer = config.getIssuer();

        // guard: empty issuer
        if (StringUtils.isEmpty(issuer)) {
            throw new ConfigurationException("issuer of federation master is not configured properly");
        }
        return issuer;
    }

    private JsonWebKey createJsonWebKeyForSignature() {
        JsonWebKey jwk = JwkUtils.fromECPublicKey(keyProvider.getKey(PUK_FEDMASTER_SIG), JsonWebKey.EC_CURVE_P256);

        jwk.setKeyId(PUK_FEDMASTER_SIG);
        jwk.setPublicKeyUse(PublicKeyUse.SIGN);
        jwk.setAlgorithm(AlgorithmUtils.ES_SHA_256_ALGO);

        return jwk;
    }

    private String toEntityStatement(ParticipantDto entity, String expectedAud, List<ParticipantKeyDto> keys) {
        Instant now = clock.instant();
        long epochSecond = now.getEpochSecond();

        // create claims
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(config.getIssuer());
        claims.setSubject(entity.getSub());
        claims.setIssuedAt(epochSecond);
        claims.setExpiryTime(now.plus(config.getTokenTimeout()).getEpochSecond());

        if (expectedAud != null) {
            claims.setAudience(expectedAud);
        }

        List<JsonWebKey> keyList = new ArrayList<>();
        for (ParticipantKeyDto key : keys) {
            keyList.add(JwtUtils.toJsonWebKey(key));
        }
        claims.setClaim("jwks", new JsonWebKeys(keyList));
        claims.setClaim("metadata", entity.getType().createDefaultMetadata());

        return tokenStrategy.toToken(claims);
    }
}
