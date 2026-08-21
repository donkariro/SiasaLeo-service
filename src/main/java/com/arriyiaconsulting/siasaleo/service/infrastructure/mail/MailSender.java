package com.arriyiaconsulting.siasaleo.service.infrastructure.mail;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Sends email over SMTP, configured through MicroProfile Config (mail.*
 * properties). Delivery runs on the container's managed executor so callers
 * never block on the SMTP conversation; failures are logged, not thrown —
 * flows that hand a message to this class must stay usable when mail is down
 * (the user can always request a resend).
 *
 * With mail.mode=log (the default, for development) nothing is transmitted:
 * the whole message, body included, goes to the server log. Because bodies
 * carry verification codes, production must run with mail.mode=smtp.
 */
@ApplicationScoped
public class MailSender {

    private static final Logger LOGGER = Logger.getLogger(MailSender.class.getName());

    @Inject
    @ConfigProperty(name = "mail.mode", defaultValue = "log")
    private String mode;

    @Inject
    @ConfigProperty(name = "mail.smtp.host", defaultValue = "localhost")
    private String host;

    @Inject
    @ConfigProperty(name = "mail.smtp.port", defaultValue = "587")
    private int port;

    @Inject
    @ConfigProperty(name = "mail.smtp.username")
    private Optional<String> username;

    @Inject
    @ConfigProperty(name = "mail.smtp.password")
    private Optional<String> password;

    @Inject
    @ConfigProperty(name = "mail.smtp.starttls", defaultValue = "true")
    private boolean starttls;

    @Inject
    @ConfigProperty(name = "mail.from", defaultValue = "no-reply@siasaleo.example")
    private String from;

    @Inject
    @ConfigProperty(name = "mail.from.name", defaultValue = "SiasaLeo")
    private String fromName;

    @Resource
    private ManagedExecutorService executor;

    private Session session;

    @PostConstruct
    void initSession() {
        if (!"smtp".equals(mode) && !"log".equals(mode)) {
            throw new IllegalStateException(
                    "mail.mode must be 'smtp' or 'log', was: " + mode);
        }
        if ("log".equals(mode)) {
            LOGGER.warning("mail.mode=log — emails are written to the server log "
                    + "instead of being sent. Not for production.");
            return;
        }
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        if (username.isPresent()) {
            props.put("mail.smtp.auth", "true");
            String user = username.get();
            String pass = password.orElseThrow(() -> new IllegalStateException(
                    "mail.smtp.username is set but mail.smtp.password is not"));
            session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });
        } else {
            session = Session.getInstance(props);
        }
    }

    /**
     * Queues the message for delivery and returns immediately. A failed
     * delivery is logged without the body, so codes never leak via smtp mode.
     */
    public void send(EmailMessage message) {
        if ("log".equals(mode)) {
            LOGGER.info(() -> "mail.mode=log — would send to %s%nSubject: %s%n%s"
                    .formatted(message.to(), message.subject(), message.textBody()));
            return;
        }
        executor.execute(() -> deliver(message));
    }

    private void deliver(EmailMessage message) {
        try {
            MimeMessage mime = new MimeMessage(session);
            mime.setFrom(new InternetAddress(from, fromName, StandardCharsets.UTF_8.name()));
            mime.setRecipient(Message.RecipientType.TO, new InternetAddress(message.to()));
            mime.setSubject(message.subject(), StandardCharsets.UTF_8.name());
            mime.setText(message.textBody(), StandardCharsets.UTF_8.name());
            Transport.send(mime);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    e, () -> "Failed to send email to %s (subject: %s)"
                            .formatted(message.to(), message.subject()));
        }
    }
}
