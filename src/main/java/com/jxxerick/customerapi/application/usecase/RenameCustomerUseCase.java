package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.RenameCustomerCommand;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.port.CustomerRepository;

public class RenameCustomerUseCase {

    private final GetCustomerUseCase getCustomer;
    private final CustomerRepository customerRepository;

    public RenameCustomerUseCase(GetCustomerUseCase getCustomer, CustomerRepository customerRepository) {
        this.getCustomer = getCustomer;
        this.customerRepository = customerRepository;
    }

    public Customer execute(RenameCustomerCommand command) {
        Customer customer = getCustomer.execute(command.customerId());
        customer.ensureVersion(command.expectedVersion());
        customer.rename(command.name().strip());
        return customerRepository.save(customer);
    }
}
