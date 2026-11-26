package com.jxxerick.customerapi.domain.model;

import com.jxxerick.customerapi.domain.exception.ConcurrentUpdateException;
import com.jxxerick.customerapi.domain.exception.DomainValidationException;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

public class Customer {

    public static final int MAX_TAGS = 10;

    private final UUID id;
    private final Email email;
    private Cpf cpf;
    private final Instant createdAt;
    private final Long version;
    private String name;
    private Address address;
    private SortedSet<Tag> tags;

    private Customer(UUID id, String name, Email email, Cpf cpf, Address address, Collection<Tag> tags,
                     Instant createdAt, Long version) {
        this.id = Validations.requireNonNull(id, "Id é obrigatório");
        this.name = Validations.requireText(name, "Nome é obrigatório");
        this.email = Validations.requireNonNull(email, "E-mail é obrigatório");
        this.cpf = cpf;
        this.address = Validations.requireNonNull(address, "Endereço é obrigatório");
        this.tags = validTags(tags == null ? Set.of() : tags);
        this.createdAt = Validations.requireNonNull(createdAt, "Data de criação é obrigatória");
        this.version = version;
    }

    /** Factory for brand-new customers; the CPF is optional. */
    public static Customer create(String name, Email email, Cpf cpf, Address address, Instant now) {
        return new Customer(UUID.randomUUID(), name, email, cpf, address, Set.of(), now, null);
    }

    public static Customer create(String name, Email email, Address address, Instant now) {
        return create(name, email, null, address, now);
    }

    /** Factory for rebuilding an existing customer from storage. */
    public static Customer restore(UUID id, String name, Email email, Cpf cpf, Address address,
                                   Collection<Tag> tags, Instant createdAt, Long version) {
        return new Customer(id, name, email, cpf, address, tags, createdAt, version);
    }

    public static Customer restore(UUID id, String name, Email email, Cpf cpf, Address address,
                                   Instant createdAt, Long version) {
        return restore(id, name, email, cpf, address, Set.of(), createdAt, version);
    }

    /** Replaces every tag; duplicates collapse after normalization. */
    public void retag(Collection<Tag> newTags) {
        this.tags = validTags(Validations.requireNonNull(newTags, "Tags são obrigatórias"));
    }

    private static SortedSet<Tag> validTags(Collection<Tag> tags) {
        SortedSet<Tag> sorted = new TreeSet<>(tags);
        if (sorted.size() > MAX_TAGS) {
            throw new DomainValidationException("Um cliente pode ter no máximo " + MAX_TAGS + " tags");
        }
        return sorted;
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

    /** A CPF is set once and never replaced. */
    public void assignCpf(Cpf newCpf) {
        Validations.requireNonNull(newCpf, "CPF é obrigatório");
        if (cpf != null) {
            throw new DomainValidationException("Cliente já possui CPF");
        }
        this.cpf = newCpf;
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

    /** Empty when the customer was registered without a CPF. */
    public Optional<Cpf> getCpf() {
        return Optional.ofNullable(cpf);
    }

    public Address getAddress() {
        return address;
    }

    /** Alphabetical, never null. */
    public SortedSet<Tag> getTags() {
        return Collections.unmodifiableSortedSet(tags);
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
