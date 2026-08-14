package com.arriyiaconsulting.siasaleo.service.security.identity.control;

import com.arriyiaconsulting.siasaleo.service.security.identity.entity.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.logging.Logger;

/**
 * Development stand-in until an SMS/email gateway is integrated: the code is
 * only written to the server log. Must be replaced before production — codes
 * must never be logged there.
 */
@ApplicationScoped
public class LogVerificationSender implements VerificationSender {

    private static final Logger LOGGER = Logger.getLogger(LogVerificationSender.class.getName());

    @Override
    public void send(Identifier destination, String code) {
        LOGGER.info(() -> "Verification code for %s %s: %s"
                .formatted(destination.type(), destination.value(), code));
    }
}
