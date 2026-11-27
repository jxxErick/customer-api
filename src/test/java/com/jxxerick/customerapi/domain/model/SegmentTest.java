package com.jxxerick.customerapi.domain.model;

import com.jxxerick.customerapi.domain.exception.DomainValidationException;
import com.jxxerick.customerapi.support.Fixtures;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SegmentTest {

    @Test
    void filterShouldNormalizeBlankFieldsAndState() {
        CustomerFilter filter = CustomerFilter.of("  ", List.of("VIP", " ", "Zona Sul"), " sp ", "  São Paulo ");

        assertThat(filter.nameContains()).isNull();
        assertThat(filter.tags()).containsExactlyInAnyOrder(new Tag("vip"), new Tag("zona-sul"));
        assertThat(filter.state()).isEqualTo("SP");
        assertThat(filter.city()).isEqualTo("São Paulo");
    }

    @Test
    void filterShouldRejectInvalidState() {
        assertThatThrownBy(() -> CustomerFilter.of(null, null, "São Paulo", null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("UF inválida");
    }

    @Test
    void emptyFilterMatchesEveryone() {
        assertThat(CustomerFilter.ALL.isEmpty()).isTrue();
        assertThat(CustomerFilter.byName(" ").isEmpty()).isTrue();
        assertThat(CustomerFilter.byName("maria").isEmpty()).isFalse();
    }

    @Test
    void segmentShouldRequireAtLeastOneCriterion() {
        assertThatThrownBy(() -> Segment.create("Todos", CustomerFilter.ALL, Fixtures.NOW))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("ao menos um critério");
    }

    @Test
    void segmentShouldTrimAndLimitName() {
        CustomerFilter filter = new CustomerFilter(null, Set.of(new Tag("vip")), null, null);

        assertThat(Segment.create("  VIPs  ", filter, Fixtures.NOW).name()).isEqualTo("VIPs");
        assertThatThrownBy(() -> Segment.create("x".repeat(61), filter, Fixtures.NOW))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> Segment.create(" ", filter, Fixtures.NOW))
                .isInstanceOf(DomainValidationException.class);
    }
}
