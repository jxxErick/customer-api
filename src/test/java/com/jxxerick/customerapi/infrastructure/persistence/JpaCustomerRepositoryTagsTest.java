package com.jxxerick.customerapi.infrastructure.persistence;

import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.Email;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.model.Tag;
import com.jxxerick.customerapi.domain.model.TagCount;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaCustomerRepository.class)
class JpaCustomerRepositoryTagsTest {

    @Autowired
    private JpaCustomerRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldStoreAndReplaceTags() {
        Customer customer = repository.save(customer("Maria Silva", "maria@example.com", "vip", "zona-sul"));
        entityManager.flush();
        entityManager.clear();

        Customer loaded = repository.findById(customer.getId()).orElseThrow();
        assertThat(loaded.getTags()).containsExactly(new Tag("vip"), new Tag("zona-sul"));

        loaded.retag(Set.of(new Tag("atacado")));
        repository.save(loaded);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(customer.getId()).orElseThrow().getTags()).containsExactly(new Tag("atacado"));
    }

    @Test
    void shouldRequireEveryTagAndCombineWithName() {
        repository.save(customer("Maria Silva", "maria@example.com", "vip", "zona-sul"));
        repository.save(customer("Mariana Costa", "mariana@example.com", "vip"));
        repository.save(customer("João Pereira", "joao@example.com", "vip", "zona-sul"));
        entityManager.flush();
        entityManager.clear();

        assertThat(names(repository.search(null, tags("vip"), 0, 10)))
                .containsExactly("João Pereira", "Maria Silva", "Mariana Costa");
        assertThat(names(repository.search(null, tags("vip", "zona-sul"), 0, 10)))
                .containsExactly("João Pereira", "Maria Silva");
        assertThat(names(repository.search("maria", tags("vip", "zona-sul"), 0, 10)))
                .containsExactly("Maria Silva");
        assertThat(repository.search(null, tags("atacado"), 0, 10).totalItems()).isZero();
    }

    @Test
    void shouldPageTaggedCustomersWithCorrectTotal() {
        repository.save(customer("Ana", "ana@example.com", "vip", "a", "b"));
        repository.save(customer("Bia", "bia@example.com", "vip", "c"));
        repository.save(customer("Caio", "caio@example.com", "vip"));
        entityManager.flush();
        entityManager.clear();

        PageResult<Customer> page = repository.search(null, tags("vip"), 1, 2);

        assertThat(page.totalItems()).isEqualTo(3);
        assertThat(names(page)).containsExactly("Caio");
    }

    @Test
    void shouldTreatLikeWildcardsInNameAsPlainText() {
        repository.save(customer("100% Maria", "maria@example.com"));
        repository.save(customer("Ana", "ana@example.com"));

        assertThat(names(repository.search("%", 0, 10))).containsExactly("100% Maria");
    }

    @Test
    void shouldCountCustomersPerTagMostUsedFirst() {
        repository.save(customer("Maria Silva", "maria@example.com", "vip", "zona-sul"));
        repository.save(customer("Mariana Costa", "mariana@example.com", "vip"));
        repository.save(customer("João Pereira", "joao@example.com"));
        entityManager.flush();

        assertThat(repository.countByTag())
                .containsExactly(new TagCount("vip", 2), new TagCount("zona-sul", 1));
    }

    private static Customer customer(String name, String email, String... tags) {
        Customer customer = Customer.create(name, new Email(email), Fixtures.address(), Fixtures.NOW);
        customer.retag(tags(tags));
        return customer;
    }

    private static Set<Tag> tags(String... values) {
        return Arrays.stream(values).map(Tag::new).collect(Collectors.toSet());
    }

    private static java.util.List<String> names(PageResult<Customer> page) {
        return page.items().stream().map(Customer::getName).toList();
    }
}
