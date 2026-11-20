package com.jxxerick.customerapi.domain.model;

import com.jxxerick.customerapi.domain.exception.ConcurrentUpdateException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Customer {

    private final UUID id;
    private final Email email;
    private final Instant createdAt;
    private final Long version;
    private String name;
    private Address address;

    private Customer(UUID id, String name, Email email, Address address, Instant createdAt, Long version) {
        this.id = Validations.requireNonNull(id, "Id é obrigatório");
        this.name = Validations.requireText(name, "Nome é obrigatório");
        this.email = Validations.requireNonNull(email, "E-mail é obrigatório");
        this.address = Validations.requireNonNull(address, "Endereço é obrigatório");
        this.createdAt = Validations.requireNonNull(createdAt, "Data de criação é obrigatória");
        this.version = version;
    }

    /** Factory for brand-new customers. */
    public static Customer create(String name, Email email, Address address, Instant now) {
        return new Customer(UUID.randomUUID(), name, email, address, now, null);
    }

    /** Factory for rebuilding an existing customer from storage. */
    public static Customer restore(UUID id, String name, Email email, Address address, Instant createdAt,
                                   Long version) {
        return new Customer(id, name, email, address, createdAt, version);
    }

    /** Rejects changes made over an outdated copy; a null expectation skips the check. */
    public void ensureVersion(Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(version)) {
            throw new ConcurrentUpdateException(id);
        }
    }

    public void rename(String newName) {
        this.name = Validations.requireText(newName, "Nome é obrigatório");
    }

    public void moveTo(Address newAddress) {
        this.address = Validations.requireNonNull(newAddress, "Endereço é obrigatório");
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Email getEmail() {
        return email;
    }

    public Address getAddress() {
        return address;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Null until the customer is first stored. */
    public Long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Customer customer && id.equals(customer.id));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
