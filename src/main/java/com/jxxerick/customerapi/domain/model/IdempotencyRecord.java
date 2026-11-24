package com.jxxerick.customerapi.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Remembers which customer a key created and a fingerprint of the request that did it. */
public record IdempotencyRecord(IdempotencyKey key, String requestHash, UUID customerId, Instant createdAt) {

    public boolean isExpired(Instant now, Duration ttl) {
        return !createdAt.plus(ttl).isAfter(now);
    }

    public boolean matches(String otherHash) {
        return requestHash.equals(otherHash);
    }
}
