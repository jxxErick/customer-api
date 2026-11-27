package com.jxxerick.customerapi.infrastructure.persistence;

import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.model.TrashedCustomer;
import com.jxxerick.customerapi.domain.port.CustomerTrash;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Adapter from Spring Data JPA to the {@link CustomerTrash} port. */
@Repository
class JpaCustomerTrash implements CustomerTrash {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("deletedAt"), Sort.Order.asc("id"));

    private final SpringDataTrashedCustomerRepository jpa;

    JpaCustomerTrash(SpringDataTrashedCustomerRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void put(TrashedCustomer trashed) {
        jpa.save(new TrashedCustomerEntity(CustomerEntityMapper.toEntity(trashed.customer()), trashed.deletedAt(),
                trashed.purgeAt()));
    }

    @Override
    public Optional<TrashedCustomer> find(UUID customerId) {
        return jpa.findById(customerId).map(JpaCustomerTrash::toDomain);
    }

    @Override
    public PageResult<TrashedCustomer> list(int page, int size) {
        Page<TrashedCustomerEntity> result = jpa.findAll(PageRequest.of(page, size, NEWEST_FIRST));
        return new PageResult<>(result.map(JpaCustomerTrash::toDomain).getContent(), page, size,
                result.getTotalElements());
    }

    @Override
    public void remove(UUID customerId) {
        jpa.deleteById(customerId);
    }

    @Override
    @Transactional
    public int purgeDue(Instant now) {
        return jpa.deletePurgeDue(now);
    }

    private static TrashedCustomer toDomain(TrashedCustomerEntity entity) {
        return new TrashedCustomer(CustomerEntityMapper.toDomain(entity.toCustomerEntity()), entity.getDeletedAt(),
                entity.getPurgeAt());
    }
}
