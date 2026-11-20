package com.jxxerick.customerapi.infrastructure.persistence;

import com.jxxerick.customerapi.domain.exception.ConcurrentUpdateException;
import com.jxxerick.customerapi.domain.model.Address;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.Email;
import com.jxxerick.customerapi.domain.model.LocationCount;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.model.PostalAddress;
import com.jxxerick.customerapi.domain.model.ZipCode;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaCustomerRepository.class)
class JpaCustomerRepositoryTest {

    @Autowired
    private JpaCustomerRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldSaveAndFindCustomer() {
        Customer customer = repository.save(Fixtures.customer());

        assertThat(repository.findById(customer.getId())).get()
                .usingRecursiveComparison().isEqualTo(customer);
        assertThat(repository.search(null, 0, 10).items()).hasSize(1);
    }

    @Test
    void shouldBumpVersionOnEveryUpdate() {
        Customer customer = repository.save(Fixtures.customer());
        entityManager.flush();
        assertThat(customer.getVersion()).isZero();

        customer.rename("Maria Souza");
        repository.save(customer);
        entityManager.flush();

        assertThat(repository.findById(customer.getId())).get().extracting(Customer::getVersion).isEqualTo(1L);
    }

    @Test
    void shouldRejectSavingAnOutdatedCopy() {
        Customer original = repository.save(Fixtures.customer());
        entityManager.flush();
        Customer stale = repository.findById(original.getId()).orElseThrow();

        original.rename("Maria Souza");
        repository.save(original);
        entityManager.flush();
        stale.rename("Maria Oliveira");

        assertThatThrownBy(() -> repository.save(stale)).isInstanceOf(ConcurrentUpdateException.class);
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
    void shouldCountCustomersPerCity() {
        repository.save(customer("Maria Silva", "maria@example.com"));
        repository.save(customer("Ana Souza", "ana@example.com"));
        repository.save(Customer.create("João Pereira", new Email("joao@example.com"),
                new Address(new PostalAddress(new ZipCode("20040-020"), "Rua da Assembleia", "Centro",
                        "Rio de Janeiro", "RJ"), "5", null), Fixtures.NOW));

        assertThat(repository.countByLocation()).containsExactlyInAnyOrder(
                new LocationCount("SP", "São Paulo", 2),
                new LocationCount("RJ", "Rio de Janeiro", 1));
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
