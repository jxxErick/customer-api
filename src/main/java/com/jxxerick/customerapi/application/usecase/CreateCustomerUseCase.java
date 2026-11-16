package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.CreateCustomerCommand;
import com.jxxerick.customerapi.domain.exception.EmailAlreadyInUseException;
import com.jxxerick.customerapi.domain.model.Address;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.Email;
import com.jxxerick.customerapi.domain.port.CustomerRepository;

import java.time.Clock;

public class CreateCustomerUseCase {

    private final CustomerRepository customerRepository;
    private final AddressResolver addressResolver;
    private final Clock clock;

    public CreateCustomerUseCase(CustomerRepository customerRepository, AddressResolver addressResolver, Clock clock) {
        this.customerRepository = customerRepository;
        this.addressResolver = addressResolver;
        this.clock = clock;
    }

    public Customer execute(CreateCustomerCommand command) {
        Email email = new Email(command.email());
        if (customerRepository.existsByEmail(email)) {
            throw new EmailAlreadyInUseException(email.value());
        }
        Address address = addressResolver.resolve(command.zipCode(), command.number(), command.complement());
        Customer customer = Customer.create(command.name(), email, address, clock.instant());
        return customerRepository.save(customer);
    }
}
