package com.jxxerick.customerapi.domain.port;

import com.jxxerick.customerapi.domain.model.Cpf;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.Email;
import com.jxxerick.customerapi.domain.model.LocationCount;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.model.Tag;
import com.jxxerick.customerapi.domain.model.TagCount;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(UUID id);

    /**
     * Customers ordered by name; a null {@code nameFragment} matches everyone and every tag in
     * {@code tags} must be present on the customer (an empty set does not filter).
     */
    PageResult<Customer> search(String nameFragment, Set<Tag> tags, int page, int size);

    default PageResult<Customer> search(String nameFragment, int page, int size) {
        return search(nameFragment, Set.of(), page, size);
    }

    /** Customer count per state and city. */
    List<LocationCount> countByLocation();

    /** Customer count per tag, most used first. */
    List<TagCount> countByTag();

    boolean existsByEmail(Email email);

    boolean existsByCpf(Cpf cpf);

    Optional<Customer> findByCpf(Cpf cpf);

    void delete(Customer customer);
}
