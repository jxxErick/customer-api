package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.CustomerChange;
import com.jxxerick.customerapi.domain.port.CustomerHistory;
import com.jxxerick.customerapi.domain.port.CustomerRepository;

import java.time.Clock;
import java.util.UUID;

public class DeleteCustomerUseCase {

    private final GetCustomerUseCase getCustomer;
    private final CustomerRepository customerRepository;
    private final CustomerHistory history;
    private final Clock clock;

    public DeleteCustomerUseCase(GetCustomerUseCase getCustomer, CustomerRepository customerRepository,
                                 CustomerHistory history, Clock clock) {
        this.getCustomer = getCustomer;
        this.customerRepository = customerRepository;
        this.history = history;
        this.clock = clock;
    }

    public void execute(UUID id) {
        Customer customer = getCustomer.execute(id);
        customerRepository.delete(customer);
        history.record(CustomerChange.deleted(customer, clock.instant()));
    }
}
