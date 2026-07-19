package com.guru.erp.modules.notification;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

/**
 * Transactional email (ARCHITECTURE.md Phase 3 — notifications, new module).
 * Ported from the reference's SMTP helper: {@code fakeSend} logs instead of
 * sending (dev/staging without a mail server); unconfigured SMTP logs and
 * returns {@code false} rather than throwing — email delivery is best-effort
 * and must never fail the business operation that triggered it.
 */
@Service
@EnableConfigurationProperties(NotificationProperties.class)
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final NotificationProperties props;

    public EmailService(NotificationProperties props) {
        this.props = props;
    }

    /** Send a plain-text email. Returns whether it was actually dispatched. */
    public boolean send(String to, String subject, String body) {
        if (props.fakeSend()) {
            log.info("email.fake_send to={} subject={} bodyPreview={}", to, subject, preview(body));
            return true;
        }
        if (!props.configured()) {
            log.info("email.no_smtp to={} subject={} reason=SMTP not configured", to, subject);
            return false;
        }
        try {
            JavaMailSender sender = buildSender();
            MimeMessage message = sender.createMimeMessage();
            message.setFrom(new InternetAddress(props.fromEmail(), props.fromName()));
            message.setRecipients(MimeMessage.RecipientType.TO, to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            log.info("email.sent to={} subject={}", to, subject);
            return true;
        } catch (Exception e) {
            log.warn("email.send_failed to={} subject={}", to, subject, e);
            return false;
        }
    }

    private JavaMailSender buildSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(props.host());
        sender.setPort(props.port());
        if (props.username() != null && !props.username().isBlank()) {
            sender.setUsername(props.username());
            sender.setPassword(props.password());
        }
        var javaMailProps = sender.getJavaMailProperties();
        javaMailProps.put("mail.smtp.auth", props.username() != null && !props.username().isBlank());
        javaMailProps.put("mail.smtp.starttls.enable", props.useTls());
        javaMailProps.put("mail.smtp.connectiontimeout", "15000");
        javaMailProps.put("mail.smtp.timeout", "15000");
        return sender;
    }

    private static String preview(String body) {
        return body.length() <= 500 ? body : body.substring(0, 500);
    }
}
