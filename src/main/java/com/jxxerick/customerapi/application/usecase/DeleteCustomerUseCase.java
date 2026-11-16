package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.domain.port.CustomerRepository;

import java.util.UUID;

public class DeleteCustomerUseCase {

    private final GetCustomerUseCase getCustomer;
    private final CustomerRepository customerRepository;

    public DeleteCustomerUseCase(GetCustomerUseCase getCustomer, CustomerRepository customerRepository) {
        this.getCustomer = getCustomer;
        this.customerRepository = customerRepository;
    }

    public void execute(UUID id) {
        customerRepository.delete(getCustomer.execute(id));
    }
}
