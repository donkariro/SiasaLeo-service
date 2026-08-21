package com.arriyiaconsulting.siasaleo.service.security.identity.control;

import com.arriyiaconsulting.siasaleo.service.infrastructure.mail.EmailMessage;
import com.arriyiaconsulting.siasaleo.service.infrastructure.mail.MailSender;
import com.arriyiaconsulting.siasaleo.service.infrastructure.sms.SmsSender;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The Identifier variant decides the transport; a code must never end up on
 * the wrong channel, and both message texts must actually contain the code.
 */
@ExtendWith(MockitoExtension.class)
class RoutingVerificationSenderTest {

    @Mock
    private MailSender mail;

    @Mock
    private SmsSender sms;

    @InjectMocks
    private RoutingVerificationSender sender;

    @Test
    void emailIdentifierGoesToMailOnly() {
        sender.send(new Identifier.Email("jane@example.com"), "483920");

        ArgumentCaptor<EmailMessage> sent = ArgumentCaptor.forClass(EmailMessage.class);
        verify(mail).send(sent.capture());
        assertEquals("jane@example.com", sent.getValue().to());
        assertTrue(sent.getValue().textBody().contains("483920"));
        verifyNoInteractions(sms);
    }

    @Test
    void phoneIdentifierGoesToSmsOnly() {
        sender.send(new Identifier.Phone("+254712345678"), "112233");

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(sms).send(eq("+254712345678"), text.capture());
        assertTrue(text.getValue().contains("112233"));
        verifyNoInteractions(mail);
    }
}
