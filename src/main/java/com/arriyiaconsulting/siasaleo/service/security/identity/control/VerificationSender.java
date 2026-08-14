package com.arriyiaconsulting.siasaleo.service.security.identity.control;

import com.arriyiaconsulting.siasaleo.service.security.identity.entity.Identifier;

/**
 * Delivers a verification code to the contact point being verified. The
 * transport (SMS gateway vs email) follows from the Identifier variant.
 */
public interface VerificationSender {

    void send(Identifier destination, String code);
}
