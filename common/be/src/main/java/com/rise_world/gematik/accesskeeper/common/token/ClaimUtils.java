/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.token;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObject;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.common.JoseHeaders;
import org.apache.cxf.rs.security.jose.common.JoseType;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

public class ClaimUtils {

    public static final String TOKEN_ID = "jti";
    public static final String SESSION_ID = "snc";
    public static final String AUTH_TIME = "auth_time";
    public static final String SCOPE = "scope";
    public static final String CLIENT_ID = "client_id";
    public static final String CERTIFICATE = "x5c";
    public static final String GIVEN_NAME = "given_name";
    public static final String FAMILY_NAME = "family_name";
    public static final String ORG_NAME = "organizationName";
    public static final String PROFESSION = "professionOID";
    public static final String ID_NUMBER = "idNummer";
    public static final String AUTH_PARTY = "azp";
    public static final String AUTH_CTX = "acr";
    public static final String AUTH_METHOD = "amr";
    public static final String TOKEN_TYPE = "token_type";
    public static final String CODE_CHALLENGE_METHOD = "code_challenge_method";
    public static final String CODE_CHALLENGE = "code_challenge";
    public static final String RESPONSE_TYPE = "response_type";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String STATE = "state";
    public static final String NONCE = "nonce";
    public static final String AT_HASH = "at_hash";
    public static final String NESTED_TOKEN = "njwt";
    public static final String TOKEN_KEY = "token_key";
    public static final String CODE_VERIFIER = "code_verifier";
    public static final String CHALLENGE_TOKEN = "challenge_token";
    public static final String AUTH_CERT = "auth_cert";

    public static final String HEADER_APU = "apu";
    public static final String HEADER_APV = "apv";
    public static final String HEADER_EPK = "epk";

    public static final String NESTED_TOKEN_CTY_VALUE = "NJWT";

    public static final List<String> CARD_CLAIMS = Collections.unmodifiableList(Arrays.asList(GIVEN_NAME, FAMILY_NAME, ORG_NAME, PROFESSION, ID_NUMBER));

    private static final Logger LOG = LoggerFactory.getLogger(ClaimUtils.class);

    private static final String SCOPE_REGEX = "([\\x21\\x23-\\x5B\\x5D-\\x7E]+)([\\x20]([\\x21\\x23-\\x5B\\x5D-\\x7E]+))*";

    private static final Pattern UNRESERVED_URI_CHARS = Pattern.compile("^[A-Za-z0-9_\\-\\.\\~]{43,128}$");

    private ClaimUtils() {
    }

    public static void copy(JwtClaims from, JwtClaims to, String... names) {
        copy(from, to, Arrays.asList(names));
    }

    public static void copy(JwtClaims from, JwtClaims to, Iterable<String> names) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and/or to need to be set");
        }

        for (String name : names) {
            if (from.containsProperty(name)) {
                to.setProperty(name, from.getProperty(name));
            }
        }
    }

    public static boolean containsAllClaims(JwtClaims claims, String... names) {
        for (String name : names) {
            if (!claims.containsProperty(name)) {
                LOG.debug("Claim '{}' is missing", name);
                return false;
            }
        }
        return true;
    }

    public static JwtClaims filter(JwtClaims claims, String... names) {
        JwtClaims filter = new JwtClaims();
        ClaimUtils.copy(claims, filter, names);
        return filter;
    }

    public static List<String> getScopes(JwtClaims claims) {
        String scopes = claims.getStringProperty(SCOPE);

        if (StringUtils.isNotEmpty(scopes) && Pattern.matches(SCOPE_REGEX, scopes)) {
            return new ArrayList<>(getScopes(scopes));
        }

        return Collections.emptyList();
    }

    public static List<String> getScopes(String scopes) {
        return Arrays.asList(scopes.trim().split("\\s+"));
    }

    public static <T> T unboxNestedTo(String content, Function<String, T> to) {
        JwtClaims payload = JwtUtils.jsonToClaims(content);
        if (!payload.containsProperty(ClaimUtils.NESTED_TOKEN)) {
            return null;
        }

        return to.apply(payload.getStringProperty(ClaimUtils.NESTED_TOKEN));
    }

    public static boolean isValidCodeVerifier(String codeVerifier) {
        return UNRESERVED_URI_CHARS.matcher(codeVerifier).matches();
    }

    /**
     * Get a claim as long value
     *
     * @param claims the map containing the claims
     * @param name   the requested claim name
     * @return the claim value or {@code null} if the value doesn't exist or is not parsable
     */
    public static Long getLongPropertyWithoutException(JsonMapObject claims, String name) {
        try {
            return claims.getLongProperty(name);
        }
        catch (NumberFormatException nfe) {
            return null;
        }
    }

    /**
     * Checks the "typ" header
     *
     * @param headers      the header map
     * @param expectedType the expected {@link JoseType} (must be not null)
     * @return {@code true} if the typ header exists and has the expected valued
     */
    public static boolean hasJoseType(JoseHeaders headers, JoseType expectedType) {
        return Objects.equals(headers.getHeader(JoseConstants.HEADER_TYPE), expectedType.toString());
    }

}
