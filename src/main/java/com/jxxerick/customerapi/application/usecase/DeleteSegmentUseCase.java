package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.domain.model.Segment;
import com.jxxerick.customerapi.domain.port.SegmentRepository;

import java.util.UUID;

/** Removes only the saved criteria; customers are untouched. */
public class DeleteSegmentUseCase {

    private final GetSegmentUseCase getSegment;
    private final SegmentRepository segmentRepository;

    public DeleteSegmentUseCase(GetSegmentUseCase getSegment, SegmentRepository segmentRepository) {
        this.getSegment = getSegment;
        this.segmentRepository = segmentRepository;
    }

    public void execute(UUID id) {
        Segment segment = getSegment.execute(id);
        segmentRepository.delete(segment.id());
    }
}
