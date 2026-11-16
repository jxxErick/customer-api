package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.ChangeAddressCommand;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.port.CustomerRepository;

public class ChangeCustomerAddressUseCase {

    private final GetCustomerUseCase getCustomer;
    private final CustomerRepository customerRepository;
    private final AddressResolver addressResolver;

    public ChangeCustomerAddressUseCase(GetCustomerUseCase getCustomer,
                                        CustomerRepository customerRepository,
                                        AddressResolver addressResolver) {
        this.getCustomer = getCustomer;
        this.customerRepository = customerRepository;
        this.addressResolver = addressResolver;
    }

    public Customer execute(ChangeAddressCommand command) {
        Customer customer = getCustomer.execute(command.customerId());
        customer.moveTo(addressResolver.resolve(command.zipCode(), command.number(), command.complement()));
        return customerRepository.save(customer);
    }
}
