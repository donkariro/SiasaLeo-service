package com.arriyiaconsulting.siasaleo.service.security.identity.control;

import com.arriyiaconsulting.siasaleo.service.infrastructure.mail.EmailMessage;
import com.arriyiaconsulting.siasaleo.service.infrastructure.mail.MailSender;
import com.arriyiaconsulting.siasaleo.service.infrastructure.sms.SmsSender;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Picks the transport from the Identifier variant: email addresses get the
 * code by mail, phone numbers by SMS. The wording of both messages lives
 * here so the transports stay content-agnostic.
 */
@ApplicationScoped
public class RoutingVerificationSender implements VerificationSender {

    @Inject
    private MailSender mail;

    @Inject
    private SmsSender sms;

    @Override
    public void send(Identifier destination, String code) {
        long minutes = RegistrationService.CODE_TTL.toMinutes();
        switch (destination) {
            case Identifier.Email(String address) -> mail.send(new EmailMessage(
                    address,
                    "Your SiasaLeo verification code",
                    """
                    Your SiasaLeo verification code is: %s

                    It expires in %d minutes. If you did not create a SiasaLeo \
                    account, you can ignore this message.
                    """.formatted(code, minutes)));
            case Identifier.Phone(String number) -> sms.send(number,
                    "SiasaLeo: %s is your verification code. It expires in %d minutes."
                            .formatted(code, minutes));
        }
    }
}
