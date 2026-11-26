package com.jxxerick.customerapi.domain.model;

import java.time.Duration;
import java.time.Instant;

/** A deleted customer kept for a while so it can be restored; purged for good at {@code purgeAt}. */
public record TrashedCustomer(Customer customer, Instant deletedAt, Instant purgeAt) {

    public TrashedCustomer {
        Validations.requireNonNull(customer, "Cliente é obrigatório");
        Validations.requireNonNull(deletedAt, "Data da exclusão é obrigatória");
        Validations.requireNonNull(purgeAt, "Data do expurgo é obrigatória");
    }

    public static TrashedCustomer of(Customer customer, Instant deletedAt, Duration retention) {
        return new TrashedCustomer(customer, deletedAt, deletedAt.plus(retention));
    }

    /** The customer as a brand-new row: same id and data, version reset. */
    public Customer toRestoredCustomer() {
        return Customer.restore(customer.getId(), customer.getName(), customer.getEmail(),
                customer.getCpf().orElse(null), customer.getAddress(), customer.getCreatedAt(), null);
    }
}
