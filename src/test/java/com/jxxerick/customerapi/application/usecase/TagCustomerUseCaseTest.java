package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.TagCustomerCommand;
import com.jxxerick.customerapi.domain.exception.ConcurrentUpdateException;
import com.jxxerick.customerapi.domain.exception.DomainValidationException;
import com.jxxerick.customerapi.domain.model.ChangeType;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.CustomerChange;
import com.jxxerick.customerapi.domain.model.FieldChange;
import com.jxxerick.customerapi.domain.model.Tag;
import com.jxxerick.customerapi.domain.model.TagCount;
import com.jxxerick.customerapi.domain.port.CustomerRepository;
import com.jxxerick.customerapi.support.Fixtures;
import com.jxxerick.customerapi.support.RecordingHistory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagCustomerUseCaseTest {

    @Mock
    private CustomerRepository customerRepository;

    private final RecordingHistory history = new RecordingHistory();

    private TagCustomerUseCase useCase() {
        return new TagCustomerUseCase(new GetCustomerUseCase(customerRepository), customerRepository, history,
                Clock.fixed(Fixtures.NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldReplaceTagsAndRecordTheChange() {
        Customer customer = Fixtures.storedCustomer(2);
        customer.retag(List.of(new Tag("vip")));
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);

        Customer saved = useCase().execute(new TagCustomerCommand(customer.getId(), List.of("Zona Sul", "VIP"), 2L));

        assertThat(saved.getTags()).extracting(Tag::value).containsExactly("vip", "zona-sul");
        assertThat(history.recorded()).singleElement().satisfies(change -> {
            assertThat(change.type()).isEqualTo(ChangeType.TAGS_CHANGED);
            assertThat(change.fields()).containsExactly(new FieldChange("tags", "vip", "vip, zona-sul"));
        });
    }

    @Test
    void sameTagsShouldLeaveNoHistory() {
        Customer customer = Fixtures.storedCustomer(0);
        customer.retag(List.of(new Tag("vip")));
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);

        useCase().execute(new TagCustomerCommand(customer.getId(), List.of(" VIP "), null));

        assertThat(history.recorded()).isEmpty();
    }

    @Test
    void emptyListShouldClearTags() {
        Customer customer = Fixtures.storedCustomer(0);
        customer.retag(List.of(new Tag("vip")));
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);

        Customer saved = useCase().execute(new TagCustomerCommand(customer.getId(), List.of(), null));

        assertThat(saved.getTags()).isEmpty();
        assertThat(history.recorded()).extracting(CustomerChange::fields)
                .containsExactly(List.of(new FieldChange("tags", "vip", null)));
    }

    @Test
    void invalidTagShouldFailBeforeLoadingTheCustomer() {
        Customer customer = Fixtures.storedCustomer(0);
        TagCustomerUseCase useCase = useCase();
        TagCustomerCommand command = new TagCustomerCommand(customer.getId(), List.of("ok", "não pode!"), null);

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(DomainValidationException.class);
        verifyNoInteractions(customerRepository);
    }

    @Test
    void outdatedVersionShouldFail() {
        Customer customer = Fixtures.storedCustomer(3);
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        TagCustomerUseCase useCase = useCase();
        TagCustomerCommand command = new TagCustomerCommand(customer.getId(), List.of("vip"), 1L);

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(ConcurrentUpdateException.class);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void listTagsShouldDelegateToRepository() {
        List<TagCount> counts = List.of(new TagCount("vip", 2));
        when(customerRepository.countByTag()).thenReturn(counts);

        assertThat(new ListTagsUseCase(customerRepository).execute()).isEqualTo(counts);
    }
}
