/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;
import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantType;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;

import java.util.function.Predicate;

/**
 * Provides metadata as {@link JwtClaims} for a {@link ParticipantDto participant}.
 * <p>
 * Implementations of this class need to pass the {@link ParticipantType} as a
 * discriminator value.
 */
abstract class EntityStatementMetadataProvider {

    private final ParticipantType type;

    protected EntityStatementMetadataProvider(ParticipantType type) {
        this.type = type;
    }

    protected abstract JwtClaims typeSpecificMetadata(ParticipantDto participant);

    /**
     * Generates the metadata as {@link JwtClaims} for the given {@code participant}
     *
     * @param participant {@link ParticipantDto}
     * @return {@link JwtClaims} for the given {@link ParticipantDto participant}
     */
    JwtClaims metadata(ParticipantDto participant) {
        var metadata = new JwtClaims();
        metadata.setClaim(type.getType(), typeSpecificMetadata(participant));

        return metadata;
    }

    /**
     * {@link Predicate} to filter {@link EntityStatementMetadataProvider providers} by the
     * given {@link ParticipantType}
     *
     * @param type {@link ParticipantType} to filter
     * @return a {@link Predicate} which can be used for filtering for example in {@link java.util.stream.Stream} or {@link java.util.Optional}
     */
    static Predicate<EntityStatementMetadataProvider> forParticipantType(ParticipantType type) {
        return metadata -> metadata.type == type;
    }
}
