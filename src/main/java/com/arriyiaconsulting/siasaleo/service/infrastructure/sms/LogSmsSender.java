package com.arriyiaconsulting.siasaleo.service.infrastructure.sms;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.logging.Logger;

/**
 * Development stand-in until an SMS gateway is integrated: the text is only
 * written to the server log. Must be replaced before production — verification
 * codes must never be logged there.
 */
@ApplicationScoped
public class LogSmsSender implements SmsSender {

    private static final Logger LOGGER = Logger.getLogger(LogSmsSender.class.getName());

    @Override
    public void send(String phoneE164, String text) {
        LOGGER.info(() -> "SMS to %s: %s".formatted(phoneE164, text));
    }
}
