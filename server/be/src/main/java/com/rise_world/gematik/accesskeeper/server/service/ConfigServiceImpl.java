/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rise_world.gematik.accesskeeper.common.OAuth2Constants;
import com.rise_world.gematik.accesskeeper.common.dto.TokenType;
import com.rise_world.gematik.accesskeeper.common.token.ClaimUtils;
import com.rise_world.gematik.accesskeeper.server.dto.RequestSource;
import com.rise_world.gematik.accesskeeper.server.exception.ConfigException;
import com.rise_world.gematik.accesskeeper.server.model.Client;
import com.rise_world.gematik.accesskeeper.server.model.Fachdienst;
import com.rise_world.gematik.accesskeeper.server.model.InfoModel;
import com.rise_world.gematik.accesskeeper.server.model.Scope;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"fb-contrib:FCBL_FIELD_COULD_BE_LOCAL", "fb-contrib:DMC_DUBIOUS_MAP_COLLECTION"})
public class ConfigServiceImpl implements ConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigServiceImpl.class);
    private static final Pattern VS_CHAR = Pattern.compile("[\\x20-\\x7E]+");
    private static final Pattern NQ_CHAR = Pattern.compile("[\\x21\\x23-\\x5B\\x5D-\\x7E]+");

    private final String configLocation;

    // init() creates and populates the cache. afterwards there are only reads - no writes
    // therefore it's safe to mark a non-primitive field as volatile
    @SuppressWarnings({"squid:S3077", "java:S3749"})
    private volatile ConfigCache cache;

    public ConfigServiceImpl(@Value("${model.configuration}") String configLocation) {
        this.configLocation = configLocation;
    }

    /**
     * Reads the configured infomodel file and caches its content
     */
    @PostConstruct
    @Override
    public synchronized void reload()  {
        Path configFile = Paths.get(configLocation).toAbsolutePath();
        LOG.info("Loading configuration from {}", configFile);
        InputStream is;
        try {
            is = Files.newInputStream(configFile);
        }
        catch (IOException e) {
            throw new ConfigException(String.format("Configuration file '%s' not found", configFile), e);
        }

        ObjectMapper objectMapper = new ObjectMapper();

        InfoModel infoModel;
        try {
            infoModel = objectMapper.readValue(is, InfoModel.class);
        }
        catch (IOException e) {
            throw new ConfigException("Failed to read configuration file", e);
        }

        init(infoModel);
    }

    /**
     * Processes and validates the loaded infomodel
     *
     * @param infoModel the loaded infomodel
     */
    protected void init(InfoModel infoModel) {
        validateMandatoryFields(infoModel);
        ConfigCache newCache = new ConfigCache(infoModel);

        addFachdienste(newCache, infoModel);
        addScopes(newCache, infoModel);
        addClients(newCache, infoModel);

        this.cache = newCache;
    }

    private void validateMandatoryFields(InfoModel infoModel) {
        if (StringUtils.isEmpty(infoModel.getIssuerTi())) {
            LOG.error("infomodel: issuer_ti is required");
            throw new ConfigException("issuer_ti is not configured");
        }
        if (StringUtils.isEmpty(infoModel.getIssuerInet())) {
            LOG.error("infomodel: issuer_internet is required");
            throw new ConfigException("issuer_internet is not configured");
        }
        if (StringUtils.isEmpty(infoModel.getAuthServerClientName())) {
            LOG.error("infomodel: auth_server_client_name is required");
            throw new ConfigException("auth_server_client_name is not configured");
        }
        if (StringUtils.isEmpty(infoModel.getAuthServerOrganizationName())) {
            LOG.error("infomodel: auth_server_organization_name is not configured");
            throw new ConfigException("auth_server_organization_name is not configured");
        }
        if (StringUtils.isEmpty(infoModel.getPairingEndpoint())) {
            LOG.error("infomodel: pairing endpoint is required");
            throw new ConfigException("Pairing endpoint is not configured");
        }
        if (StringUtils.isEmpty(infoModel.getSalt())) {
            LOG.error("infomodel: subject salt is required");
            throw new ConfigException("Salt is not configured");
        }
    }

    private void addFachdienste(ConfigCache newCache, InfoModel infoModel) {
        final Map<String, Fachdienst> fachdienstMap = newCache.fachdienstMap;
        final Set<String> processedIds = new HashSet<>();

        for (Fachdienst f : infoModel.getFachdienste()) {

            if (isValidFachdienst(processedIds, f)) {
                fachdienstMap.put(f.getId(), f);
            }
            else {
                if (f.getId() != null) {
                    fachdienstMap.remove(f.getId());
                    newCache.invalidFachdienstIds.add(f.getId());
                }
                newCache.effectiveInfoModel.getFachdienste().removeIf(f::equals);
            }

            processedIds.add(f.getId());
        }
    }

    private void addScopes(ConfigCache newCache, InfoModel infoModel) {
        final Map<String, Scope> scopeMap = newCache.scopeMap;
        final Set<String> processedIds = new HashSet<>();

        for (Scope s : infoModel.getScopes()) {
            if (isValidScope(newCache.fachdienstMap.keySet(), processedIds, s)) {
                scopeMap.put(s.getId(), s);
            }
            else {
                if (s.getId() != null) {
                    scopeMap.remove(s.getId());
                    newCache.invalidScopeIds.add(s.getId());
                }
                newCache.effectiveInfoModel.getScopes().removeIf(s::equals);
            }

            processedIds.add(s.getId());
        }

        for (Scope scope : scopeMap.values()) {
            if (scope.getFachdienst() != null) {
                newCache.scopeFachdienstMap.put(scope.getId(), newCache.fachdienstMap.get(scope.getFachdienst()));
            }
        }
    }

    private void addClients(ConfigCache newCache, InfoModel infoModel) {
        final Map<String, Client> clientMap = newCache.clientMap;
        final Set<String> processedIds = new HashSet<>();

        for (Client c : infoModel.getPublicClients()) {

            if (isValidClient(processedIds, c)) {
                clientMap.put(c.getId(), c);
            }
            else {
                if (c.getId() != null) {
                    clientMap.remove(c.getId());
                    newCache.invalidClientIds.add(c.getId());
                }
                newCache.effectiveInfoModel.getPublicClients().removeIf(c::equals);
            }

            processedIds.add(c.getId());
        }
    }

    private boolean isValidFachdienst(Set<String> processedIds, Fachdienst f) {
        if (!isValidVsChar("fachdienst_id", 32, f.getId())) {
            return false;
        }

        if (!isValidVsChar("aud", 0, f.getAud())) {
            return false;
        }

        if (!isValidVsChar("sectorIdentifier", 256, f.getSectorIdentifier())) {
            return false;
        }

        if (processedIds.contains(f.getId())) {
            LOG.error("infomodel: duplicate fachdienst_id: {}", f.getId());
            return false;
        }

        return true;
    }

    private boolean isValidScope(Set<String> fachdienstIds, Set<String> processedIds, Scope s) {
        if (!isValidNqChar("scope_id", 0, s.getId())) {
            return false;
        }

        if (StringUtils.isEmpty(s.getDescription())) {
            LOG.error("infomodel: scope {} description must not be empty", s.getId());
            return false;
        }

        if (processedIds.contains(s.getId())) {
            LOG.error("infomodel: duplicate scope_id: {}", s.getId());
            return false;
        }

        if (OAuth2Constants.SCOPE_OPENID.equals(s.getId())) {
            if (!CollectionUtils.isEmpty(s.getClaims())) {
                LOG.error("infomodel: scope openid must not contain claims");
                return false;
            }
            if (StringUtils.isNotEmpty(s.getFachdienst())) {
                LOG.error("infomodel: scope openid must not have a fachdienst set");
                return false;
            }
        }
        else {
            if (!fachdienstIds.contains(s.getFachdienst())) {
                LOG.error("infomodel: scope {} has an invalid fachdienst: {}", s.getId(), s.getFachdienst());
                return false;
            }
           return isValidClaims(s);
        }

        return true;
    }

    private boolean isValidClaims(Scope s) {
        if (CollectionUtils.isEmpty(s.getClaims()) || !s.getClaims().containsKey(ClaimUtils.ID_NUMBER)) {
            LOG.error("infomodel: scope {} mandatory claim idNumber is missing", s.getId());
            return false;
        }

        for (Map.Entry<String, String> claimEntry : s.getClaims().entrySet()) {
            if (!ClaimUtils.CARD_CLAIMS.contains(claimEntry.getKey())) {
                LOG.error("infomodel: scope {} unexpected claim '{}'", s.getId(), claimEntry.getKey());
                return false;
            }
            if (StringUtils.isEmpty(claimEntry.getValue())) {
                LOG.error("infomodel: scope {} claim {} description must not be empty", s.getId(), claimEntry.getKey());
                return false;
            }
        }
        return true;
    }

    // @AFO: A_20434 - die Liste der redirect URIs darf nicht leer sein
    // @AFO: A_20434 - Validierung der redirect URI laut RFC
    private boolean isValidClient(Set<String> processedIds, Client c) {
        if (!isValidVsChar("client_id", 32, c.getId())) {
            return false;
        }

        if (processedIds.contains(c.getId())) {
            LOG.error("infomodel: duplicate client_id: {}", c.getId());
            return false;
        }

        if (CollectionUtils.isEmpty(c.getValidRedirectUris())) {
            LOG.error("infomodel: client {} has no redirect uris", c.getId());
            return false;
        }

        for (String redirectUri : c.getValidRedirectUris()) {
            if (!isValidRedirectUri(redirectUri)) {
                LOG.error("infomodel: client {} has an invalid redirect uri: {}", c.getId(), redirectUri);
                return false;
            }
        }

        return true;
    }

    private boolean isValidRedirectUri(String redirectUri) {
        if (StringUtils.isEmpty(redirectUri)) {
            return false;
        }

        try {
            URI u = new URI(redirectUri);
            if (StringUtils.isEmpty(u.getScheme()) || u.getHost() == null || u.getFragment() != null) {
                throw new IllegalArgumentException();
            }
            if ("http".equalsIgnoreCase(u.getScheme()) || "https".equalsIgnoreCase(u.getScheme())) {
                validatePort(u);
            }
            return true;
        }
        catch (IllegalArgumentException | URISyntaxException | MalformedURLException e) {
            return false;
        }
    }

    private void validatePort(URI u) throws MalformedURLException {
        u.toURL(); // perform default URL validations
        if (u.getPort() == 0 || u.getPort() > 65535) {
            throw new IllegalArgumentException("invalid port");
        }
    }

    private boolean isValidVsChar(String name, int maxLength, String value) {
        return isValidChar(name, maxLength, VS_CHAR, value);
    }

    private boolean isValidNqChar(String name, int maxLength, String value) {
        return isValidChar(name, maxLength, NQ_CHAR, value);
    }

    private boolean isValidChar(String name, int maxLength, Pattern pattern, String value) {
        if (StringUtils.isEmpty(value)) {
            LOG.error("infomodel: {} must not be empty", name);
            return false;
        }

        if (maxLength != 0 && value.length() > maxLength) {
            LOG.error("infomodel: {} {} is too long", name, value);
            return false;
        }

        if (!pattern.matcher(value).matches()) {
            LOG.error("infomodel: {} {} doesn't match regex", name, value);
            return false;
        }

        return true;
    }

    @Override
    public String getConfigLocation() {
        return new File(configLocation).getAbsolutePath();
    }

    @Override
    public InfoModel getEffectiveInfoModel() {
        return cache.effectiveInfoModel;
    }

    @Override
    public InfoModel getParsedInfoModel() {
        return cache.parsedInfoModel;
    }

    @Override
    public String getIssuer(RequestSource requestSource) {
        switch (requestSource) {
            case TI:
                return cache.issuerTi;
            case INTERNET:
                return cache.issuerInternet;
            default:
                throw new IllegalArgumentException("Invalid request source: " + requestSource);
        }
    }

    @Override
    public Set<String> getIssuers() {
        return cache.issuers;
    }

    @Override
    public String getAuthServerClientName() {
        return cache.authServerClientName;
    }

    @Override
    public String getAuthServerOrganizationName() {
        return cache.authServerOrganizationName;
    }

    @Override
    public String getPairingEndpoint() {
        return cache.pairingEndpoint;
    }

    @Override
    public String getSalt() {
        return cache.salt;
    }

    @Override
    public Long getTokenTimeout(TokenType tokenType) {
        switch (tokenType) {
            case CHALLENGE:
                return cache.challengeTimeout;
            case AUTH_CODE:
                return cache.authCodeTimeout;
            default:
                throw new IllegalArgumentException("Invalid token type: " + tokenType);
        }
    }

    @Override
    public Scope getScopeById(String scopeId) {
        return cache.scopeMap.get(scopeId);
    }

    @Override
    public Client getClientById(String clientId) {
        return cache.clientMap.get(clientId);
    }

    @Override
    public Set<String> getRedirectUrisForEntityStatement() {
        return cache.clientMap.values().stream()
            .filter(Client::isRedirectUrisInES)
            .map(Client::getValidRedirectUris)
            .flatMap(List::stream)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public Set<String> getInvalidClientIds() {
        return cache.invalidClientIds;
    }

    @Override
    public Set<String> getInvalidFachdienstIds() {
        return cache.invalidFachdienstIds;
    }

    @Override
    public Set<String> getInvalidScopeIds() {
        return cache.invalidScopeIds;
    }

    @Override
    public Set<String> getInvalidSektorAppIds() {
        return cache.invalidSektorAppIds;
    }


    @Override
    public Fachdienst getFachdienstByScope(String scopeId) {
        return cache.scopeFachdienstMap.get(scopeId);
    }

    @Override
    public List<String> getFachdienstScopes() {
        final ArrayList<String> scopes = new ArrayList<>(cache.scopeFachdienstMap.keySet());
        scopes.sort(String::compareTo);
        return scopes;
    }

    @SuppressWarnings("fb-contrib:DMC_DUBIOUS_MAP_COLLECTION")
    private static class ConfigCache {
        private final InfoModel parsedInfoModel;
        private final InfoModel effectiveInfoModel;
        private final String issuerTi;
        private final String issuerInternet;
        private final Set<String> issuers;
        private final String authServerClientName;
        private final String authServerOrganizationName;
        private final String pairingEndpoint;
        private final String salt;

        private final Long challengeTimeout;
        private final Long authCodeTimeout;

        private final Map<String, Client> clientMap = new HashMap<>();
        private final Map<String, Scope> scopeMap = new HashMap<>();
        private final Map<String, Fachdienst> fachdienstMap = new HashMap<>();
        private final Map<String, Fachdienst> scopeFachdienstMap = new HashMap<>();

        private final Set<String> invalidClientIds = new TreeSet<>();
        private final Set<String> invalidFachdienstIds = new TreeSet<>();
        private final Set<String> invalidScopeIds = new TreeSet<>();
        private final Set<String> invalidSektorAppIds = new TreeSet<>();

        public ConfigCache(InfoModel infoModel) {
            this.parsedInfoModel = infoModel;
            this.effectiveInfoModel = new InfoModel(infoModel);

            this.issuerTi = infoModel.getIssuerTi();
            this.issuerInternet = infoModel.getIssuerInet();
            this.issuers = Set.of(this.issuerInternet, this.issuerTi);
            this.authServerClientName = infoModel.getAuthServerClientName();
            this.authServerOrganizationName = infoModel.getAuthServerOrganizationName();
            this.pairingEndpoint = infoModel.getPairingEndpoint();
            this.salt = infoModel.getSalt();
            this.challengeTimeout = infoModel.getChallengeExpires();
            this.authCodeTimeout = infoModel.getAuthCodeExpires();
        }
    }
}
