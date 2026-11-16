package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.ChangeAddressCommand;
import com.jxxerick.customerapi.domain.exception.AddressNotFoundException;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.PostalAddress;
import com.jxxerick.customerapi.domain.model.ZipCode;
import com.jxxerick.customerapi.domain.port.AddressProvider;
import com.jxxerick.customerapi.domain.port.CustomerRepository;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeCustomerAddressUseCaseTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AddressProvider addressProvider;

    private ChangeCustomerAddressUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ChangeCustomerAddressUseCase(
                new GetCustomerUseCase(customerRepository), customerRepository, new AddressResolver(addressProvider));
    }

    @Test
    void shouldMoveCustomerToNewAddress() {
        Customer customer = Fixtures.customer();
        ZipCode newZip = new ZipCode("20040-020");
        PostalAddress rio = new PostalAddress(newZip, "Rua da Assembleia", "Centro", "Rio de Janeiro", "RJ");
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(addressProvider.findByZipCode(newZip)).thenReturn(Optional.of(rio));
        when(customerRepository.save(customer)).thenReturn(customer);

        Customer updated = useCase.execute(new ChangeAddressCommand(customer.getId(), "20040020", "5", null));

        assertThat(updated.getAddress().postal()).isEqualTo(rio);
        assertThat(updated.getAddress().number()).isEqualTo("5");
    }

    @Test
    void shouldKeepAddressWhenZipCodeIsUnknown() {
        Customer customer = Fixtures.customer();
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(addressProvider.findByZipCode(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ChangeAddressCommand(customer.getId(), "99999999", "5", null)))
                .isInstanceOf(AddressNotFoundException.class);
        assertThat(customer.getAddress()).isEqualTo(Fixtures.address());
        verify(customerRepository, never()).save(any());
    }
}
