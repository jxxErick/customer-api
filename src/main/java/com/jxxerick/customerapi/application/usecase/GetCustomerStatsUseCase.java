package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.domain.model.CustomerStats;
import com.jxxerick.customerapi.domain.port.CustomerRepository;

public class GetCustomerStatsUseCase {

    private final CustomerRepository customerRepository;

    public GetCustomerStatsUseCase(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerStats execute() {
        return CustomerStats.from(customerRepository.countByLocation());
    }
}
