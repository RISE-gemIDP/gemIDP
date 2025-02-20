/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.service.validation;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class RelyingPartyValidationResult {

    private final List<RelyingPartyValidationDetails> errors = new ArrayList<>();
    private final List<RelyingPartyValidationDetails> warnings = new ArrayList<>();

    private boolean lockParticipant;

    public void add(RelyingPartyClaimValidationResult result) {

        result.getError()
            .ifPresent(error -> addError(error, result.isLockParticipant()));

        result.getWarning().ifPresent(warnings::add);
    }

    private void addError(RelyingPartyValidationDetails error, boolean lockParticipant) {
        errors.add(error);
        this.lockParticipant = this.lockParticipant || lockParticipant;
    }

    public boolean lockParticipant() {
        return lockParticipant;
    }

    public Optional<RelyingPartyValidationDetails> errors() {
        if (errors.isEmpty()) {
            return Optional.empty();
        }

        return details(errors);
    }

    public Optional<RelyingPartyValidationDetails> warnings() {
        if (warnings.isEmpty()) {
            return Optional.empty();
        }

        return details(warnings);
    }

    private Optional<RelyingPartyValidationDetails> details(List<RelyingPartyValidationDetails> details) {
        var message = new StringBuilder();
        var related = new HashMap<String, String>();
        for (var detail : details) {
            if (!message.isEmpty()) {
                message.append(StringUtils.LF);
            }
            message.append(detail.getMessage());

            related.putAll(detail.getRelated());
        }

        return Optional.of(new RelyingPartyValidationDetails(message.toString(), related));
    }
}
