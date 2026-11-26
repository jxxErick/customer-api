package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.domain.exception.CpfAlreadyInUseException;
import com.jxxerick.customerapi.domain.exception.EmailAlreadyInUseException;
import com.jxxerick.customerapi.domain.exception.TrashedCustomerNotFoundException;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.CustomerChange;
import com.jxxerick.customerapi.domain.model.TrashedCustomer;
import com.jxxerick.customerapi.domain.port.CustomerHistory;
import com.jxxerick.customerapi.domain.port.CustomerRepository;
import com.jxxerick.customerapi.domain.port.CustomerTrash;

import java.time.Clock;
import java.util.UUID;

/**
 * Brings a customer back from the trash with the same id and data. Someone may have taken the
 * e-mail or CPF in the meantime, and then the restore is refused instead of creating a duplicate.
 */
public class RestoreCustomerUseCase {

    private final CustomerTrash trash;
    private final CustomerRepository customerRepository;
    private final CustomerHistory history;
    private final Clock clock;

    public RestoreCustomerUseCase(CustomerTrash trash, CustomerRepository customerRepository,
                                  CustomerHistory history, Clock clock) {
        this.trash = trash;
        this.customerRepository = customerRepository;
        this.history = history;
        this.clock = clock;
    }

    public Customer execute(UUID id) {
        TrashedCustomer trashed = trash.find(id).orElseThrow(() -> new TrashedCustomerNotFoundException(id));
        Customer customer = trashed.toRestoredCustomer();
        if (customerRepository.existsByEmail(customer.getEmail())) {
            throw new EmailAlreadyInUseException(customer.getEmail().value());
        }
        customer.getCpf().filter(customerRepository::existsByCpf).ifPresent(cpf -> {
            throw new CpfAlreadyInUseException(cpf);
        });

        Customer saved = customerRepository.save(customer);
        trash.remove(id);
        history.record(CustomerChange.restored(saved, clock.instant()));
        return saved;
    }
}
