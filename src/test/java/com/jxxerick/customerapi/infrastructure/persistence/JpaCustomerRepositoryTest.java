package com.jxxerick.customerapi.infrastructure.persistence;

import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.Email;
import com.jxxerick.customerapi.domain.model.PageResult;
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
        assertThat(repository.search(null, 0, 10).items()).hasSize(1);
    }

    @Test
    void shouldSearchByNameIgnoringCaseAndPage() {
        repository.save(customer("Maria Silva", "maria@example.com"));
        repository.save(customer("Ana Maria Souza", "ana@example.com"));
        repository.save(customer("João Pereira", "joao@example.com"));

        PageResult<Customer> firstPage = repository.search("MARIA", 0, 1);
        PageResult<Customer> secondPage = repository.search("MARIA", 1, 1);

        assertThat(firstPage.totalItems()).isEqualTo(2);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.items()).extracting(Customer::getName).containsExactly("Ana Maria Souza");
        assertThat(secondPage.items()).extracting(Customer::getName).containsExactly("Maria Silva");
        assertThat(repository.search(null, 0, 10).totalItems()).isEqualTo(3);
    }

    private static Customer customer(String name, String email) {
        return Customer.create(name, new Email(email), Fixtures.address(), Fixtures.NOW);
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
