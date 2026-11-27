package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.ListCustomersQuery;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.port.CustomerRepository;

public class ListCustomersUseCase {

    private final CustomerRepository customerRepository;

    public ListCustomersUseCase(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public PageResult<Customer> execute(ListCustomersQuery query) {
        return customerRepository.findMatching(query.filter(), query.page(), query.size());
    }
}
