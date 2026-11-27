package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.domain.model.SegmentSummary;
import com.jxxerick.customerapi.domain.port.CustomerRepository;
import com.jxxerick.customerapi.domain.port.SegmentRepository;

import java.util.List;

/** Every segment with its live customer count. */
public class ListSegmentsUseCase {

    private final SegmentRepository segmentRepository;
    private final CustomerRepository customerRepository;

    public ListSegmentsUseCase(SegmentRepository segmentRepository, CustomerRepository customerRepository) {
        this.segmentRepository = segmentRepository;
        this.customerRepository = customerRepository;
    }

    public List<SegmentSummary> execute() {
        return segmentRepository.findAll().stream()
                .map(segment -> new SegmentSummary(segment, customerRepository.countMatching(segment.filter())))
                .toList();
    }
}
