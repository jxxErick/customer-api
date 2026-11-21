package com.jxxerick.customerapi.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class CustomerEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true, length = 11)
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

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    protected CustomerEntity() {
    }

    @SuppressWarnings("java:S107")
    CustomerEntity(UUID id, String name, String email, String cpf, String zipCode, String street, String number,
                   String complement, String neighborhood, String city, String state, Instant createdAt,
                   Long version) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.cpf = cpf;
        this.zipCode = zipCode;
        this.street = street;
        this.number = number;
        this.complement = complement;
        this.neighborhood = neighborhood;
        this.city = city;
        this.state = state;
        this.createdAt = createdAt;
        this.version = version;
    }

    UUID getId() {
        return id;
    }

    String getName() {
        return name;
    }

    String getEmail() {
        return email;
    }

    String getCpf() {
        return cpf;
    }

    String getZipCode() {
        return zipCode;
    }

    String getStreet() {
        return street;
    }

    String getNumber() {
        return number;
    }

    String getComplement() {
        return complement;
    }

    String getNeighborhood() {
        return neighborhood;
    }

    String getCity() {
        return city;
    }

    String getState() {
        return state;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Long getVersion() {
        return version;
    }
}
