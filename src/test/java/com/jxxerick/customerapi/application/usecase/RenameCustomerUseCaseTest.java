package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.RenameCustomerCommand;
import com.jxxerick.customerapi.domain.exception.ConcurrentUpdateException;
import com.jxxerick.customerapi.domain.exception.CustomerNotFoundException;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.port.CustomerRepository;
import com.jxxerick.customerapi.domain.model.ChangeType;
import com.jxxerick.customerapi.domain.model.FieldChange;
import com.jxxerick.customerapi.support.Fixtures;
import com.jxxerick.customerapi.support.RecordingHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
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
class RenameCustomerUseCaseTest {

    @Mock
    private CustomerRepository customerRepository;

    private final RecordingHistory history = new RecordingHistory();

    private RenameCustomerUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RenameCustomerUseCase(new GetCustomerUseCase(customerRepository), customerRepository,
                history, Clock.fixed(Fixtures.NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldRenameAndTrimName() {
        Customer customer = Fixtures.customer();
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);

        Customer updated = useCase.execute(new RenameCustomerCommand(customer.getId(), "  Maria Souza  ", null));

        assertThat(updated.getName()).isEqualTo("Maria Souza");
        assertThat(history.recorded()).singleElement().satisfies(change -> {
            assertThat(change.type()).isEqualTo(ChangeType.RENAMED);
            assertThat(change.occurredAt()).isEqualTo(Fixtures.NOW);
            assertThat(change.fields()).containsExactly(new FieldChange("name", "Maria Silva", "Maria Souza"));
        });
    }

    @Test
    void shouldFailWhenCustomerIsMissing() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new RenameCustomerCommand(id, "Maria", null)))
                .isInstanceOf(CustomerNotFoundException.class);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldRenameWhenVersionMatches() {
        Customer customer = Fixtures.storedCustomer(2);
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);

        Customer updated = useCase.execute(new RenameCustomerCommand(customer.getId(), "Maria Souza", 2L));

        assertThat(updated.getName()).isEqualTo("Maria Souza");
    }

    @Test
    void shouldRejectOutdatedVersion() {
        Customer customer = Fixtures.storedCustomer(2);
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> useCase.execute(new RenameCustomerCommand(customer.getId(), "Maria Souza", 1L)))
                .isInstanceOf(ConcurrentUpdateException.class);
        assertThat(customer.getName()).isEqualTo("Maria Silva");
        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldNotRecordHistoryWhenNameIsUnchanged() {
        Customer customer = Fixtures.customer();
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);

        useCase.execute(new RenameCustomerCommand(customer.getId(), " Maria Silva ", null));

        assertThat(history.recorded()).isEmpty();
    }
}
