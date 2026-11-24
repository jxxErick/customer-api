package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.CustomerHistoryQuery;
import com.jxxerick.customerapi.domain.exception.CustomerNotFoundException;
import com.jxxerick.customerapi.domain.model.ChangeType;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.CustomerChange;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.support.Fixtures;
import com.jxxerick.customerapi.support.RecordingHistory;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetCustomerHistoryUseCaseTest {

    private final RecordingHistory history = new RecordingHistory();
    private final GetCustomerHistoryUseCase useCase = new GetCustomerHistoryUseCase(history);

    @Test
    void shouldReturnNewestChangesFirst() {
        Customer customer = Fixtures.customer();
        history.record(CustomerChange.created(customer, Fixtures.NOW));
        customer.rename("Maria Souza");
        history.record(CustomerChange.renamed(customer, "Maria Silva", Fixtures.NOW.plusSeconds(60)));
        history.record(CustomerChange.deleted(customer, Fixtures.NOW.plusSeconds(120)));

        PageResult<CustomerChange> page = useCase.execute(new CustomerHistoryQuery(customer.getId(), 0, 2));

        assertThat(page.items()).extracting(CustomerChange::type)
                .containsExactly(ChangeType.DELETED, ChangeType.RENAMED);
        assertThat(page.totalItems()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void shouldFailWhenCustomerNeverExisted() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.execute(new CustomerHistoryQuery(id, 0, 20)))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void queryShouldClampPaging() {
        CustomerHistoryQuery query = new CustomerHistoryQuery(UUID.randomUUID(), -3, 500);

        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(CustomerHistoryQuery.MAX_SIZE);
    }
}
