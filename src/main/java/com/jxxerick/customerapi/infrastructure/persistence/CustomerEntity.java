package com.jxxerick.customerapi.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
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

    // eager is fine: a customer has at most a handful of tags
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "customer_tags", joinColumns = @JoinColumn(name = "customer_id"),
            indexes = @Index(name = "idx_customer_tags_tag", columnList = "tag"))
    @Column(name = "tag", nullable = false, length = 30)
    @BatchSize(size = 100)
    private Set<String> tags = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    protected CustomerEntity() {
    }

    @SuppressWarnings("java:S107")
    CustomerEntity(UUID id, String name, String email, String cpf, String zipCode, String street, String number,
                   String complement, String neighborhood, String city, String state, Set<String> tags,
                   Instant createdAt, Long version) {
        this.tags = new HashSet<>(tags);
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

    Set<String> getTags() {
        return Set.copyOf(tags);
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Long getVersion() {
        return version;
    }
}
