package com.jxxerick.customerapi.infrastructure.persistence;

import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.Email;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.port.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adapter from Spring Data JPA to the {@link CustomerRepository} port. */
@Repository
class JpaCustomerRepository implements CustomerRepository {

    private final SpringDataCustomerRepository jpa;

    JpaCustomerRepository(SpringDataCustomerRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Customer save(Customer customer) {
        return CustomerEntityMapper.toDomain(jpa.save(CustomerEntityMapper.toEntity(customer)));
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return jpa.findById(id).map(CustomerEntityMapper::toDomain);
    }

    @Override
    public PageResult<Customer> search(String nameFragment, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("name", "id"));
        Page<CustomerEntity> result = nameFragment == null
                ? jpa.findAll(pageable)
                : jpa.findByNameContainingIgnoreCase(nameFragment, pageable);
        return new PageResult<>(result.map(CustomerEntityMapper::toDomain).getContent(), page, size,
                result.getTotalElements());
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    public void delete(Customer customer) {
        jpa.deleteById(customer.getId());
    }
}
