package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.CreateCustomerCommand;
import com.jxxerick.customerapi.application.command.ImportRow;
import com.jxxerick.customerapi.application.usecase.ImportCustomersUseCase.Report;
import com.jxxerick.customerapi.application.usecase.ImportCustomersUseCase.RowResult;
import com.jxxerick.customerapi.application.usecase.ImportCustomersUseCase.RowStatus;
import com.jxxerick.customerapi.domain.exception.AddressProviderUnavailableException;
import com.jxxerick.customerapi.domain.exception.DomainValidationException;
import com.jxxerick.customerapi.domain.exception.EmailAlreadyInUseException;
import com.jxxerick.customerapi.domain.exception.ImportTooLargeException;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportCustomersUseCaseTest {

    @Mock
    private CreateCustomerUseCase createCustomer;

    @InjectMocks
    private ImportCustomersUseCase useCase;

    @Test
    void shouldCreateGoodRowsAndReportBadOnes() {
        Customer maria = Fixtures.customer();
        ImportRow ok = row(2, "maria@example.com");
        ImportRow duplicated = row(3, "maria@example.com");
        ImportRow invalid = row(4, "nope");
        when(createCustomer.execute(ok.command())).thenReturn(maria)
                .thenThrow(new EmailAlreadyInUseException("maria@example.com"));
        when(createCustomer.execute(invalid.command())).thenThrow(new DomainValidationException("E-mail inválido: nope"));

        Report report = useCase.execute(List.of(ok, duplicated, invalid));

        assertThat(report.total()).isEqualTo(3);
        assertThat(report.created()).isEqualTo(1);
        assertThat(report.failed()).isEqualTo(2);
        assertThat(report.rows()).extracting(RowResult::line, RowResult::status, RowResult::error).containsExactly(
                tuple(2, RowStatus.CREATED, null),
                tuple(3, RowStatus.FAILED, "E-mail já cadastrado: maria@example.com"),
                tuple(4, RowStatus.FAILED, "E-mail inválido: nope"));
        assertThat(report.rows().getFirst().customerId()).isEqualTo(maria.getId());
    }

    @Test
    void shouldStopCallingTheZipCodeServiceOnceItIsDown() {
        when(createCustomer.execute(any())).thenThrow(new AddressProviderUnavailableException(new RuntimeException()));

        Report report = useCase.execute(List.of(row(2, "a@example.com"), row(3, "b@example.com"),
                row(4, "c@example.com")));

        assertThat(report.failed()).isEqualTo(3);
        assertThat(report.rows()).allSatisfy(r -> assertThat(r.error()).contains("CEP indisponível"));
        verify(createCustomer, times(1)).execute(any());
    }

    @Test
    void shouldRejectFilesAboveTheLimit() {
        List<ImportRow> rows = IntStream.range(0, ImportCustomersUseCase.MAX_ROWS + 1)
                .mapToObj(i -> row(i + 2, "c" + i + "@example.com"))
                .toList();

        assertThatThrownBy(() -> useCase.execute(rows)).isInstanceOf(ImportTooLargeException.class);
        verifyNoInteractions(createCustomer);
    }

    private static ImportRow row(int line, String email) {
        return new ImportRow(line, new CreateCustomerCommand("Maria Silva", email, "01001-000", "100", null));
    }
}
