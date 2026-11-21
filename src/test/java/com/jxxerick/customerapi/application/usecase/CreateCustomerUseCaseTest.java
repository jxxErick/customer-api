package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.CreateCustomerCommand;
import com.jxxerick.customerapi.domain.exception.AddressNotFoundException;
import com.jxxerick.customerapi.domain.exception.CpfAlreadyInUseException;
import com.jxxerick.customerapi.domain.exception.DomainValidationException;
import com.jxxerick.customerapi.domain.exception.EmailAlreadyInUseException;
import com.jxxerick.customerapi.domain.model.Cpf;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.Email;
import com.jxxerick.customerapi.domain.model.ZipCode;
import com.jxxerick.customerapi.domain.port.AddressProvider;
import com.jxxerick.customerapi.domain.port.CustomerRepository;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCustomerUseCaseTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AddressProvider addressProvider;

    private CreateCustomerUseCase useCase;

    private final CreateCustomerCommand command =
            new CreateCustomerCommand("Maria Silva", "Maria@Example.com", "01001-000", "100", "Apto 12");

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Fixtures.NOW, ZoneOffset.UTC);
        useCase = new CreateCustomerUseCase(customerRepository, new AddressResolver(addressProvider), clock);
    }

    @Test
    void shouldCreateCustomerWithResolvedAddress() {
        when(customerRepository.existsByEmail(new Email("maria@example.com"))).thenReturn(false);
        when(addressProvider.findByZipCode(new ZipCode("01001000"))).thenReturn(Optional.of(Fixtures.postalAddress()));
        when(customerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Customer customer = useCase.execute(command);

        assertThat(customer.getEmail().value()).isEqualTo("maria@example.com");
        assertThat(customer.getAddress().postal().city()).isEqualTo("São Paulo");
        assertThat(customer.getAddress().number()).isEqualTo("100");
        assertThat(customer.getCreatedAt()).isEqualTo(Fixtures.NOW);
    }

    @Test
    void shouldFailWhenEmailIsAlreadyInUse() {
        when(customerRepository.existsByEmail(any())).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(EmailAlreadyInUseException.class);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldStoreCpfWhenInformed() {
        when(customerRepository.existsByEmail(any())).thenReturn(false);
        when(customerRepository.existsByCpf(new Cpf("52998224725"))).thenReturn(false);
        when(addressProvider.findByZipCode(any())).thenReturn(Optional.of(Fixtures.postalAddress()));
        when(customerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Customer customer = useCase.execute(withCpf("529.982.247-25"));

        assertThat(customer.getCpf()).contains(new Cpf("52998224725"));
    }

    @Test
    void shouldFailWhenCpfIsAlreadyInUseWithoutCallingTheZipCodeService() {
        when(customerRepository.existsByEmail(any())).thenReturn(false);
        when(customerRepository.existsByCpf(new Cpf("52998224725"))).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(withCpf("52998224725")))
                .isInstanceOf(CpfAlreadyInUseException.class)
                .hasMessage("CPF já cadastrado: ***.982.247-**");
        verifyNoInteractions(addressProvider);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldRejectInvalidCpfBeforeAnyLookup() {
        assertThatThrownBy(() -> useCase.execute(withCpf("123.456.789-00")))
                .isInstanceOf(DomainValidationException.class);
        verifyNoInteractions(customerRepository, addressProvider);
    }

    @Test
    void shouldTreatBlankCpfAsAbsent() {
        when(customerRepository.existsByEmail(any())).thenReturn(false);
        when(addressProvider.findByZipCode(any())).thenReturn(Optional.of(Fixtures.postalAddress()));
        when(customerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(useCase.execute(withCpf("  ")).getCpf()).isEmpty();
        verify(customerRepository, never()).existsByCpf(any());
    }

    private static CreateCustomerCommand withCpf(String cpf) {
        return new CreateCustomerCommand("Maria Silva", "maria@example.com", "01001-000", "100", null, cpf);
    }

    @Test
    void shouldFailWhenZipCodeIsUnknown() {
        when(customerRepository.existsByEmail(any())).thenReturn(false);
        when(addressProvider.findByZipCode(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(AddressNotFoundException.class)
                .hasMessageContaining("01001-000");
        verify(customerRepository, never()).save(any());
    }
}
