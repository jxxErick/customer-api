package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.MergeCustomersCommand;
import com.jxxerick.customerapi.domain.exception.CustomerMergeConflictException;
import com.jxxerick.customerapi.domain.exception.DomainValidationException;
import com.jxxerick.customerapi.domain.model.Cpf;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.CustomerChange;
import com.jxxerick.customerapi.domain.port.CustomerHistory;
import com.jxxerick.customerapi.domain.port.CustomerRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Keeps the target customer and removes the duplicate. The target keeps its own data and only
 * inherits the duplicate's CPF when it has none; two different CPFs mean two different people.
 */
public class MergeCustomersUseCase {

    private final GetCustomerUseCase getCustomer;
    private final CustomerRepository customerRepository;
    private final CustomerHistory history;
    private final Clock clock;

    public MergeCustomersUseCase(GetCustomerUseCase getCustomer, CustomerRepository customerRepository,
                                 CustomerHistory history, Clock clock) {
        this.getCustomer = getCustomer;
        this.customerRepository = customerRepository;
        this.history = history;
        this.clock = clock;
    }

    public Customer execute(MergeCustomersCommand command) {
        if (command.targetId().equals(command.duplicateId())) {
            throw new DomainValidationException("Um cliente não pode ser mesclado com ele mesmo");
        }
        Customer target = getCustomer.execute(command.targetId());
        target.ensureVersion(command.expectedVersion());
        Customer duplicate = getCustomer.execute(command.duplicateId());

        Optional<Cpf> inheritedCpf = inheritedCpf(target, duplicate);
        // the duplicate goes first, so its CPF is free before the target takes it
        customerRepository.delete(duplicate);
        inheritedCpf.ifPresent(target::assignCpf);
        Customer saved = customerRepository.save(target);

        Instant now = clock.instant();
        history.record(CustomerChange.mergedInto(duplicate, saved, now));
        history.record(CustomerChange.merged(saved, duplicate, inheritedCpf.orElse(null), now));
        return saved;
    }

    private static Optional<Cpf> inheritedCpf(Customer target, Customer duplicate) {
        Optional<Cpf> targetCpf = target.getCpf();
        Optional<Cpf> duplicateCpf = duplicate.getCpf();
        if (targetCpf.isPresent() && duplicateCpf.isPresent() && !targetCpf.equals(duplicateCpf)) {
            throw new CustomerMergeConflictException("Os clientes têm CPFs diferentes e não podem ser mesclados");
        }
        return targetCpf.isPresent() ? Optional.empty() : duplicateCpf;
    }
}
