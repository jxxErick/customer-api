package com.jxxerick.customerapi.domain.model;

/** A segment with how many customers match it right now. */
public record SegmentSummary(Segment segment, long customers) {
}
