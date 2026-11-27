package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.domain.model.TagCount;
import com.jxxerick.customerapi.domain.port.CustomerRepository;

import java.util.List;

/** Every tag in use with how many customers carry it, e.g. for autocomplete. */
public class ListTagsUseCase {

    private final CustomerRepository customerRepository;

    public ListTagsUseCase(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<TagCount> execute() {
        return customerRepository.countByTag();
    }
}
