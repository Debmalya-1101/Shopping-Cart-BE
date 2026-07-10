package com.demoproject.shoppingcart.notification.service.impl;

import com.demoproject.shoppingcart.notification.entity.Notification;
import com.demoproject.shoppingcart.notification.model.NotificationChannel;
import com.demoproject.shoppingcart.notification.model.NotificationRequest;
import com.demoproject.shoppingcart.notification.service.NotificationProvider;
import com.demoproject.shoppingcart.notification.service.TemplateResolver;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Slf4j
@Component
public class EmailNotificationProvider implements NotificationProvider {

    private final SpringTemplateEngine templateEngine;
    private final TemplateResolver templateResolver;

    @Value("${app.frontend.url:https://nexis-store-sigma.vercel.app}")
    private String frontendUrl;

    @Value("${gmail.oauth2.client-id}")
    private String clientId;

    @Value("${gmail.oauth2.client-secret}")
    private String clientSecret;

    @Value("${gmail.oauth2.refresh-token}")
    private String refreshToken;

    @Value("${gmail.oauth2.sender-email}")
    private String senderEmail;

    public EmailNotificationProvider(SpringTemplateEngine templateEngine, TemplateResolver templateResolver) {
        this.templateEngine = templateEngine;
        this.templateResolver = templateResolver;
    }

    @Override
    public NotificationChannel getSupportedChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(NotificationRequest request, Notification notification) throws Exception {
        String templateName = templateResolver.resolve(request.getType(), request.getChannel());

        Context context = new Context();
        if (request.getPayload() != null) {
            context.setVariables(request.getPayload());
        }

        String sanitizedFrontendUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
        context.setVariable("frontendUrl", sanitizedFrontendUrl);

        String htmlBody = templateEngine.process(templateName, context);

        // Build Gmail service with OAuth2 credentials
        Gmail gmailService = buildGmailService();

        // Build the MIME message
        MimeMessage mimeMessage = createMimeMessage(request, htmlBody);

        // Encode to Base64 and send via Gmail REST API
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        mimeMessage.writeTo(buffer);
        byte[] rawBytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(rawBytes);

        Message message = new Message();
        message.setRaw(encodedEmail);

        gmailService.users().messages().send("me", message).execute();
        log.info("Email sent via Gmail REST API successfully for Notification ID: {}", notification.getId());
    }

    /**
     * Builds an authenticated Gmail service using the stored OAuth2 refresh token.
     * The GoogleCredential automatically refreshes the access token when it expires.
     */
    @SuppressWarnings("deprecation")
    private Gmail buildGmailService() throws Exception {
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                .setJsonFactory(GsonFactory.getDefaultInstance())
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(refreshToken);

        // Force a token refresh on first use
        credential.refreshToken();

        return new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("Nexis Shopping Cart")
                .build();
    }

    /**
     * Creates a standard MIME email message using Jakarta Mail (in-memory only, not sent via SMTP).
     */
    private MimeMessage createMimeMessage(NotificationRequest request, String htmlBody) throws Exception {
        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(senderEmail, "Nexis Store"));
        email.setSubject(request.getSubject(), "UTF-8");
        email.setContent(htmlBody, "text/html; charset=UTF-8");

        if (request.getTo() != null && !request.getTo().isEmpty()) {
            email.setRecipients(MimeMessage.RecipientType.TO, parseAddresses(request.getTo()));
        }
        if (request.getCc() != null && !request.getCc().isEmpty()) {
            email.setRecipients(MimeMessage.RecipientType.CC, parseAddresses(request.getCc()));
        }
        if (request.getBcc() != null && !request.getBcc().isEmpty()) {
            email.setRecipients(MimeMessage.RecipientType.BCC, parseAddresses(request.getBcc()));
        }

        return email;
    }

    private InternetAddress[] parseAddresses(List<String> addresses) throws Exception {
        return Arrays.stream(InternetAddress.parse(String.join(",", addresses)))
                .toArray(InternetAddress[]::new);
    }
}
