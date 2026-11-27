package com.jxxerick.customerapi.web.dto;

import com.jxxerick.customerapi.domain.model.CustomerFilter;
import com.jxxerick.customerapi.domain.model.Segment;
import com.jxxerick.customerapi.domain.model.SegmentSummary;
import com.jxxerick.customerapi.domain.model.Tag;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** {@code customers} is the live count; null right after creation, when it was not computed. */
public record SegmentResponse(UUID id, String name, Criteria criteria, Long customers, Instant createdAt) {

    public static SegmentResponse from(Segment segment) {
        return new SegmentResponse(segment.id(), segment.name(), Criteria.from(segment.filter()), null,
                segment.createdAt());
    }

    public static SegmentResponse from(SegmentSummary summary) {
        Segment segment = summary.segment();
        return new SegmentResponse(segment.id(), segment.name(), Criteria.from(segment.filter()),
                summary.customers(), segment.createdAt());
    }

    public record Criteria(String nameContains, List<String> tags, String state, String city) {

        static Criteria from(CustomerFilter filter) {
            return new Criteria(filter.nameContains(), filter.tags().stream().sorted().map(Tag::value).toList(),
                    filter.state(), filter.city());
        }
    }
}
