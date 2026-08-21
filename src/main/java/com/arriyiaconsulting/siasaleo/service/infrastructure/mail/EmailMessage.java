package com.arriyiaconsulting.siasaleo.service.infrastructure.mail;

/**
 * A plain-text email to a single recipient. Callers own the wording; the
 * sender address and transport details are the MailSender's concern.
 */
public record EmailMessage(String to, String subject, String textBody) {

    public EmailMessage {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Recipient address is required");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Subject is required");
        }
        if (textBody == null || textBody.isBlank()) {
            throw new IllegalArgumentException("Body is required");
        }
    }
}
