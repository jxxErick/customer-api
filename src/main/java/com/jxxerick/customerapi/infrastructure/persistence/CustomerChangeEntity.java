package com.jxxerick.customerapi.infrastructure.persistence;

import com.jxxerick.customerapi.domain.model.ChangeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** No foreign key to customers on purpose: the history outlives the customer. */
@Entity
@Table(name = "customer_changes", indexes = @Index(name = "idx_customer_changes_customer", columnList = "customer_id"))
public class CustomerChangeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChangeType type;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @ElementCollection
    @CollectionTable(name = "customer_change_fields", joinColumns = @JoinColumn(name = "change_id"))
    @OrderColumn(name = "field_order")
    private List<FieldChangeEmbeddable> fields = new ArrayList<>();

    protected CustomerChangeEntity() {
    }

    CustomerChangeEntity(UUID customerId, ChangeType type, Instant occurredAt, List<FieldChangeEmbeddable> fields) {
        this.customerId = customerId;
        this.type = type;
        this.occurredAt = occurredAt;
        this.fields = new ArrayList<>(fields);
    }

    UUID getCustomerId() {
        return customerId;
    }

    ChangeType getType() {
        return type;
    }

    Instant getOccurredAt() {
        return occurredAt;
    }

    List<FieldChangeEmbeddable> getFields() {
        return fields;
    }
}
