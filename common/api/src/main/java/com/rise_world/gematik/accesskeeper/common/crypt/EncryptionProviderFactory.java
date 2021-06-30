/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.crypt;

import com.rise_world.gematik.accesskeeper.common.dto.TokenType;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;

/**
 * Factory for JweEncryptionProvider
 */
public interface EncryptionProviderFactory {

    /**
     * Creates a JweEncryptionProvider that fits the internal encryption
     *
     * @param type The TokenType that needs to be encrypted
     * @return the appropriate encryption provider for the requested TokenType, if none can be found
     *         an {@link IllegalArgumentException} will be thrown
     */
    JweEncryptionProvider createEncryptionProvider(TokenType type);
}
