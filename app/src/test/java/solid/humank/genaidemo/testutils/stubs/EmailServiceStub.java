package solid.humank.genaidemo.testutils.stubs;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import solid.humank.genaidemo.testutils.isolation.TestResource;

/**
 * Email Service Stub
 */
public class EmailServiceStub implements TestResource {

    private final List<EmailRecord> sentEmails = new CopyOnWriteArrayList<>();
    private boolean shouldFailSending = false;
    private String failureReason = "Email sending failed";
    private boolean isCleanedUp = false;

    public EmailResult sendEmail(String to, String subject, String body) {
        if (isCleanedUp) {
            throw new IllegalStateException("Service has been cleaned up");
        }

        EmailRecord record = new EmailRecord(to, subject, body, System.currentTimeMillis());
        sentEmails.add(record);

        if (shouldFailSending) {
            return new EmailResult(false, failureReason);
        }

        return new EmailResult(true, "Email sent successfully");
    }

    public EmailResult sendEmail(String to, String from, String subject, String body) {
        return sendEmail(to, subject, body);
    }

    public void setSendingFailure(boolean shouldFail, String reason) {
        this.shouldFailSending = shouldFail;
        this.failureReason = reason;
    }

    public List<EmailRecord> getSentEmails() {
        return List.copyOf(sentEmails);
    }

    public int getEmailCount() {
        return isCleanedUp ? 0 : sentEmails.size();
    }

    public boolean hasEmailFor(String recipient) {
        return !isCleanedUp && sentEmails.stream()
                .anyMatch(record -> record.to().equals(recipient));
    }

    public boolean hasEmailWithSubject(String subject) {
        return !isCleanedUp && sentEmails.stream()
                .anyMatch(record -> record.subject().equals(subject));
    }

    public void clear() {
        if (!isCleanedUp) {
            sentEmails.clear();
            shouldFailSending = false;
            failureReason = "Email sending failed";
        }
    }

    @Override
    public void cleanup() throws Exception {
        if (!isCleanedUp) {
            clear();
            isCleanedUp = true;
        }
    }

    @Override
    public String getResourceName() {
        return "EmailServiceStub";
    }

    @Override
    public boolean isCleanedUp() {
        return isCleanedUp;
    }

    public record EmailRecord(String to, String subject, String body, long timestamp) {
    }

    public record EmailResult(boolean success, String message) {
    }
}