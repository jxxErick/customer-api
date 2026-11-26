package com.jxxerick.customerapi.infrastructure.persistence;

import com.jxxerick.customerapi.domain.model.Cpf;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.Email;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.model.TrashedCustomer;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaCustomerTrash.class, JpaCustomerRepository.class})
class JpaCustomerTrashTest {

    private static final Duration RETENTION = Duration.ofDays(30);

    @Autowired
    private JpaCustomerTrash trash;

    @Autowired
    private JpaCustomerRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldKeepEveryFieldOfTheSnapshot() {
        Customer customer = Customer.restore(UUID.randomUUID(), "Maria Silva", new Email("maria@example.com"),
                new Cpf("52998224725"), Fixtures.address(), Fixtures.NOW, 2L);
        trash.put(TrashedCustomer.of(customer, Fixtures.NOW.plusSeconds(10), RETENTION));
        entityManager.flush();
        entityManager.clear();

        TrashedCustomer found = trash.find(customer.getId()).orElseThrow();

        assertThat(found.customer().getName()).isEqualTo("Maria Silva");
        assertThat(found.customer().getCpf()).contains(new Cpf("52998224725"));
        assertThat(found.customer().getAddress()).isEqualTo(Fixtures.address());
        assertThat(found.customer().getCreatedAt()).isEqualTo(Fixtures.NOW);
        assertThat(found.customer().getVersion()).isNull();
        assertThat(found.deletedAt()).isEqualTo(Fixtures.NOW.plusSeconds(10));
        assertThat(found.purgeAt()).isEqualTo(Fixtures.NOW.plusSeconds(10).plus(RETENTION));
    }

    @Test
    void shouldAllowTheSameEmailTwice() {
        trash.put(TrashedCustomer.of(Fixtures.customer(), Fixtures.NOW, RETENTION));
        trash.put(TrashedCustomer.of(Fixtures.customer(), Fixtures.NOW, RETENTION));
        entityManager.flush();

        assertThat(trash.list(0, 10).totalItems()).isEqualTo(2);
    }

    @Test
    void shouldListMostRecentlyDeletedFirst() {
        Customer older = Fixtures.customer();
        Customer newer = Fixtures.customer();
        trash.put(TrashedCustomer.of(older, Fixtures.NOW, RETENTION));
        trash.put(TrashedCustomer.of(newer, Fixtures.NOW.plusSeconds(60), RETENTION));
        entityManager.flush();

        PageResult<TrashedCustomer> page = trash.list(0, 1);

        assertThat(page.totalItems()).isEqualTo(2);
        assertThat(page.items()).extracting(t -> t.customer().getId()).containsExactly(newer.getId());
    }

    @Test
    void shouldPurgeOnlyWhatIsDue() {
        Customer due = Fixtures.customer();
        Customer notYet = Fixtures.customer();
        trash.put(TrashedCustomer.of(due, Fixtures.NOW, RETENTION));
        trash.put(TrashedCustomer.of(notYet, Fixtures.NOW.plusSeconds(1), RETENTION));
        entityManager.flush();

        int removed = trash.purgeDue(Fixtures.NOW.plus(RETENTION));
        entityManager.clear();

        assertThat(removed).isEqualTo(1);
        assertThat(trash.find(due.getId())).isEmpty();
        assertThat(trash.find(notYet.getId())).isPresent();
    }

    @Test
    void restoredSnapshotShouldBeInsertableUnderItsOriginalId() {
        Customer saved = repository.save(Fixtures.customer());
        repository.delete(saved);
        trash.put(TrashedCustomer.of(saved, Fixtures.NOW, RETENTION));
        entityManager.flush();
        entityManager.clear();

        Customer restored = repository.save(trash.find(saved.getId()).orElseThrow().toRestoredCustomer());
        entityManager.flush();

        assertThat(restored.getId()).isEqualTo(saved.getId());
        assertThat(repository.findById(saved.getId())).isPresent();
    }
}
