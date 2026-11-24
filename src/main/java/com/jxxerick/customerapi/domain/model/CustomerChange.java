package com.jxxerick.customerapi.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Audit entry of something that happened to a customer; kept even after deletion. */
public record CustomerChange(UUID customerId, ChangeType type, List<FieldChange> fields, Instant occurredAt) {

    public CustomerChange {
        Validations.requireNonNull(customerId, "Id do cliente é obrigatório");
        Validations.requireNonNull(type, "Tipo da alteração é obrigatório");
        Validations.requireNonNull(occurredAt, "Data da alteração é obrigatória");
        fields = fields == null ? List.of() : List.copyOf(fields);
    }

    public static CustomerChange created(Customer customer, Instant now) {
        return new CustomerChange(customer.getId(), ChangeType.CREATED, List.of(
                new FieldChange("name", null, customer.getName()),
                new FieldChange("email", null, customer.getEmail().value()),
                new FieldChange("address", null, customer.getAddress().summary())), now);
    }

    public static CustomerChange renamed(Customer customer, String previousName, Instant now) {
        return new CustomerChange(customer.getId(), ChangeType.RENAMED,
                List.of(new FieldChange("name", previousName, customer.getName())), now);
    }

    public static CustomerChange moved(Customer customer, Address previousAddress, Instant now) {
        return new CustomerChange(customer.getId(), ChangeType.ADDRESS_CHANGED,
                List.of(new FieldChange("address", previousAddress.summary(), customer.getAddress().summary())), now);
    }

    public static CustomerChange deleted(Customer customer, Instant now) {
        return new CustomerChange(customer.getId(), ChangeType.DELETED, List.of(), now);
    }
}
