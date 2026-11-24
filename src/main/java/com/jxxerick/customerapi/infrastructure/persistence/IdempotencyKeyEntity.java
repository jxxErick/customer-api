package com.jxxerick.customerapi.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKeyEntity {

    @Id
    @Column(name = "idempotency_key", length = 64)
    private String key;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyKeyEntity() {
    }

    IdempotencyKeyEntity(String key, String requestHash, UUID customerId, Instant createdAt) {
        this.key = key;
        this.requestHash = requestHash;
        this.customerId = customerId;
        this.createdAt = createdAt;
    }

    String getKey() {
        return key;
    }

    String getRequestHash() {
        return requestHash;
    }

    UUID getCustomerId() {
        return customerId;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
