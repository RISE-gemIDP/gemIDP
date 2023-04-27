/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.util;

import com.rise_world.gematik.accesskeeper.fedmaster.dto.ParticipantDto;
import net.logstash.logback.marker.LogstashMarker;

import static net.logstash.logback.marker.Markers.append;

public class MarkerUtils {

    public static final String DOMAIN = "domain";
    public static final String ISSUER = "issuer";
    public static final String NOT_BEFORE = "not_before";
    public static final String NOT_AFTER = "not_after";
    public static final String PUBKEY_SHA_256 = "pubkey_sha256";
    public static final String MEMBER_PUBKEY_SHA_256 = "member_pubkey_sha256";
    public static final String MEMBER_ID = "member_id";
    public static final String MEMBER_URI = "member_uri";
    public static final String ZIS_ASSIGNMENT_GROUP = "zis_assignment_group";

    private MarkerUtils() {
        // avoid instantiation
    }

    public static LogstashMarker appendParticipant(ParticipantDto participant) {
        return append(MEMBER_ID, participant.getId())
            .and(append(MEMBER_URI, participant.getSub()))
            .and(append(ZIS_ASSIGNMENT_GROUP, participant.getZisGroup()));
    }

}
