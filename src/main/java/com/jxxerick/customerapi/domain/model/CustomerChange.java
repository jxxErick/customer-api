package com.jxxerick.customerapi.domain.model;

import java.time.Instant;
import java.util.ArrayList;
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

    public static CustomerChange restored(Customer customer, Instant now) {
        return new CustomerChange(customer.getId(), ChangeType.RESTORED, List.of(), now);
    }

    /** Entry on the customer that absorbed a duplicate; {@code inheritedCpf} may be null. */
    public static CustomerChange merged(Customer target, Customer duplicate, Cpf inheritedCpf, Instant now) {
        List<FieldChange> fields = new ArrayList<>();
        fields.add(new FieldChange("mergedFrom", null, duplicate.getId().toString()));
        if (inheritedCpf != null) {
            fields.add(new FieldChange("cpf", null, inheritedCpf.masked()));
        }
        return new CustomerChange(target.getId(), ChangeType.MERGED, fields, now);
    }

    /** Entry on the duplicate that was removed by a merge. */
    public static CustomerChange mergedInto(Customer duplicate, Customer target, Instant now) {
        return new CustomerChange(duplicate.getId(), ChangeType.MERGED_INTO,
                List.of(new FieldChange("mergedInto", null, target.getId().toString())), now);
    }
}
