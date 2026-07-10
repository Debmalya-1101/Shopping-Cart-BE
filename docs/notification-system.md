# Notification Framework Documentation

This document explains the current implementation of the Notification Framework in our Spring Boot backend. It serves as a guide for future developers, a reference for interview preparation, and a deep-dive into production-grade notification systems.

---

## 1. Introduction

### What is a Notification System?
A Notification System is a dedicated component responsible for sending messages (Emails, SMS, Push, etc.) to users based on system events. 

### Why modern backend applications need one
Instead of tightly coupling email logic into core business services (like `OrderService`), modern systems decouple this logic. This ensures that a failure in the email server does not cause an entire order transaction to fail or slow down.

### Difference between directly sending emails and using a framework
Directly sending an email means writing `javaMailSender.send()` inside the business logic. Using a framework means publishing an event (`OrderPlacedEvent`), which is processed asynchronously. The framework handles templates, routing to the correct provider (Email, SMS), tracking history, and idempotency.

### Why our implementation is scalable
Our implementation uses Event-Driven Architecture, the Provider Pattern, and Idempotency. This means adding a new channel (like SMS) or a new event (like `PasswordResetEvent`) requires zero changes to existing core logic.

---

## 2. High Level Architecture

Our notification system follows an event-driven, decoupled architecture.

```text
       Client
          │
          ▼
   OrderService (Business Logic)
          │
  Publishes Domain Event (OrderPlacedEvent)
          │
          ▼
@TransactionalEventListener (NotificationEventListener)
          │
          ▼
 NotificationService
          │ 
          ├─► Checks Idempotency & Saves Notification Entity
          │
          ▼
  Provider Registry
          │
          ▼
 Email Notification Provider
          │
          ▼
    Gmail REST API (HTTPS)
```

**Step-by-step:**
1. The business service publishes an event (e.g., `OrderPlacedEvent`).
2. The `NotificationEventListener` intercepts the event *only after* the transaction commits successfully.
3. The listener builds a `NotificationRequest` and passes it to the `NotificationService`.
4. The service generates an idempotency key, saves the notification to the database, and looks up the correct provider (Email) from the `ProviderRegistry`.
5. The provider processes the template, builds a MIME payload, and sends the email via the Gmail REST API over HTTPS.

---

## 3. Package Structure

The `notification` module is structured by domain responsibilities:

```text
notification
├── entity       # Database JPA entities (e.g., Notification)
├── job          # Scheduled tasks (e.g., NotificationCleanupJob)
├── listener     # Spring Event listeners (e.g., NotificationEventListener)
├── model        # DTOs and Enums (NotificationRequest, NotificationChannel, etc.)
├── repository   # Spring Data JPA repositories
├── service      # Core services, interfaces, and implementations
│   └── impl     # Provider implementations, Registries, Resolvers
└── util         # Utilities (e.g., Exception classification)
```

- **entity**: Defines how a notification is saved to the DB.
- **listener**: Bridges domain events to notification requests.
- **model**: Defines the API and payloads for notifications.
- **service/impl**: Contains the core logic, provider resolution, and actual sending mechanisms.

---

## 4. Database Design

The `notifications` table acts as our audit log and idempotency store.

| Column | Datatype | Purpose |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique identifier for the database row. |
| `reference_id` | VARCHAR | The ID of the domain entity triggering this (e.g., the Order ID). |
| `idempotency_key` | VARCHAR (UNIQUE) | Prevents duplicate notifications. e.g., `ORDER_123_ORDER_PLACED_EMAIL`. |
| `notification_type` | VARCHAR | The type of event (`ORDER_PLACED`, `PAYMENT_SUCCESS`). |
| `channel` | VARCHAR | The delivery method (`EMAIL`). |
| `recipient_to` | VARCHAR | The destination address. |
| `status` | VARCHAR | Current state (`PENDING`, `SENT`, `FAILED`). |
| `retry_count` | INT | Number of times this notification was retried. |
| `failure_reason` | TEXT | If it failed, stores the exact exception message. |
| `metadata` | TEXT | JSON string holding Correlation ID and Trace ID for distributed tracing. |

---

## 5. Core Components

### `NotificationRequest`
**Purpose**: A standardized object passed to the NotificationService. 
**Responsibilities**: Holds the payload, metadata, type, and channel regardless of the event source.
```java
public class NotificationRequest {
    private NotificationType type;
    private NotificationChannel channel;
    private String referenceId;
    private Map<String, Object> payload;
    private NotificationMetadata metadata;
    // ... basic to, cc, subject fields
}
```

### `NotificationService` & `NotificationServiceImpl`
**Purpose**: The orchestrator of the framework.
**Responsibilities**: Handles MDC logging, idempotency checks, database persistence, metric tracking, and delegating to the registry.
```java
// Snippet from NotificationServiceImpl
String idempotencyKey = generateIdempotencyKey(request);

if (notificationRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
    return; // Duplicate detected
}

Notification notification = createAndSaveNotification(request, idempotencyKey);
NotificationProvider provider = providerRegistry.getProvider(request.getChannel());
provider.send(request, notification);
```

### `NotificationProvider` & `EmailNotificationProvider`
**Purpose**: Abstracts the actual sending mechanism.
**Responsibilities**: Implementations define *how* a notification is sent. `EmailNotificationProvider` processes Thymeleaf templates and uses the Google API Client to send over HTTPS (bypassing restricted SMTP ports).
```java
// Snippet from EmailNotificationProvider
String templateName = templateResolver.resolve(request.getType(), request.getChannel());
Context context = new Context();
context.setVariables(request.getPayload());
String htmlBody = templateEngine.process(templateName, context);
// Build MimeMessage and send via Gmail Service...
```

### `NotificationProviderRegistry`
**Purpose**: Holds a map of all available providers.
**Responsibilities**: Uses an `EnumMap` to map `NotificationChannel` to a `NotificationProvider`.

### `TemplateResolver`
**Purpose**: Determines which HTML template to load.
**Responsibilities**: Formats the channel and type into a file path (e.g. `email/order-placed`).

---

## 6. End-to-End Workflow

1. **Order Placed**: User checks out.
2. **OrderPlacedEvent Published**: `OrderService` publishes the Spring domain event.
3. **Transactional Event Listener**: `NotificationEventListener` catches the event *after* the DB commit.
4. **NotificationRequest Created**: The listener maps the event data into a generic `NotificationRequest`.
5. **Notification Entity Saved**: `NotificationService` generates an idempotency key and saves a `PENDING` notification in the DB.
6. **Provider Registry**: The service asks the registry for the `EMAIL` provider.
7. **Email Provider**: Uses `TemplateResolver` to find the Thymeleaf template, injects the payload, and creates a MimeMessage.
8. **Email Sent**: The Gmail REST API dispatches the email via standard HTTPS.
9. **Notification Status Updated**: If successful, status becomes `SENT`. If it throws an exception, the `handleFailure` method catches it, logs it, increments `retryCount`, and marks it as `FAILED`.

---

## 7. Event Driven Architecture

### What is a Domain Event?
A domain event is a message stating that something significant happened in the business domain (e.g., "Order was placed").

### Why use Spring Events?
Instead of tightly coupling classes (e.g., `OrderService` injecting `EmailService`), the `OrderService` simply announces an event. The `NotificationEventListener` listens for it independently.

### Benefits
- **Separation of Concerns**: Order logic doesn't care about SMTP servers.
- **Testability**: You can test order creation without mocking email services.
- **Extensibility**: Later, we can add a `SmsEventListener` without touching the `OrderService`.

---

## 8. @TransactionalEventListener

### AFTER_COMMIT
Our listener uses `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.

### Why AFTER_COMMIT?
If the database transaction for creating the Order rolls back (e.g., out of stock, network failure), the event is **discarded**. We do not want to send an "Order Confirmation" email to a customer if the order failed to save in the database.

### What if BEFORE_COMMIT was used?
The email might send, but then the transaction could fail a millisecond later. The customer would receive an email for an order that doesn't exist.

---

## 9. @Async

### Why asynchronous?
Sending an email over the network takes time (often 500ms - 2s). 

### Difference
- **Synchronous**: The user clicks "Checkout" and waits looking at a loading spinner while the backend connects to the Gmail API to send the email.
- **Asynchronous**: The user clicks "Checkout", the backend saves the order, returns a "Success" response instantly, and a background thread handles the email.

By tagging our listener with `@Async`, the HTTP response is returned immediately to the frontend.

---

## 10. Provider Pattern

### Why we use NotificationProvider
We program to interfaces, not implementations. The `NotificationService` does not know how to send an email. It just calls `provider.send()`.

### Future additions (Open/Closed Principle)
If we want to add SMS notifications later:
1. Create `SmsNotificationProvider implements NotificationProvider`.
2. Return `NotificationChannel.SMS`.
The Spring framework automatically detects it, adds it to the registry, and the core `NotificationService` works without modifying a single line of existing code. This satisfies the Open/Closed Principle (open for extension, closed for modification).

---

## 11. Provider Registry

### Why Registry over If-Else?
Without a registry, the service would look like this:
```java
if (channel == EMAIL) { emailProvider.send(); }
else if (channel == SMS) { smsProvider.send(); }
```
This violates the Open/Closed principle because every new channel requires changing the `NotificationService`.

### O(1) Lookup
Our registry uses an `EnumMap`. Upon application startup (`@PostConstruct`), it iterates over all beans implementing `NotificationProvider` and stores them in a Map. When sending, it does a lightning-fast `registry.get(channel)` lookup.

---

## 12. Template Resolver

### Why not hardcode?
Hardcoding template names (e.g., `"email-order-confirmation.html"`) scatters configuration logic.

### How it works
The `ThymeleafTemplateResolver` automatically deduces the path using standard naming conventions:
```java
String folder = channel.name().toLowerCase();
String templateName = type.name().toLowerCase().replace("_", "-");
return folder + "/" + templateName;
```
For an `ORDER_PLACED` email, it resolves to `"email/order-placed"`. To add a new template, simply drop a file matching this convention into the templates folder.

---

## 13. Idempotency

### The Problem
If a network glitch causes an event to fire twice, or if a user double-clicks "Checkout", they could receive two identical emails.

### How it works
We generate a unique, deterministic key:
```java
String idempotencyKey = request.getReferenceId() + "_" + request.getType().name() + "_" + request.getChannel().name();
// e.g., "1045_ORDER_PLACED_EMAIL"
```
Before processing, we check the database for this key. If it exists, we silently ignore the request. The database column is also marked `UNIQUE` to prevent race conditions.

---

## 14. Notification History

### Why store history?
We persist every notification request as an Entity before attempting to send it.
- **Audit**: Customer service can verify if/when a user was emailed.
- **Troubleshooting**: We store the exact `failure_reason` (stack trace) in the DB if the Gmail API rejects it (e.g., auth failure).
- **Metrics**: Allows us to query success/failure rates.

---

## 15. Retry Mechanism

### How it works
If a failure occurs, the `handleFailure` method uses the `NotificationExceptionClassifier` to determine if the error was a transient network glitch (`isRetryable`) or a hard failure (e.g., Invalid Email Format).

If retryable, the `retryCount` is incremented. Currently, the status is marked as `FAILED` with a retry reason, laying the groundwork for a future cron job (`Scheduled Retry Worker`) that will periodically sweep the database for retryable failures and attempt them again.

---

## 16. Structured Logging

### MDC (Mapped Diagnostic Context)
When processing thousands of background events, logs become tangled. 
We inject a `correlationId` into the logging context:
```java
try (MDC.MDCCloseable cId = MDC.putCloseable("correlationId", correlationId)) {
    // ... logic
}
```
This ensures every log line generated during this email process shares the same ID, making it trivial to trace the lifecycle of a specific notification in log aggregators (like ELK/Splunk).

---

## 17. Metrics

### Micrometer
We inject `MeterRegistry` to track operational health.
- **Counters**: `meterRegistry.counter("notifications.sent").increment();` tracks totals.
- **Timers**: `Timer.Sample sample = Timer.start(meterRegistry);` tracks exactly how many milliseconds the Gmail API takes to respond.

These metrics automatically expose themselves to the `/actuator/prometheus` endpoint, which tools like Grafana scrape to build real-time monitoring dashboards.

---

## 18. Email Templates

### Thymeleaf
We use Spring's Thymeleaf integration. Templates are stored in `src/main/resources/templates/email/`.
When rendering, we pass a `Context` containing variables (like `totalAmount`, `orderId`). Thymeleaf injects these variables into the HTML using `th:text="${totalAmount}"` syntax.

---

## 19. Configuration

Located in `application.properties`:
```properties
# ── Gmail REST API (OAuth2) Configuration ────────────────────────────────────
gmail.oauth2.client-id=${GMAIL_CLIENT_ID}
gmail.oauth2.client-secret=${GMAIL_CLIENT_SECRET}
gmail.oauth2.refresh-token=${GMAIL_REFRESH_TOKEN}
gmail.oauth2.sender-email=nexis.store.vercel@gmail.com
```
We use environment variables (`${GMAIL_CLIENT_SECRET}`) so that raw credentials are never committed to the GitHub repository, maintaining strict security standards. 
Additionally, we use the Gmail REST API (HTTPS) instead of traditional SMTP because many cloud platforms (like Render) block outbound SMTP traffic (ports 25, 465, 587) on their free tiers to prevent spam.

---

## 20. Sequence Diagram

```text
User           OrderService       NotificationListener       NotificationService        ProviderRegistry          EmailProvider             GmailAPI
 │                  │                      │                          │                         │                       │                     │
 │─Place Order─────►│                      │                          │                         │                       │                     │
 │                  │─Publish Event───────►│                          │                         │                       │                     │
 │◄─Return Success──│                      │                          │                         │                       │                     │
 │                  │                      │─Map to Request──────────►│                         │                       │                     │
 │                  │                      │                          │─Check Idempotency       │                       │                     │
 │                  │                      │                          │─Save PENDING to DB      │                       │                     │
 │                  │                      │                          │─Get Provider(EMAIL)────►│                       │                     │
 │                  │                      │                          │◄─Return EmailProvider───│                       │                     │
 │                  │                      │                          │─send(request, entity)──────────────────────────►│                     │
 │                  │                      │                          │                         │                       │─Parse Template      │
 │                  │                      │                          │                         │                       │─Send Payload───────►│
 │                  │                      │                          │                         │                       │◄─Success Response───│
 │                  │                      │                          │◄─Update Status to SENT──────────────────────────│                     │
```

---

## 21. Design Principles

- **Single Responsibility (SRP)**: The `EmailNotificationProvider` only knows how to send emails. The `NotificationEventListener` only knows how to map events.
- **Open/Closed (OCP)**: The registry allows us to add new providers without modifying existing service code.
- **Dependency Injection (DI)**: Components request dependencies (like `TemplateResolver`) via constructors, relying on Spring's IoC container.
- **Strategy Pattern**: The `NotificationProvider` acts as a strategy interface. The runtime chooses the specific strategy based on the channel.

---

## 22. Future Improvements

*Note: The following are conceptual and NOT currently implemented in the codebase.*

- **Message Broker (Kafka/RabbitMQ)**: Replacing simple Spring Events with a distributed message queue for guaranteed delivery across microservices.
- **SMS & Push Providers**: Implementing Twilio or Firebase providers.
- **Scheduled Retry Worker**: A cron job that actively sweeps the DB for `FAILED` notifications with `retryCount < 3` and re-queues them.
- **Outbox Pattern**: Saving the domain event to a database table in the exact same transaction as the order, guaranteeing 100% atomicity before publishing to a broker.

---

## 23. Interview Questions

1. **Why use `@TransactionalEventListener(phase = AFTER_COMMIT)` instead of `@EventListener`?**
   *Answer*: To ensure the notification is only queued if the database transaction holding the core business logic (e.g., saving the order) successfully commits. If it rolls back, the email is never sent.
2. **What problem does the Provider Registry solve?**
   *Answer*: It prevents large `switch` or `if/else` statements in the service layer, adhering to the Open/Closed Principle.
3. **How does our system guarantee idempotency?**
   *Answer*: By generating a unique string from the `referenceId`, `type`, and `channel`, checking it against a unique column in the database, and discarding the request if it already exists.
4. **Why is the `@Async` annotation critical in the Event Listener?**
   *Answer*: Without it, the thread handling the user's HTTP request would block and wait for the SMTP server to respond before returning a success message to the UI.
5. **What is MDC and why do we use it in the NotificationService?**
   *Answer*: Mapped Diagnostic Context. We use it to inject a `correlationId` into the thread, ensuring all logs related to a specific notification share an ID, making distributed tracing and debugging possible.
6. **Why persist a notification entity before sending it?**
   *Answer*: To ensure an audit trail. If the JVM crashes right as the email is being sent, we have a record in the database that a notification was requested but never marked as `SENT`.
7. **How do we track the performance of the email provider?**
   *Answer*: We use Micrometer's `Timer.Sample` to wrap the `provider.send()` execution, exposing the exact latency metrics to Prometheus/Grafana.
8. **What is the Strategy Pattern and where is it used here?**
   *Answer*: The Strategy Pattern enables selecting an algorithm at runtime. We use it via the `NotificationProvider` interface; the `NotificationService` dynamically chooses the `EmailNotificationProvider` strategy based on the channel.
9. **If the Gmail API is down, what happens?**
   *Answer*: The provider throws an exception. `handleFailure` catches it, records the stack trace in `failureReason`, increments the `retryCount`, and updates the status to `FAILED`.
10. **Why avoid calling the email API directly inside `OrderService`?**
    *Answer*: Tight coupling. It mixes business logic with infrastructure logic, makes testing harder, and reduces the resiliency of the checkout flow.
11. **What is a Correlation ID?**
    *Answer*: A unique identifier (UUID) generated at the origin of a request and passed along to all downstream systems and logs to track the full lifecycle of an action.
12. **How does the TemplateResolver avoid hardcoded strings?**
    *Answer*: It uses naming conventions derived from the enums (e.g., `NotificationType.ORDER_PLACED` becomes `order-placed`), automatically mapping domain events to filesystem paths.
13. **Why do we use the Gmail REST API instead of standard SMTP?**
    *Answer*: Because cloud hosting providers (like Render, Heroku) frequently block outbound traffic on SMTP ports (25, 465, 587) on free tiers to prevent spam. The REST API runs over standard HTTPS (port 443) and is never blocked.
14. **What is the purpose of the `metadata` column in the database?**
    *Answer*: It stores a JSON payload containing tracing information (correlation IDs, trace IDs, source services) to assist in debugging.
15. **If two threads try to insert the exact same notification simultaneously, what happens?**
    *Answer*: The `idempotency_key` column has a database-level `UNIQUE` constraint. One thread will succeed, the other will throw a `DataIntegrityViolationException` which we gracefully catch and ignore.
16. **Why use Thymeleaf instead of string concatenation for email bodies?**
    *Answer*: Security (prevents HTML injection), maintainability (designers can edit HTML files directly), and simplicity of injecting dynamic variables.
17. **What is the purpose of `NotificationCleanupJob`?**
    *Answer*: To prevent the database from growing indefinitely, it periodically deletes old `SENT` or `FAILED` notifications to save storage (vital for constrained environments like TiDB free tier).
18. **How does our implementation differentiate between a transient error and a terminal error?**
    *Answer*: We use a `NotificationExceptionClassifier` utility to analyze the exception type. Transient errors (timeouts) are marked retryable, whereas terminal errors (authentication failures) are not.
19. **What is the difference between Spring Events and Kafka?**
    *Answer*: Spring Events run within the same JVM memory space. Kafka is a distributed message broker. If the server crashes, Spring Events in memory are lost, whereas Kafka persists them to disk.
20. **How would you add WhatsApp notifications to this framework?**
    *Answer*: Create a `WhatsAppNotificationProvider`, implement the `NotificationProvider` interface, return `NotificationChannel.WHATSAPP`, and add the API credentials. Zero changes to core logic required.

---

## 24. Key Takeaways

The Notification Framework transforms a traditionally rigid, error-prone task (sending emails) into a resilient, decoupled sub-domain. By leveraging Spring Events, the Provider Pattern, and rigorous database auditing, the system achieves maximum extensibility. Developers can safely add new events, swap email providers, or introduce new channels (like SMS) with complete confidence that the core business logic remains untouched and secure.
