package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.domain.exception.CpfAlreadyInUseException;
import com.jxxerick.customerapi.domain.exception.CustomerNotFoundException;
import com.jxxerick.customerapi.domain.exception.EmailAlreadyInUseException;
import com.jxxerick.customerapi.domain.exception.TrashedCustomerNotFoundException;
import com.jxxerick.customerapi.domain.model.ChangeType;
import com.jxxerick.customerapi.domain.model.Cpf;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.CustomerChange;
import com.jxxerick.customerapi.domain.model.Email;
import com.jxxerick.customerapi.domain.model.TrashedCustomer;
import com.jxxerick.customerapi.domain.port.CustomerRepository;
import com.jxxerick.customerapi.support.Fixtures;
import com.jxxerick.customerapi.support.InMemoryTrash;
import com.jxxerick.customerapi.support.RecordingHistory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerTrashUseCasesTest {

    private static final Duration RETENTION = Duration.ofDays(30);

    @Mock
    private CustomerRepository customerRepository;

    private final InMemoryTrash trash = new InMemoryTrash();
    private final RecordingHistory history = new RecordingHistory();
    private final Clock clock = Clock.fixed(Fixtures.NOW, ZoneOffset.UTC);

    private DeleteCustomerUseCase deleteUseCase() {
        return new DeleteCustomerUseCase(new GetCustomerUseCase(customerRepository), customerRepository, trash,
                history, clock, RETENTION);
    }

    private RestoreCustomerUseCase restoreUseCase() {
        return new RestoreCustomerUseCase(trash, customerRepository, history, clock);
    }

    @Test
    void deleteShouldMoveCustomerToTrashWithPurgeDate() {
        Customer customer = Fixtures.storedCustomer(3);
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        deleteUseCase().execute(customer.getId());

        verify(customerRepository).delete(customer);
        TrashedCustomer trashed = trash.find(customer.getId()).orElseThrow();
        assertThat(trashed.deletedAt()).isEqualTo(Fixtures.NOW);
        assertThat(trashed.purgeAt()).isEqualTo(Fixtures.NOW.plus(RETENTION));
        assertThat(history.recorded()).extracting(CustomerChange::type).containsExactly(ChangeType.DELETED);
    }

    @Test
    void deleteShouldFailWhenCustomerDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());
        DeleteCustomerUseCase useCase = deleteUseCase();

        assertThatThrownBy(() -> useCase.execute(id)).isInstanceOf(CustomerNotFoundException.class);
        verify(customerRepository, never()).delete(any());
        assertThat(trash.size()).isZero();
    }

    @Test
    void restoreShouldSaveSameCustomerAsNewRowAndEmptyTheSlot() {
        Customer customer = Customer.restore(UUID.randomUUID(), "Maria Silva", new Email("maria@example.com"),
                new Cpf("52998224725"), Fixtures.address(), Fixtures.NOW, 4L);
        trash.put(TrashedCustomer.of(customer, Fixtures.NOW, RETENTION));
        when(customerRepository.existsByEmail(customer.getEmail())).thenReturn(false);
        when(customerRepository.existsByCpf(new Cpf("52998224725"))).thenReturn(false);
        when(customerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Customer restored = restoreUseCase().execute(customer.getId());

        ArgumentCaptor<Customer> saved = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(customer.getId());
        assertThat(saved.getValue().getVersion()).isNull();
        assertThat(saved.getValue().getCpf()).contains(new Cpf("52998224725"));
        assertThat(restored.getCreatedAt()).isEqualTo(Fixtures.NOW);
        assertThat(trash.find(customer.getId())).isEmpty();
        assertThat(history.recorded()).extracting(CustomerChange::type).containsExactly(ChangeType.RESTORED);
    }

    @Test
    void restoreShouldFailWhenEmailWasTakenMeanwhile() {
        Customer customer = Fixtures.customer();
        trash.put(TrashedCustomer.of(customer, Fixtures.NOW, RETENTION));
        when(customerRepository.existsByEmail(customer.getEmail())).thenReturn(true);
        RestoreCustomerUseCase useCase = restoreUseCase();

        assertThatThrownBy(() -> useCase.execute(customer.getId())).isInstanceOf(EmailAlreadyInUseException.class);
        verify(customerRepository, never()).save(any());
        assertThat(trash.find(customer.getId())).isPresent();
        assertThat(history.recorded()).isEmpty();
    }

    @Test
    void restoreShouldFailWhenCpfWasTakenMeanwhile() {
        Customer customer = Customer.create("Maria Silva", new Email("maria@example.com"), new Cpf("52998224725"),
                Fixtures.address(), Fixtures.NOW);
        trash.put(TrashedCustomer.of(customer, Fixtures.NOW, RETENTION));
        when(customerRepository.existsByEmail(customer.getEmail())).thenReturn(false);
        when(customerRepository.existsByCpf(new Cpf("52998224725"))).thenReturn(true);
        RestoreCustomerUseCase useCase = restoreUseCase();

        assertThatThrownBy(() -> useCase.execute(customer.getId())).isInstanceOf(CpfAlreadyInUseException.class);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void restoreShouldFailWhenNotInTrash() {
        UUID id = UUID.randomUUID();
        RestoreCustomerUseCase useCase = restoreUseCase();

        assertThatThrownBy(() -> useCase.execute(id)).isInstanceOf(TrashedCustomerNotFoundException.class);
    }

    @Test
    void listShouldClampPagingAndShowNewestFirst() {
        Customer older = Fixtures.customer();
        Customer newer = Fixtures.customer();
        trash.put(TrashedCustomer.of(older, Fixtures.NOW, RETENTION));
        trash.put(TrashedCustomer.of(newer, Fixtures.NOW.plusSeconds(60), RETENTION));

        var page = new ListTrashUseCase(trash).execute(-1, 0);

        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(1);
        assertThat(page.totalItems()).isEqualTo(2);
        assertThat(page.items()).extracting(t -> t.customer().getId()).containsExactly(newer.getId());
    }

    @Test
    void purgeShouldRemoveOnlyThatCustomer() {
        Customer gone = Fixtures.customer();
        Customer kept = Fixtures.customer();
        trash.put(TrashedCustomer.of(gone, Fixtures.NOW, RETENTION));
        trash.put(TrashedCustomer.of(kept, Fixtures.NOW, RETENTION));

        new PurgeTrashedCustomerUseCase(trash).execute(gone.getId());

        assertThat(trash.find(gone.getId())).isEmpty();
        assertThat(trash.find(kept.getId())).isPresent();
    }

    @Test
    void purgeShouldFailWhenNotInTrash() {
        PurgeTrashedCustomerUseCase useCase = new PurgeTrashedCustomerUseCase(trash);
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.execute(id)).isInstanceOf(TrashedCustomerNotFoundException.class);
    }
}
