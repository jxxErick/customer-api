package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.CustomerChange;
import com.jxxerick.customerapi.domain.model.TrashedCustomer;
import com.jxxerick.customerapi.domain.port.CustomerHistory;
import com.jxxerick.customerapi.domain.port.CustomerRepository;
import com.jxxerick.customerapi.domain.port.CustomerTrash;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Removes the customer and keeps a copy in the trash for {@code retention}. */
public class DeleteCustomerUseCase {

    private final GetCustomerUseCase getCustomer;
    private final CustomerRepository customerRepository;
    private final CustomerTrash trash;
    private final CustomerHistory history;
    private final Clock clock;
    private final Duration retention;

    public DeleteCustomerUseCase(GetCustomerUseCase getCustomer, CustomerRepository customerRepository,
                                 CustomerTrash trash, CustomerHistory history, Clock clock, Duration retention) {
        this.getCustomer = getCustomer;
        this.customerRepository = customerRepository;
        this.trash = trash;
        this.history = history;
        this.clock = clock;
        this.retention = retention;
    }

    public void execute(UUID id) {
        Customer customer = getCustomer.execute(id);
        Instant now = clock.instant();
        customerRepository.delete(customer);
        trash.put(TrashedCustomer.of(customer, now, retention));
        history.record(CustomerChange.deleted(customer, now));
    }
}
