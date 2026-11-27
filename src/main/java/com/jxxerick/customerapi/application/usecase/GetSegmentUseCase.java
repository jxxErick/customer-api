package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.domain.exception.SegmentNotFoundException;
import com.jxxerick.customerapi.domain.model.Segment;
import com.jxxerick.customerapi.domain.port.SegmentRepository;

import java.util.UUID;

public class GetSegmentUseCase {

    private final SegmentRepository segmentRepository;

    public GetSegmentUseCase(SegmentRepository segmentRepository) {
        this.segmentRepository = segmentRepository;
    }

    public Segment execute(UUID id) {
        return segmentRepository.findById(id).orElseThrow(() -> new SegmentNotFoundException(id));
    }
}
