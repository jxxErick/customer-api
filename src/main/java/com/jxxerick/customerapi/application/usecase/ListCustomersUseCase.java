package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.port.CustomerRepository;

import java.util.List;

public class ListCustomersUseCase {

    private final CustomerRepository customerRepository;

    public ListCustomersUseCase(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> execute() {
        return customerRepository.findAll();
    }
}
