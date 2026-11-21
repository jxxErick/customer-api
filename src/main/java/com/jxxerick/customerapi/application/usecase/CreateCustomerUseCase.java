package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.CreateCustomerCommand;
import com.jxxerick.customerapi.domain.exception.CpfAlreadyInUseException;
import com.jxxerick.customerapi.domain.exception.EmailAlreadyInUseException;
import com.jxxerick.customerapi.domain.model.Address;
import com.jxxerick.customerapi.domain.model.Cpf;
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

    /** Cheap local checks run before the zip code lookup, which may hit an external service. */
    public Customer execute(CreateCustomerCommand command) {
        Email email = new Email(command.email());
        Cpf cpf = command.cpf() == null || command.cpf().isBlank() ? null : new Cpf(command.cpf());
        if (customerRepository.existsByEmail(email)) {
            throw new EmailAlreadyInUseException(email.value());
        }
        if (cpf != null && customerRepository.existsByCpf(cpf)) {
            throw new CpfAlreadyInUseException(cpf);
        }
        Address address = addressResolver.resolve(command.zipCode(), command.number(), command.complement());
        Customer customer = Customer.create(command.name(), email, cpf, address, clock.instant());
        return customerRepository.save(customer);
    }
}
