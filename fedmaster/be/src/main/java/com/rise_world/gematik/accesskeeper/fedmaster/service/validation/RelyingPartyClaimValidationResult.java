/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service.validation;

import java.util.Map;
import java.util.Optional;

import static java.util.Objects.nonNull;

public class RelyingPartyClaimValidationResult {

    private final RelyingPartyValidationDetails warning;
    private final RelyingPartyValidationDetails error;
    private final boolean lockParticipant;

    public RelyingPartyClaimValidationResult(RelyingPartyValidationDetails warning,
                                             RelyingPartyValidationDetails error,
                                             boolean lockParticipant) {
        this.warning = warning;
        this.error = error;
        this.lockParticipant = lockParticipant;
    }

    public Optional<RelyingPartyValidationDetails> getWarning() {
        return Optional.ofNullable(warning);
    }

    public Optional<RelyingPartyValidationDetails> getError() {
        return Optional.ofNullable(error);
    }

    public boolean isLockParticipant() {
        return nonNull(error) && lockParticipant;
    }

    public static RelyingPartyClaimValidationResult ok() {
        return new RelyingPartyClaimValidationResult(null, null, false);
    }

    public static RelyingPartyClaimValidationResult warning(String warning, Map<String, String> related) {
        return new RelyingPartyClaimValidationResult(new RelyingPartyValidationDetails(warning, related), null, false);
    }

    public static RelyingPartyClaimValidationResult error(String error, Map<String, String> related) {
        return new RelyingPartyClaimValidationResult(null, new RelyingPartyValidationDetails(error, related), false);
    }

    public static RelyingPartyClaimValidationResult lockingError(String error, Map<String, String> related) {
        return new RelyingPartyClaimValidationResult(null, new RelyingPartyValidationDetails(error, related), true);
    }
}
