package com.jxxerick.customerapi.infrastructure.persistence;

import com.jxxerick.customerapi.domain.model.ChangeType;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.CustomerChange;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaCustomerHistory.class)
class JpaCustomerHistoryTest {

    @Autowired
    private JpaCustomerHistory history;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldStoreFieldsInOrderAndReadNewestFirst() {
        Customer customer = Fixtures.customer();
        CustomerChange created = CustomerChange.created(customer, Fixtures.NOW);
        history.record(created);
        customer.rename("Maria Souza");
        history.record(CustomerChange.renamed(customer, "Maria Silva", Fixtures.NOW.plusSeconds(30)));
        history.record(CustomerChange.created(Fixtures.customer(), Fixtures.NOW));
        entityManager.flush();
        entityManager.clear();

        PageResult<CustomerChange> page = history.findByCustomer(customer.getId(), 0, 10);

        assertThat(page.totalItems()).isEqualTo(2);
        assertThat(page.items()).extracting(CustomerChange::type)
                .containsExactly(ChangeType.RENAMED, ChangeType.CREATED);
        assertThat(page.items().get(1)).isEqualTo(created);
    }

    @Test
    void shouldBreakTiesByInsertionOrder() {
        Customer customer = Fixtures.customer();
        history.record(CustomerChange.created(customer, Fixtures.NOW));
        history.record(CustomerChange.deleted(customer, Fixtures.NOW));

        assertThat(history.findByCustomer(customer.getId(), 0, 1).items()).extracting(CustomerChange::type)
                .containsExactly(ChangeType.DELETED);
    }

    @Test
    void shouldReturnEmptyPageForUnknownCustomer() {
        assertThat(history.findByCustomer(UUID.randomUUID(), 0, 10).totalItems()).isZero();
    }
}
