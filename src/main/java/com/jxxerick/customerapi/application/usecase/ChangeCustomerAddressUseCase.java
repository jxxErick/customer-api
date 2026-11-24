package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.ChangeAddressCommand;
import com.jxxerick.customerapi.domain.model.Address;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.CustomerChange;
import com.jxxerick.customerapi.domain.port.CustomerHistory;
import com.jxxerick.customerapi.domain.port.CustomerRepository;

import java.time.Clock;

public class ChangeCustomerAddressUseCase {

    private final GetCustomerUseCase getCustomer;
    private final CustomerRepository customerRepository;
    private final AddressResolver addressResolver;
    private final CustomerHistory history;
    private final Clock clock;

    public ChangeCustomerAddressUseCase(GetCustomerUseCase getCustomer,
                                        CustomerRepository customerRepository,
                                        AddressResolver addressResolver,
                                        CustomerHistory history,
                                        Clock clock) {
        this.getCustomer = getCustomer;
        this.customerRepository = customerRepository;
        this.addressResolver = addressResolver;
        this.history = history;
        this.clock = clock;
    }

    /** Moving to the very same address is accepted but leaves no history entry. */
    public Customer execute(ChangeAddressCommand command) {
        Customer customer = getCustomer.execute(command.customerId());
        customer.ensureVersion(command.expectedVersion());
        Address previousAddress = customer.getAddress();
        customer.moveTo(addressResolver.resolve(command.zipCode(), command.number(), command.complement()));
        Customer saved = customerRepository.save(customer);
        if (!previousAddress.equals(saved.getAddress())) {
            history.record(CustomerChange.moved(saved, previousAddress, clock.instant()));
        }
        return saved;
    }
}
