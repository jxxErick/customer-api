package com.jxxerick.customerapi.domain.port;

import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.Email;
import com.jxxerick.customerapi.domain.model.PageResult;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(UUID id);

    /** Customers ordered by name; a null {@code nameFragment} matches everyone. */
    PageResult<Customer> search(String nameFragment, int page, int size);

    boolean existsByEmail(Email email);

    void delete(Customer customer);
}
