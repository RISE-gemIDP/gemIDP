/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairing.filter;

import com.rise_world.gematik.accesskeeper.pairingdienst.dto.AccessTokenDTO;

public class AuthorizationContext {

    // @AFO: A_21422 - AccessTokenDTO wird vom AuthorizationFilter in einem ThreadLocal abgelegt und so dem Servicelayer zur Verf&uuml;gung gestellt.
    private static final ThreadLocal<AccessTokenDTO> ACCESS_TOKEN = new ThreadLocal<>();

    private AuthorizationContext() {
        //no instances
    }

    /**
     * Gets the {@link AccessTokenDTO} of the current thread.
     *
     * @return the {@link AccessTokenDTO}
     */
    public static AccessTokenDTO getAccessToken() {
        return ACCESS_TOKEN.get();
    }

    /**
     * Gets the {@link AccessTokenDTO} of the current thread.
     *
     * @param accessToken the {@link AccessTokenDTO}
     */
    public static void setAccessToken(AccessTokenDTO accessToken) {
        ACCESS_TOKEN.set(accessToken);
    }

    /**
     * Remove the current thread's {@link AccessTokenDTO}.
     */
    public static void clear() {
        ACCESS_TOKEN.remove();
    }
}
