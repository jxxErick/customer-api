package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.domain.exception.DomainValidationException;
import com.jxxerick.customerapi.domain.model.Address;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.DuplicateCandidate;
import com.jxxerick.customerapi.domain.model.Email;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.model.PostalAddress;
import com.jxxerick.customerapi.domain.model.ZipCode;
import com.jxxerick.customerapi.domain.port.CustomerRepository;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindDuplicateCustomersUseCaseTest {

    private static final int SIZE = FindDuplicateCustomersUseCase.PAGE_SIZE;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private FindDuplicateCustomersUseCase useCase;

    private static final Address OTHER_ADDRESS = new Address(
            new PostalAddress(new ZipCode("20040-002"), "Rua da Assembleia", "Centro", "Rio de Janeiro", "RJ"),
            "10", null);

    private static Customer customer(String name, String email, Address address) {
        return Customer.create(name, new Email(email), address, Fixtures.NOW);
    }

    private static Customer customer(String name, String email) {
        return customer(name, email, Fixtures.address());
    }

    @Test
    void shouldCompareCustomersAcrossPagesAndSortByScore() {
        Customer maria = customer("Maria Silva", "maria@example.com");
        Customer mariaTypo = customer("Maira Silva", "m.silva@example.com", OTHER_ADDRESS);
        Customer joao = customer("João Pereira", "joao@example.com");
        Customer mariaAgain = customer("Silva Maria", "contato@example.com");
        when(customerRepository.search(null, 0, SIZE))
                .thenReturn(new PageResult<>(List.of(maria, mariaTypo), 0, SIZE, SIZE + 2));
        when(customerRepository.search(null, 1, SIZE))
                .thenReturn(new PageResult<>(List.of(joao, mariaAgain), 1, SIZE, SIZE + 2));

        List<DuplicateCandidate> duplicates = useCase.execute(0.85);

        assertThat(duplicates).hasSize(3);
        assertThat(duplicates.get(0).score()).isEqualTo(1.0);
        assertThat(List.of(duplicates.get(0).first(), duplicates.get(0).second()))
                .containsExactlyInAnyOrder(maria, mariaAgain);
        assertThat(duplicates).extracting(DuplicateCandidate::score).isSortedAccordingTo((a, b) -> Double.compare(b, a));
        assertThat(duplicates).noneMatch(candidate -> candidate.first() == joao || candidate.second() == joao);
    }

    @Test
    void shouldApplyMinimumScore() {
        Customer maria = customer("Maria Silva", "maria@example.com");
        Customer mariaTypo = customer("Maira Silva", "outra@example.com", OTHER_ADDRESS);
        when(customerRepository.search(null, 0, SIZE))
                .thenReturn(new PageResult<>(List.of(maria, mariaTypo), 0, SIZE, 2));

        assertThat(useCase.execute(1.0)).isEmpty();
    }

    @Test
    void shouldRejectMinimumScoreOutOfRange() {
        assertThatThrownBy(() -> useCase.execute(0.2)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> useCase.execute(1.5)).isInstanceOf(DomainValidationException.class);
        verifyNoInteractions(customerRepository);
    }
}
