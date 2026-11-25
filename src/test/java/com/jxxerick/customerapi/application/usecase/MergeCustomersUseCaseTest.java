package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.MergeCustomersCommand;
import com.jxxerick.customerapi.domain.exception.ConcurrentUpdateException;
import com.jxxerick.customerapi.domain.exception.CustomerMergeConflictException;
import com.jxxerick.customerapi.domain.exception.CustomerNotFoundException;
import com.jxxerick.customerapi.domain.exception.DomainValidationException;
import com.jxxerick.customerapi.domain.model.ChangeType;
import com.jxxerick.customerapi.domain.model.Cpf;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.Email;
import com.jxxerick.customerapi.domain.model.FieldChange;
import com.jxxerick.customerapi.domain.port.CustomerRepository;
import com.jxxerick.customerapi.support.Fixtures;
import com.jxxerick.customerapi.support.RecordingHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MergeCustomersUseCaseTest {

    private static final Cpf CPF = new Cpf("529.982.247-25");
    private static final Cpf OTHER_CPF = new Cpf("111.444.777-35");

    @Mock
    private CustomerRepository customerRepository;

    private final RecordingHistory history = new RecordingHistory();

    private MergeCustomersUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new MergeCustomersUseCase(new GetCustomerUseCase(customerRepository), customerRepository,
                history, Clock.fixed(Fixtures.NOW, ZoneOffset.UTC));
    }

    private static Customer stored(String email, Cpf cpf) {
        return Customer.restore(UUID.randomUUID(), "Maria Silva", new Email(email), cpf, Fixtures.address(),
                Fixtures.NOW, 1L);
    }

    private void given(Customer... customers) {
        for (Customer customer : customers) {
            when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        }
    }

    @Test
    void shouldDeleteDuplicateFirstAndInheritItsCpf() {
        Customer target = stored("maria@example.com", null);
        Customer duplicate = stored("maria.silva@example.com", CPF);
        given(target, duplicate);
        when(customerRepository.save(target)).thenReturn(target);

        Customer merged = useCase.execute(new MergeCustomersCommand(target.getId(), duplicate.getId(), 1L));

        assertThat(merged.getCpf()).contains(CPF);
        assertThat(merged.getEmail().value()).isEqualTo("maria@example.com");
        InOrder order = inOrder(customerRepository);
        order.verify(customerRepository).delete(duplicate);
        order.verify(customerRepository).save(target);
        assertThat(history.recorded()).satisfiesExactly(
                change -> {
                    assertThat(change.customerId()).isEqualTo(duplicate.getId());
                    assertThat(change.type()).isEqualTo(ChangeType.MERGED_INTO);
                    assertThat(change.fields()).containsExactly(
                            new FieldChange("mergedInto", null, target.getId().toString()));
                },
                change -> {
                    assertThat(change.customerId()).isEqualTo(target.getId());
                    assertThat(change.type()).isEqualTo(ChangeType.MERGED);
                    assertThat(change.fields()).containsExactly(
                            new FieldChange("mergedFrom", null, duplicate.getId().toString()),
                            new FieldChange("cpf", null, CPF.masked()));
                });
    }

    @Test
    void shouldKeepTargetCpfWhenBothHaveTheSame() {
        Customer target = stored("maria@example.com", CPF);
        Customer duplicate = stored("maria.silva@example.com", CPF);
        given(target, duplicate);
        when(customerRepository.save(target)).thenReturn(target);

        useCase.execute(new MergeCustomersCommand(target.getId(), duplicate.getId(), null));

        assertThat(history.recorded().get(1).fields()).hasSize(1);
    }

    @Test
    void shouldRefuseCustomersWithDifferentCpfs() {
        Customer target = stored("maria@example.com", CPF);
        Customer duplicate = stored("maria.silva@example.com", OTHER_CPF);
        given(target, duplicate);

        assertThatThrownBy(() -> useCase.execute(new MergeCustomersCommand(target.getId(), duplicate.getId(), null)))
                .isInstanceOf(CustomerMergeConflictException.class);
        verify(customerRepository, never()).delete(any());
        assertThat(history.recorded()).isEmpty();
    }

    @Test
    void shouldRefuseMergingCustomerWithItself() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.execute(new MergeCustomersCommand(id, id, null)))
                .isInstanceOf(DomainValidationException.class);
        verifyNoInteractions(customerRepository);
    }

    @Test
    void shouldRejectOutdatedTargetVersion() {
        Customer target = stored("maria@example.com", null);
        given(target);

        assertThatThrownBy(() -> useCase.execute(new MergeCustomersCommand(target.getId(), UUID.randomUUID(), 0L)))
                .isInstanceOf(ConcurrentUpdateException.class);
        verify(customerRepository, never()).delete(any());
    }

    @Test
    void shouldFailWhenDuplicateIsMissing() {
        Customer target = stored("maria@example.com", null);
        UUID missing = UUID.randomUUID();
        given(target);
        when(customerRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new MergeCustomersCommand(target.getId(), missing, null)))
                .isInstanceOf(CustomerNotFoundException.class);
        verify(customerRepository, never()).delete(any());
    }
}
