/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.pairingdienst.dto;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * A plain (unencrypted), validated access token.
 */
public class AccessTokenDTO implements Serializable {

    private final String idNummer;
    private final List<String> amr;

    public AccessTokenDTO(String idNummer, List<String> amr) {
        this.idNummer = idNummer;
        this.amr = Collections.unmodifiableList(amr);
    }

    public String getIdNummer() {
        return idNummer;
    }

    public List<String> getAmr() {
        return amr;
    }
}
