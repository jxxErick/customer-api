package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.CreateSegmentCommand;
import com.jxxerick.customerapi.domain.exception.SegmentNameInUseException;
import com.jxxerick.customerapi.domain.exception.SegmentNotFoundException;
import com.jxxerick.customerapi.domain.model.Customer;
import com.jxxerick.customerapi.domain.model.CustomerFilter;
import com.jxxerick.customerapi.domain.model.PageResult;
import com.jxxerick.customerapi.domain.model.Segment;
import com.jxxerick.customerapi.domain.model.SegmentSummary;
import com.jxxerick.customerapi.domain.model.Tag;
import com.jxxerick.customerapi.domain.port.CustomerRepository;
import com.jxxerick.customerapi.domain.port.SegmentRepository;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SegmentUseCasesTest {

    private static final Clock CLOCK = Clock.fixed(Fixtures.NOW, ZoneOffset.UTC);

    @Mock
    private SegmentRepository segmentRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Test
    void createShouldSaveNormalizedCriteria() {
        when(segmentRepository.existsByName("VIPs SP")).thenReturn(false);
        when(segmentRepository.save(any(Segment.class))).thenAnswer(inv -> inv.getArgument(0));

        Segment segment = new CreateSegmentUseCase(segmentRepository, CLOCK)
                .execute(new CreateSegmentCommand(" VIPs SP ", null, List.of("VIP"), "sp", null));

        assertThat(segment.name()).isEqualTo("VIPs SP");
        assertThat(segment.filter()).isEqualTo(new CustomerFilter(null, Set.of(new Tag("vip")), "SP", null));
        assertThat(segment.createdAt()).isEqualTo(Fixtures.NOW);
    }

    @Test
    void createShouldRejectNameAlreadyTaken() {
        when(segmentRepository.existsByName("VIPs")).thenReturn(true);

        assertThatThrownBy(() -> new CreateSegmentUseCase(segmentRepository, CLOCK)
                .execute(new CreateSegmentCommand("VIPs", null, List.of("vip"), null, null)))
                .isInstanceOf(SegmentNameInUseException.class);
        verify(segmentRepository, never()).save(any());
    }

    @Test
    void listShouldCountCustomersOfEachSegment() {
        Segment vips = segment("VIPs", new CustomerFilter(null, Set.of(new Tag("vip")), null, null));
        Segment rio = segment("Rio", new CustomerFilter(null, Set.of(), "RJ", null));
        when(segmentRepository.findAll()).thenReturn(List.of(rio, vips));
        when(customerRepository.countMatching(rio.filter())).thenReturn(3L);
        when(customerRepository.countMatching(vips.filter())).thenReturn(12L);

        assertThat(new ListSegmentsUseCase(segmentRepository, customerRepository).execute())
                .containsExactly(new SegmentSummary(rio, 3), new SegmentSummary(vips, 12));
    }

    @Test
    void customersShouldUseSegmentFilterWithClampedPaging() {
        Segment rio = segment("Rio", new CustomerFilter(null, Set.of(), "RJ", null));
        PageResult<Customer> page = new PageResult<>(List.of(Fixtures.customer()), 0, 100, 1);
        when(segmentRepository.findById(rio.id())).thenReturn(Optional.of(rio));
        when(customerRepository.findMatching(rio.filter(), 0, 100)).thenReturn(page);

        ListSegmentCustomersUseCase useCase =
                new ListSegmentCustomersUseCase(new GetSegmentUseCase(segmentRepository), customerRepository);

        assertThat(useCase.execute(rio.id(), -1, 1000)).isEqualTo(page);
    }

    @Test
    void deleteShouldFailForUnknownSegment() {
        UUID id = UUID.randomUUID();
        when(segmentRepository.findById(id)).thenReturn(Optional.empty());

        DeleteSegmentUseCase useCase = new DeleteSegmentUseCase(new GetSegmentUseCase(segmentRepository),
                segmentRepository);

        assertThatThrownBy(() -> useCase.execute(id)).isInstanceOf(SegmentNotFoundException.class);
        verify(segmentRepository, never()).delete(any());
    }

    @Test
    void deleteShouldRemoveOnlyTheSegment() {
        Segment rio = segment("Rio", new CustomerFilter(null, Set.of(), "RJ", null));
        when(segmentRepository.findById(rio.id())).thenReturn(Optional.of(rio));

        new DeleteSegmentUseCase(new GetSegmentUseCase(segmentRepository), segmentRepository).execute(rio.id());

        verify(segmentRepository).delete(rio.id());
    }

    private static Segment segment(String name, CustomerFilter filter) {
        return Segment.create(name, filter, Fixtures.NOW);
    }
}
