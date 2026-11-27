package com.jxxerick.customerapi.infrastructure.persistence;

import com.jxxerick.customerapi.domain.model.Address;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.CustomerFilter;
import com.jxxerick.customerapi.domain.model.Email;
import com.jxxerick.customerapi.domain.model.PostalAddress;
import com.jxxerick.customerapi.domain.model.Segment;
import com.jxxerick.customerapi.domain.model.Tag;
import com.jxxerick.customerapi.domain.model.ZipCode;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaSegmentRepository.class, JpaCustomerRepository.class})
class JpaSegmentRepositoryTest {

    @Autowired
    private JpaSegmentRepository segments;

    @Autowired
    private JpaCustomerRepository customers;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldStoreCriteriaAndListByName() {
        Segment vips = segments.save(Segment.create("VIPs", new CustomerFilter("silva", tags("vip", "zona-sul"),
                "SP", "São Paulo"), Fixtures.NOW));
        segments.save(Segment.create("Atacado", new CustomerFilter(null, tags("atacado"), null, null), Fixtures.NOW));
        entityManager.flush();
        entityManager.clear();

        assertThat(segments.findById(vips.id())).contains(vips);
        assertThat(segments.findAll()).extracting(Segment::name).containsExactly("Atacado", "VIPs");
        assertThat(segments.existsByName("vips")).isTrue();
        assertThat(segments.existsByName("Outro")).isFalse();
    }

    @Test
    void shouldDelete() {
        Segment vips = segments.save(Segment.create("VIPs", new CustomerFilter(null, tags("vip"), null, null),
                Fixtures.NOW));

        segments.delete(vips.id());

        assertThat(segments.findById(vips.id())).isEmpty();
    }

    @Test
    void customerFilterShouldCombineStateCityAndTags() {
        customers.save(customer("Maria Silva", "maria@example.com", "São Paulo", "SP", "vip"));
        customers.save(customer("Ana Souza", "ana@example.com", "Campinas", "SP", "vip"));
        customers.save(customer("João Pereira", "joao@example.com", "Rio de Janeiro", "RJ", "vip"));
        customers.save(customer("Bia Lima", "bia@example.com", "São Paulo", "SP"));
        entityManager.flush();
        entityManager.clear();

        assertThat(names(new CustomerFilter(null, Set.of(), "SP", null))).containsExactly("Ana Souza", "Bia Lima",
                "Maria Silva");
        assertThat(names(new CustomerFilter(null, tags("vip"), "SP", "SÃO PAULO"))).containsExactly("Maria Silva");
        assertThat(customers.countMatching(new CustomerFilter(null, tags("vip"), null, null))).isEqualTo(3);
        assertThat(customers.countMatching(new CustomerFilter(null, Set.of(), "MG", null))).isZero();
    }

    private List<String> names(CustomerFilter filter) {
        return customers.findMatching(filter, 0, 10).items().stream().map(Customer::getName).toList();
    }

    private static Customer customer(String name, String email, String city, String state, String... tags) {
        Address address = new Address(new PostalAddress(new ZipCode("01001-000"), "Rua A", "Centro", city, state),
                "1", null);
        Customer customer = Customer.create(name, new Email(email), address, Fixtures.NOW);
        customer.retag(tags(tags));
        return customer;
    }

    private static Set<Tag> tags(String... values) {
        return Arrays.stream(values).map(Tag::new).collect(Collectors.toSet());
    }
}
