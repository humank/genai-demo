# Webhooks

> **Status**: ✅ Active  
> **Last Updated**: 2024-11-19

## Overview

This document describes the webhook system for the GenAI Demo API, allowing real-time event notifications to external systems.

---

## Quick Reference

For related documentation, see:
- [API Overview](../README.md) - Complete API documentation
- [Event Catalog](../events/event-catalog.md) - Available events
- [Integration Guide](README.md) - Integration overview

---

## Webhook Events

### Available Events

| Event Type | Description | Payload |
|------------|-------------|---------|
| `order.created` | New order created | Order details |
| `order.updated` | Order status changed | Order details |
| `order.cancelled` | Order cancelled | Order ID, reason |
| `payment.completed` | Payment successful | Payment details |
| `payment.failed` | Payment failed | Payment ID, error |
| `customer.created` | New customer registered | Customer details |
| `customer.updated` | Customer profile updated | Customer details |
| `inventory.low` | Low inventory alert | Product ID, quantity |

---

## Webhook Configuration

### Register Webhook

```http
POST /api/v1/webhooks
Authorization: Bearer <token>
Content-Type: application/json

{
  "url": "https://your-domain.com/webhooks/genai-demo",
  "events": ["order.created", "order.updated", "payment.completed"],
  "secret": "your-webhook-secret",
  "active": true
}
```

### Response

```json
{
  "id": "webhook_123",
  "url": "https://your-domain.com/webhooks/genai-demo",
  "events": ["order.created", "order.updated", "payment.completed"],
  "secret": "whsec_***",
  "active": true,
  "createdAt": "2024-11-19T10:00:00Z"
}
```

---

## Webhook Payload

### Structure

```json
{
  "id": "evt_123",
  "type": "order.created",
  "timestamp": "2024-11-19T10:00:00Z",
  "data": {
    "orderId": "ORDER-001",
    "customerId": "CUST-001",
    "status": "PENDING",
    "totalAmount": 150.00,
    "items": [
      {
        "productId": "PROD-001",
        "quantity": 2,
        "price": 75.00
      }
    ]
  }
}
```

### Signature Verification

```http
POST /your-webhook-endpoint
Content-Type: application/json
X-Webhook-Signature: sha256=abc123...
X-Webhook-Timestamp: 1700000000

{
  "id": "evt_123",
  "type": "order.created",
  ...
}
```

---

## Security

### Signature Verification

```java
public class WebhookSignatureVerifier {
    
    public boolean verifySignature(
            String payload,
            String signature,
            String timestamp,
            String secret) {
        
        // Construct signed payload
        String signedPayload = timestamp + "." + payload;
        
        // Calculate expected signature
        String expectedSignature = calculateHmacSha256(signedPayload, secret);
        
        // Compare signatures
        return MessageDigest.isEqual(
            signature.getBytes(),
            expectedSignature.getBytes()
        );
    }
    
    private String calculateHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate signature", e);
        }
    }
}
```

### JavaScript/Node.js Example

```javascript
const crypto = require('crypto');

function verifyWebhookSignature(payload, signature, timestamp, secret) {
  const signedPayload = `${timestamp}.${payload}`;
  const expectedSignature = crypto
    .createHmac('sha256', secret)
    .update(signedPayload)
    .digest('hex');
  
  return crypto.timingSafeEqual(
    Buffer.from(signature),
    Buffer.from(`sha256=${expectedSignature}`)
  );
}

// Express middleware
app.post('/webhooks/genai-demo', (req, res) => {
  const signature = req.headers['x-webhook-signature'];
  const timestamp = req.headers['x-webhook-timestamp'];
  const payload = JSON.stringify(req.body);
  
  if (!verifyWebhookSignature(payload, signature, timestamp, process.env.WEBHOOK_SECRET)) {
    return res.status(401).json({ error: 'Invalid signature' });
  }
  
  // Process webhook
  handleWebhook(req.body);
  res.status(200).json({ received: true });
});
```

---

## Retry Logic

### Retry Strategy

- **Initial Attempt**: Immediate
- **Retry 1**: After 1 minute
- **Retry 2**: After 5 minutes
- **Retry 3**: After 15 minutes
- **Retry 4**: After 1 hour
- **Retry 5**: After 6 hours

### Success Criteria

- HTTP status code 2xx
- Response received within 30 seconds

### Failure Handling

```java
@Component
public class WebhookDeliveryService {
    
    @Async
    public void deliverWebhook(Webhook webhook, WebhookEvent event) {
        int maxRetries = 5;
        int[] retryDelays = {60, 300, 900, 3600, 21600}; // seconds
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                HttpResponse response = sendWebhook(webhook, event);
                
                if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                    logSuccess(webhook, event, attempt);
                    return;
                }
                
                if (attempt < maxRetries) {
                    Thread.sleep(retryDelays[attempt] * 1000);
                }
                
            } catch (Exception e) {
                logFailure(webhook, event, attempt, e);
                
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelays[attempt] * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // All retries failed
        moveToDeadLetterQueue(webhook, event);
        notifyWebhookOwner(webhook, event);
    }
}
```

---

## Testing Webhooks

### Test Endpoint

```http
POST /api/v1/webhooks/{webhookId}/test
Authorization: Bearer <token>
Content-Type: application/json

{
  "eventType": "order.created"
}
```

### Response

```json
{
  "success": true,
  "statusCode": 200,
  "responseTime": 145,
  "message": "Webhook delivered successfully"
}
```

### Local Testing with ngrok

```bash
# Start ngrok
ngrok http 3000

# Register webhook with ngrok URL
curl -X POST https://api.example.com/api/v1/webhooks \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://abc123.ngrok.io/webhooks",
    "events": ["order.created"]
  }'
```

---

## Monitoring

### Webhook Metrics

```java
@Component
public class WebhookMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordWebhookDelivery(
            String webhookId,
            String eventType,
            boolean success,
            long responseTime) {
        
        Counter.builder("webhook.deliveries")
            .tag("webhook_id", webhookId)
            .tag("event_type", eventType)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .increment();
        
        Timer.builder("webhook.response.time")
            .tag("webhook_id", webhookId)
            .tag("event_type", eventType)
            .register(meterRegistry)
            .record(responseTime, TimeUnit.MILLISECONDS);
    }
}
```

### Webhook Dashboard

Monitor webhook health:
- Delivery success rate
- Average response time
- Failed deliveries
- Retry attempts
- Dead letter queue size

---

## Best Practices

### For Webhook Consumers

✅ **Do**:
- Verify webhook signatures
- Respond quickly (< 30s)
- Process asynchronously
- Implement idempotency
- Log all webhook events

❌ **Don't**:
- Perform long-running operations synchronously
- Ignore signature verification
- Return errors for duplicate events
- Block webhook delivery

### For Webhook Providers

✅ **Do**:
- Include event ID for idempotency
- Implement retry logic
- Provide signature verification
- Document event schemas
- Monitor delivery success

❌ **Don't**:
- Send sensitive data without encryption
- Retry indefinitely
- Change event schemas without versioning
- Ignore delivery failures

---

## Troubleshooting

### Common Issues

**Webhook Not Receiving Events**:
- Check webhook is active
- Verify URL is accessible
- Check firewall rules
- Verify SSL certificate

**Signature Verification Failing**:
- Check secret is correct
- Verify timestamp is recent
- Check payload format
- Ensure proper encoding

**High Failure Rate**:
- Check endpoint availability
- Verify response time < 30s
- Check for errors in logs
- Review retry configuration

---


**Document Version**: 1.0  
**Last Updated**: 2024-11-19  
**Owner**: API Team
