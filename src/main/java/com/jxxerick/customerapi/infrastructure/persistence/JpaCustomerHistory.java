package com.jxxerick.customerapi.infrastructure.persistence;

import com.jxxerick.customerapi.domain.model.CustomerChange;
import com.jxxerick.customerapi.domain.model.FieldChange;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.port.CustomerHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Adapter from Spring Data JPA to the {@link CustomerHistory} port. */
@Repository
class JpaCustomerHistory implements CustomerHistory {

    // id breaks ties between changes recorded in the same instant
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"));

    private final SpringDataCustomerChangeRepository jpa;

    JpaCustomerHistory(SpringDataCustomerChangeRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void record(CustomerChange change) {
        jpa.save(new CustomerChangeEntity(change.customerId(), change.type(), change.occurredAt(),
                change.fields().stream()
                        .map(f -> new FieldChangeEmbeddable(f.field(), f.previous(), f.current()))
                        .toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CustomerChange> findByCustomer(UUID customerId, int page, int size) {
        Page<CustomerChangeEntity> result = jpa.findByCustomerId(customerId, PageRequest.of(page, size, NEWEST_FIRST));
        return new PageResult<>(result.map(JpaCustomerHistory::toDomain).getContent(), page, size,
                result.getTotalElements());
    }

    private static CustomerChange toDomain(CustomerChangeEntity entity) {
        return new CustomerChange(entity.getCustomerId(), entity.getType(),
                entity.getFields().stream()
                        .map(f -> new FieldChange(f.getField(), f.getPrevious(), f.getCurrent()))
                        .toList(),
                entity.getOccurredAt());
    }
}
