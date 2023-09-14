/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.common.crypt.KeyProvider;
import com.rise_world.gematik.accesskeeper.common.exception.AccessKeeperException;
import com.rise_world.gematik.accesskeeper.common.exception.ErrorCodes;
import com.rise_world.gematik.accesskeeper.common.token.creation.TokenCreationStrategy;
import com.rise_world.gematik.accesskeeper.common.token.extraction.parser.IdpJwsJwtCompactConsumer;
import com.rise_world.gematik.accesskeeper.server.dto.OpenidProviderDTO;
import com.rise_world.gematik.accesskeeper.server.model.SektorApp;
import com.rise_world.gematik.idp.server.api.federation.FederationEndpoint;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.rise_world.gematik.accesskeeper.common.crypt.KeyConstants.PUK_FEDMASTER_SIG;

@Component
public class IdentityFederationDirectoryServiceImpl implements IdentityFederationDirectoryService {

    private static final Logger LOG = LoggerFactory.getLogger(IdentityFederationDirectoryServiceImpl.class);

    private static final String FED_IDP_LIST = "fed_idp_list";
    private static final String FED_IDP_NAME = "idp_name";
    private static final String IDP_ISS = "idp_iss";
    private static final String IDP_SEK_2 = "idp_sek_2";
    private static final String IDP_PKV = "idp_pkv";
    private static final String IDP_LOGO = "idp_logo";
    private static final String ORGANIZATION_NAME = "organization_name";
    private static final String ISSUER = "iss";
    private static final String LOGO_URI = "logo_uri";
    private static final String PKV = "pkv";

    private final FederationEndpoint federationEndpoint;
    private final KeyProvider keyProvider;
    private final ConfigService configService;
    private final TokenCreationStrategy discStrategy;

    public IdentityFederationDirectoryServiceImpl(FederationEndpoint federationEndpoint,
                                                  KeyProvider keyProvider,
                                                  ConfigService configService,
                                                  @Qualifier("discStrategy") TokenCreationStrategy discStrategy) {
        this.federationEndpoint = federationEndpoint;
        this.keyProvider = keyProvider;
        this.discStrategy = discStrategy;
        this.configService = configService;
    }

    @Override
    public String getRemoteIdps() {
        List<Map<String, Object>> allFederatedIps = new ArrayList<>();

        for (OpenidProviderDTO sektorIdp : getOpenIdProviders()) {
            Map<String, Object> idp = new HashMap<>();
            idp.put(FED_IDP_NAME, sektorIdp.getOrganizationName());
            idp.put(IDP_ISS, sektorIdp.getIssuer());
            idp.put(IDP_SEK_2, Boolean.TRUE);
            idp.put(IDP_PKV, sektorIdp.isPkv());
            idp.put(IDP_LOGO, sektorIdp.getLogoUri());
            allFederatedIps.add(idp);
        }
        for (SektorApp sektorApp : configService.getSektorApps()) {
            Map<String, Object> idp = new HashMap<>();
            idp.put(FED_IDP_NAME, sektorApp.getName());
            idp.put(IDP_ISS, sektorApp.getId());
            idp.put(IDP_SEK_2, Boolean.FALSE);
            idp.put(IDP_PKV, Boolean.FALSE);
            idp.put(IDP_LOGO, "");
            allFederatedIps.add(idp);
        }
        // A_23683: analog zu A_20591-01 wird die 'discStrategy' verwendet um den Token mit PrK_DISC_SIG zu signieren.
        return discStrategy.toToken(new JwtClaims(Collections.singletonMap(FED_IDP_LIST, allFederatedIps)));
    }

    @Override
    public List<OpenidProviderDTO> getOpenIdProviders() {
        String identityProviders;
        try {
            identityProviders = this.federationEndpoint.listIdentityProviders();
        }
        catch (WebApplicationException e) {
            LOG.error("Federation Master request failed");
            throw new AccessKeeperException(ErrorCodes.FED_MASTER_NOT_AVAILABLE, e);
        }
        catch (ProcessingException e) {
            // handle timeout when remote service not available
            if ((e.getCause() instanceof SocketTimeoutException) || (e.getCause() instanceof ConnectException) ||
                (e.getCause() instanceof NoRouteToHostException) || (e.getCause() instanceof UnknownHostException)) {
                LOG.error("Federation Master is not available");
                throw new AccessKeeperException(ErrorCodes.FED_MASTER_NOT_AVAILABLE, e);
            }
            else {
                throw e;
            }
        }
        return validateFedmasterResponse(identityProviders);
    }

    private List<OpenidProviderDTO> validateFedmasterResponse(String fedMasterResponse) {
        List<OpenidProviderDTO> idpList = new ArrayList<>();

        JwsJwtCompactConsumer consumer;
        List<Map<String, Object>> sekIdps;
        try {
            consumer = new IdpJwsJwtCompactConsumer(fedMasterResponse.trim());
            JwtClaims claims = consumer.getJwtClaims();  // trigger token parsing

            sekIdps = claims.getListMapProperty("idp_entity");
            Validate.notNull(sekIdps);
        }
        catch (Exception e) {
            throw new AccessKeeperException(ErrorCodes.FED_TOKEN_INVALID, e);
        }

        if (!consumer.verifySignatureWith(keyProvider.getKey(PUK_FEDMASTER_SIG), SignatureAlgorithm.ES256)) {
            throw new AccessKeeperException(ErrorCodes.FED_INVALID_MASTER_SIGNATURE);
        }

        for (Map<String, Object> sekIdp : sekIdps) {
            if (isValidSektorIdp(sekIdp)) {
                String issuer = (String) sekIdp.get(ISSUER);
                String organizationName = (String) sekIdp.get(ORGANIZATION_NAME);
                String logoUri = (String) sekIdp.get(LOGO_URI);
                Boolean pkv = (Boolean) sekIdp.get(PKV);

                idpList.add(new OpenidProviderDTO(issuer, organizationName, logoUri, pkv));
            }
        }
        return idpList;
    }

    private boolean isValidSektorIdp(Map<String, Object> sekIdp) {
        if (!(sekIdp.get(ORGANIZATION_NAME) instanceof String name) || StringUtils.isBlank(name)) {
            LOG.error("sektor_idp has an invalid name");
            return false;
        }

        if (!(sekIdp.get(ISSUER) instanceof String issuer) || StringUtils.isBlank(issuer)) {
            LOG.error("sektor_idp has an invalid issuer");
            return false;
        }

        if (sekIdp.get(LOGO_URI) != null && !(sekIdp.get(LOGO_URI) instanceof String)) {
            LOG.error("sektor_idp has a missing logoUri");
            return false;
        }

        if (!(sekIdp.get(PKV) instanceof Boolean)) {
            LOG.error("sektor_idp has an invalid pkv flag");
            return false;
        }

        return true;
    }
}

