package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.BulkTagCommand;
import com.jxxerick.customerapi.application.usecase.BulkTagCustomersUseCase.Item;
import com.jxxerick.customerapi.application.usecase.BulkTagCustomersUseCase.Outcome;
import com.jxxerick.customerapi.application.usecase.BulkTagCustomersUseCase.Report;
import com.jxxerick.customerapi.domain.exception.ConcurrentUpdateException;
import com.jxxerick.customerapi.domain.exception.DomainValidationException;
import com.jxxerick.customerapi.domain.model.ChangeType;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.CustomerChange;
import com.jxxerick.customerapi.domain.model.CustomerFilter;
import com.jxxerick.customerapi.domain.model.Email;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.model.Segment;
import com.jxxerick.customerapi.domain.model.Tag;
import com.jxxerick.customerapi.domain.port.CustomerHistory;
import com.jxxerick.customerapi.domain.port.CustomerRepository;
import com.jxxerick.customerapi.domain.port.SegmentRepository;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkTagCustomersUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Fixtures.NOW, ZoneOffset.UTC);

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SegmentRepository segmentRepository;

    @Mock
    private CustomerHistory history;

    private BulkTagCustomersUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new BulkTagCustomersUseCase(new GetSegmentUseCase(segmentRepository), customerRepository, history,
                CLOCK);
        lenient().when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void shouldAddAndRemoveKeepingOtherTagsAndRecordHistory() {
        Customer maria = customer("maria", "inativo", "zona-sul");
        when(customerRepository.findById(maria.getId())).thenReturn(Optional.of(maria));

        Report report = useCase.execute(BulkTagCommand.forCustomers(List.of(maria.getId()), List.of("VIP"),
                List.of("inativo")));

        assertThat(report.updated()).isEqualTo(1);
        assertThat(maria.getTags()).containsExactly(new Tag("vip"), new Tag("zona-sul"));
        ArgumentCaptor<CustomerChange> change = ArgumentCaptor.forClass(CustomerChange.class);
        verify(history).record(change.capture());
        assertThat(change.getValue().type()).isEqualTo(ChangeType.TAGS_CHANGED);
        assertThat(change.getValue().fields().getFirst().previous()).isEqualTo("inativo, zona-sul");
    }

    @Test
    void shouldReportEachCustomerOnItsOwn() {
        Customer alreadyVip = customer("ana", "vip");
        Customer full = customer("bia", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10");
        Customer stale = customer("caio");
        UUID missing = UUID.randomUUID();
        when(customerRepository.findById(alreadyVip.getId())).thenReturn(Optional.of(alreadyVip));
        when(customerRepository.findById(full.getId())).thenReturn(Optional.of(full));
        when(customerRepository.findById(stale.getId())).thenReturn(Optional.of(stale));
        when(customerRepository.findById(missing)).thenReturn(Optional.empty());
        when(customerRepository.save(stale)).thenThrow(new ConcurrentUpdateException(stale.getId()));

        Report report = useCase.execute(BulkTagCommand.forCustomers(
                List.of(alreadyVip.getId(), full.getId(), stale.getId(), missing, missing), List.of("vip"), null));

        assertThat(report.items()).extracting(Item::outcome)
                .containsExactly(Outcome.UNCHANGED, Outcome.REJECTED, Outcome.CONFLICT, Outcome.NOT_FOUND);
        assertThat(report.total()).isEqualTo(4);
        assertThat(report.unchanged()).isEqualTo(1);
        assertThat(report.failed()).isEqualTo(3);
        assertThat(report.items().get(1).message()).contains("no máximo 10 tags");
        assertThat(full.getTags()).hasSize(10).doesNotContain(new Tag("vip"));
        verifyNoInteractions(history);
    }

    @Test
    void shouldCollectSegmentMembersBeforeChangingThem() {
        CustomerFilter filter = new CustomerFilter(null, Set.of(new Tag("inativo")), null, null);
        Segment inactive = Segment.create("Inativos", filter, Fixtures.NOW);
        List<Customer> members = IntStream.range(0, 150).mapToObj(i -> customer("c" + i, "inativo")).toList();
        when(segmentRepository.findById(inactive.id())).thenReturn(Optional.of(inactive));
        when(customerRepository.countMatching(filter)).thenReturn(150L);
        when(customerRepository.findMatching(filter, 0, 100))
                .thenReturn(new PageResult<>(members.subList(0, 100), 0, 100, 150));
        when(customerRepository.findMatching(filter, 1, 100))
                .thenReturn(new PageResult<>(members.subList(100, 150), 1, 100, 150));
        members.forEach(c -> when(customerRepository.findById(c.getId())).thenReturn(Optional.of(c)));

        Report report = useCase.execute(BulkTagCommand.forSegment(inactive.id(), null, List.of("inativo")));

        assertThat(report.updated()).isEqualTo(150);
        verify(customerRepository, never()).findMatching(eq(filter), eq(2), anyInt());
    }

    @Test
    void shouldRefuseSegmentsAboveTheLimit() {
        CustomerFilter filter = new CustomerFilter(null, Set.of(), "SP", null);
        Segment sp = Segment.create("SP", filter, Fixtures.NOW);
        when(segmentRepository.findById(sp.id())).thenReturn(Optional.of(sp));
        when(customerRepository.countMatching(filter)).thenReturn(1001L);

        assertThatThrownBy(() -> useCase.execute(BulkTagCommand.forSegment(sp.id(), List.of("sp"), null)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("1001");
        verify(customerRepository, never()).findMatching(any(), anyInt(), anyInt());
    }

    @Test
    void shouldValidateTheRequestBeforeTouchingAnyCustomer() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.execute(BulkTagCommand.forCustomers(List.of(id), List.of(), List.of())))
                .hasMessageContaining("adicionar ou remover");
        assertThatThrownBy(() -> useCase.execute(BulkTagCommand.forCustomers(List.of(id), List.of("VIP"),
                List.of("vip")))).hasMessageContaining("adicionada e removida");
        assertThatThrownBy(() -> useCase.execute(BulkTagCommand.forCustomers(List.of(id), List.of("vip!"), null)))
                .hasMessageContaining("Tag inválida");
        assertThatThrownBy(() -> useCase.execute(BulkTagCommand.of(List.of(id), UUID.randomUUID(), List.of("vip"),
                null))).hasMessageContaining("um dos dois");
        assertThatThrownBy(() -> useCase.execute(BulkTagCommand.forCustomers(List.of(), List.of("vip"), null)))
                .hasMessageContaining("um dos dois");
        verifyNoInteractions(customerRepository, history);
    }

    private static Customer customer(String name, String... tags) {
        Customer customer = Customer.create(name, new Email(name + "@example.com"), Fixtures.address(), Fixtures.NOW);
        customer.retag(Arrays.stream(tags).map(Tag::new).collect(Collectors.toSet()));
        return customer;
    }
}
