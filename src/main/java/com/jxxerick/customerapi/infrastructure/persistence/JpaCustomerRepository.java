package com.jxxerick.customerapi.infrastructure.persistence;

import com.jxxerick.customerapi.domain.exception.ConcurrentUpdateException;
import com.jxxerick.customerapi.domain.model.Cpf;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.Email;
import com.jxxerick.customerapi.domain.model.LocationCount;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.model.Tag;
import com.jxxerick.customerapi.domain.model.TagCount;
import com.jxxerick.customerapi.domain.port.CustomerRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Adapter from Spring Data JPA to the {@link CustomerRepository} port. */
@Repository
class JpaCustomerRepository implements CustomerRepository {

    private final SpringDataCustomerRepository jpa;

    JpaCustomerRepository(SpringDataCustomerRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Customer save(Customer customer) {
        try {
            return CustomerEntityMapper.toDomain(jpa.save(CustomerEntityMapper.toEntity(customer)));
        } catch (OptimisticLockingFailureException exception) {
            throw new ConcurrentUpdateException(customer.getId());
        }
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return jpa.findById(id).map(CustomerEntityMapper::toDomain);
    }

    @Override
    public PageResult<Customer> search(String nameFragment, Set<Tag> tags, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("name", "id"));
        Set<String> tagValues = tags.stream().map(Tag::value).collect(Collectors.toSet());
        Page<CustomerEntity> result = jpa.findAll(
                CustomerSpecifications.nameContains(nameFragment).and(CustomerSpecifications.hasAllTags(tagValues)),
                pageable);
        return new PageResult<>(result.map(CustomerEntityMapper::toDomain).getContent(), page, size,
                result.getTotalElements());
    }

    @Override
    public List<LocationCount> countByLocation() {
        return jpa.countByLocation().stream()
                .map(row -> new LocationCount(row.getState(), row.getCity(), row.getCustomers()))
                .toList();
    }

    @Override
    public List<TagCount> countByTag() {
        return jpa.countByTag().stream().map(row -> new TagCount(row.getTag(), row.getCustomers())).toList();
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    public boolean existsByCpf(Cpf cpf) {
        return jpa.existsByCpf(cpf.value());
    }

    @Override
    public Optional<Customer> findByCpf(Cpf cpf) {
        return jpa.findByCpf(cpf.value()).map(CustomerEntityMapper::toDomain);
    }

    @Override
    public void delete(Customer customer) {
        jpa.deleteById(customer.getId());
    }
}
