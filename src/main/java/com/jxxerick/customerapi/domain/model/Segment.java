package com.jxxerick.customerapi.domain.model;

import com.jxxerick.customerapi.domain.exception.DomainValidationException;

import java.time.Instant;
import java.util.UUID;

/**
 * A saved, named filter such as "VIPs de São Paulo". It stores the criteria, not the customers,
 * so it always reflects the current base.
 */
public record Segment(UUID id, String name, CustomerFilter filter, Instant createdAt) {

    public static final int MAX_NAME_LENGTH = 60;

    public Segment {
        Validations.requireNonNull(id, "Id é obrigatório");
        name = Validations.requireText(name, "Nome do segmento é obrigatório");
        if (name.length() > MAX_NAME_LENGTH) {
            throw new DomainValidationException(
                    "O nome do segmento pode ter no máximo " + MAX_NAME_LENGTH + " caracteres");
        }
        Validations.requireNonNull(filter, "Critérios são obrigatórios");
        if (filter.isEmpty()) {
            throw new DomainValidationException("Informe ao menos um critério para o segmento");
        }
        Validations.requireNonNull(createdAt, "Data de criação é obrigatória");
    }

    public static Segment create(String name, CustomerFilter filter, Instant now) {
        return new Segment(UUID.randomUUID(), name, filter, now);
    }
}
