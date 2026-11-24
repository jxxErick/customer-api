package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.domain.exception.CustomerNotFoundException;
import com.jxxerick.customerapi.domain.model.Cpf;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.port.CustomerRepository;

public class FindCustomerByCpfUseCase {

    private final CustomerRepository customerRepository;

    public FindCustomerByCpfUseCase(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer execute(String rawCpf) {
        Cpf cpf = new Cpf(rawCpf);
        return customerRepository.findByCpf(cpf).orElseThrow(() -> CustomerNotFoundException.withCpf(cpf));
    }
}
