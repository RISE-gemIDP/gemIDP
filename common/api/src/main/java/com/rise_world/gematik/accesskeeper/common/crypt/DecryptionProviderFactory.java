/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.common.crypt;

import com.rise_world.gematik.accesskeeper.common.dto.TokenType;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;

/**
 * Provides decryption providers
 */
public interface DecryptionProviderFactory {


    /**
     * Creates a JweDecryptionProvider that fits the requested TokenType
     *
     * @param type The TokenType that needs to be decrypted
     * @return the appropriate decryption provider for the requested TokenType, if none can be found
     *         an {@link IllegalArgumentException} will be thrown
     */
    JweDecryptionProvider createDecryptionProvider(TokenType type);
}
