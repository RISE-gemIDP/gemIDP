/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.fedmaster.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class SchedulingExceptionMapper {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulingExceptionMapper.class);

    /**
     * Method handles {@link SchedulingException} for Spring Web
     * and returns HTTP status code 406
     *
     * @param exception thrown exception
     */
    @ExceptionHandler(SchedulingException.class)
    @ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE, reason = "Scheduled synchronization is already running")
    public void toResponse(Exception exception) {
        LOG.info("Scheduling exception was thrown", exception);
    }

}
