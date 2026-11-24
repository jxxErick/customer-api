package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.RenameCustomerCommand;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.CustomerChange;
import com.jxxerick.customerapi.domain.port.CustomerHistory;
import com.jxxerick.customerapi.domain.port.CustomerRepository;

import java.time.Clock;

public class RenameCustomerUseCase {

    private final GetCustomerUseCase getCustomer;
    private final CustomerRepository customerRepository;
    private final CustomerHistory history;
    private final Clock clock;

    public RenameCustomerUseCase(GetCustomerUseCase getCustomer, CustomerRepository customerRepository,
                                 CustomerHistory history, Clock clock) {
        this.getCustomer = getCustomer;
        this.customerRepository = customerRepository;
        this.history = history;
        this.clock = clock;
    }

    /** Renaming to the same name is accepted but leaves no history entry. */
    public Customer execute(RenameCustomerCommand command) {
        Customer customer = getCustomer.execute(command.customerId());
        customer.ensureVersion(command.expectedVersion());
        String previousName = customer.getName();
        customer.rename(command.name().strip());
        Customer saved = customerRepository.save(customer);
        if (!previousName.equals(saved.getName())) {
            history.record(CustomerChange.renamed(saved, previousName, clock.instant()));
        }
        return saved;
    }
}
