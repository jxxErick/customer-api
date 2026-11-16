package com.jxxerick.customerapi.infrastructure.persistence;

import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.Email;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaCustomerRepository.class)
class JpaCustomerRepositoryTest {

    @Autowired
    private JpaCustomerRepository repository;

    @Test
    void shouldSaveAndFindCustomer() {
        Customer customer = repository.save(Fixtures.customer());

        assertThat(repository.findById(customer.getId())).get()
                .usingRecursiveComparison().isEqualTo(customer);
        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void shouldCheckEmailExistence() {
        repository.save(Fixtures.customer());

        assertThat(repository.existsByEmail(new Email("maria@example.com"))).isTrue();
        assertThat(repository.existsByEmail(new Email("other@example.com"))).isFalse();
    }

    @Test
    void shouldDeleteCustomer() {
        Customer customer = repository.save(Fixtures.customer());

        repository.delete(customer);

        assertThat(repository.findById(customer.getId())).isEmpty();
    }
}
