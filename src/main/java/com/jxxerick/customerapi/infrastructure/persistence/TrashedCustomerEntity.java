package com.jxxerick.customerapi.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Snapshot of a deleted customer; e-mail and CPF are not unique here, the live table owns them. */
@Entity
@Table(name = "customer_trash", indexes = @Index(name = "idx_customer_trash_purge_at", columnList = "purge_at"))
public class TrashedCustomerEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(length = 11)
    private String cpf;

    @Column(name = "zip_code", nullable = false, length = 8)
    private String zipCode;

    private String street;

    @Column(nullable = false)
    private String number;

    private String complement;

    private String neighborhood;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false, length = 2)
    private String state;

    // comma-separated; a tag never contains a comma
    @Column(length = 400)
    private String tags;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at", nullable = false)
    private Instant deletedAt;

    @Column(name = "purge_at", nullable = false)
    private Instant purgeAt;

    protected TrashedCustomerEntity() {
    }

    TrashedCustomerEntity(CustomerEntity customer, Instant deletedAt, Instant purgeAt) {
        this.id = customer.getId();
        this.name = customer.getName();
        this.email = customer.getEmail();
        this.cpf = customer.getCpf();
        this.zipCode = customer.getZipCode();
        this.street = customer.getStreet();
        this.number = customer.getNumber();
        this.complement = customer.getComplement();
        this.neighborhood = customer.getNeighborhood();
        this.city = customer.getCity();
        this.state = customer.getState();
        this.tags = customer.getTags().isEmpty() ? null : String.join(",", new TreeSet<>(customer.getTags()));
        this.createdAt = customer.getCreatedAt();
        this.deletedAt = deletedAt;
        this.purgeAt = purgeAt;
    }

    /** Rebuilds the live-table shape so the regular mapper can turn it into a domain customer. */
    CustomerEntity toCustomerEntity() {
        return new CustomerEntity(id, name, email, cpf, zipCode, street, number, complement, neighborhood, city,
                state, tags == null ? Set.of() : Set.of(tags.split(",")), createdAt, null);
    }

    UUID getId() {
        return id;
    }

    Instant getDeletedAt() {
        return deletedAt;
    }

    Instant getPurgeAt() {
        return purgeAt;
    }
}
