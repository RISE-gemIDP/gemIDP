/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.server.dto.PARResponse;

public class PushedAuthResponse {
    private final int status;
    private final PARResponse parResponse;

    PushedAuthResponse(int status, PARResponse parResponse) {
        this.status = status;
        this.parResponse = parResponse;
    }

    public int status() {
        return status;
    }

    public PARResponse parResponse() {
        return parResponse;
    }
}
