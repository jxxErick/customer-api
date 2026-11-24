package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.port.CustomerRepository;

import java.util.function.Consumer;

/** Walks every matching customer page by page, so the export never holds the whole table. */
public class ExportCustomersUseCase {

    static final int PAGE_SIZE = 500;

    private final CustomerRepository customerRepository;

    public ExportCustomersUseCase(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /** Sends each customer, ordered by name, to {@code sink}; a blank filter exports everyone. */
    public long execute(String nameFilter, Consumer<Customer> sink) {
        String name = nameFilter == null || nameFilter.isBlank() ? null : nameFilter.strip();
        long exported = 0;
        int page = 0;
        PageResult<Customer> result;
        do {
            result = customerRepository.search(name, page++, PAGE_SIZE);
            result.items().forEach(sink);
            exported += result.items().size();
        } while (page < result.totalPages());
        return exported;
    }
}
