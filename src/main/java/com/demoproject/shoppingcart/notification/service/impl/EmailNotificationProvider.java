package com.demoproject.shoppingcart.notification.service.impl;

import com.demoproject.shoppingcart.notification.entity.Notification;
import com.demoproject.shoppingcart.notification.model.NotificationChannel;
import com.demoproject.shoppingcart.notification.model.NotificationRequest;
import com.demoproject.shoppingcart.notification.service.NotificationProvider;
import com.demoproject.shoppingcart.notification.service.TemplateResolver;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationProvider implements NotificationProvider {

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;
    private final TemplateResolver templateResolver;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:https://nexis-store-sigma.vercel.app}")
    private String frontendUrl;

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
        
        String sanitizedFrontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        context.setVariable("frontendUrl", sanitizedFrontendUrl);
        
        String htmlBody = templateEngine.process(templateName, context);
        
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        
        helper.setSubject(request.getSubject());
        helper.setText(htmlBody, true);
        
        if (request.getTo() != null && !request.getTo().isEmpty()) {
            helper.setTo(request.getTo().toArray(new String[0]));
        }
        
        if (request.getCc() != null && !request.getCc().isEmpty()) {
            helper.setCc(request.getCc().toArray(new String[0]));
        }
        
        if (request.getBcc() != null && !request.getBcc().isEmpty()) {
            helper.setBcc(request.getBcc().toArray(new String[0]));
        }
        
        javaMailSender.send(mimeMessage);
        log.info("Email sent successfully for Notification ID: {}", notification.getId());
    }
}
