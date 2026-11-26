package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.TagCustomerCommand;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.CustomerChange;
import com.jxxerick.customerapi.domain.model.Tag;
import com.jxxerick.customerapi.domain.port.CustomerHistory;
import com.jxxerick.customerapi.domain.port.CustomerRepository;

import java.time.Clock;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/** Replaces the customer's tags; an empty list removes them all. */
public class TagCustomerUseCase {

    private final GetCustomerUseCase getCustomer;
    private final CustomerRepository customerRepository;
    private final CustomerHistory history;
    private final Clock clock;

    public TagCustomerUseCase(GetCustomerUseCase getCustomer, CustomerRepository customerRepository,
                              CustomerHistory history, Clock clock) {
        this.getCustomer = getCustomer;
        this.customerRepository = customerRepository;
        this.history = history;
        this.clock = clock;
    }

    /** The same set of tags is accepted but leaves no history entry. */
    public Customer execute(TagCustomerCommand command) {
        // parse before loading, so a bad tag fails without touching the database
        List<Tag> tags = command.tags().stream().map(Tag::new).toList();
        Customer customer = getCustomer.execute(command.customerId());
        customer.ensureVersion(command.expectedVersion());
        SortedSet<Tag> previous = new TreeSet<>(customer.getTags());
        customer.retag(tags);
        Customer saved = customerRepository.save(customer);
        if (!previous.equals(saved.getTags())) {
            history.record(CustomerChange.retagged(saved, previous, clock.instant()));
        }
        return saved;
    }
}
