package com.demoproject.shoppingcart.notification.util;

import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

@Component
public class NotificationExceptionClassifier {

    /**
     * Determines if a given exception during notification sending is transient (retryable)
     * or terminal (non-retryable).
     *
     * @param ex the exception thrown during sending
     * @return true if it's retryable, false otherwise
     */
    public boolean isRetryable(Throwable ex) {
        if (ex == null) {
            return false;
        }
        
        Throwable rootCause = getRootCause(ex);

        // Terminal errors: Authentication failures, parse errors, template issues
        if (rootCause instanceof MailAuthenticationException ||
            rootCause instanceof MailParseException ||
            rootCause instanceof IllegalArgumentException ||
            rootCause instanceof org.thymeleaf.exceptions.TemplateEngineException) {
            return false;
        }

        // Transient errors: Timeouts, connection issues
        if (rootCause instanceof SocketTimeoutException ||
            rootCause instanceof ConnectException ||
            rootCause instanceof org.springframework.mail.MailSendException) {
            return true;
        }

        // Default to retryable for unknown exceptions as a safeguard, 
        // though in a strict system you might default to false.
        return true; 
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause != cause.getCause()) {
            cause = cause.getCause();
        }
        return cause;
    }
}
