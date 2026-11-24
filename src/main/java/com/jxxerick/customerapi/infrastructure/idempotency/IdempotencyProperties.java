package com.jxxerick.customerapi.infrastructure.idempotency;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** {@code ttl}: how long a key is remembered; {@code cleanupInterval}: how often expired keys are purged. */
@ConfigurationProperties(prefix = "idempotency")
public record IdempotencyProperties(Duration ttl, Duration cleanupInterval) {
}
