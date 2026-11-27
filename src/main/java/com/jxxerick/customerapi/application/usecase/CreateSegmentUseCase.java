package com.jxxerick.customerapi.application.usecase;

import com.jxxerick.customerapi.application.command.CreateSegmentCommand;
import com.jxxerick.customerapi.domain.exception.SegmentNameInUseException;
import com.jxxerick.customerapi.domain.model.CustomerFilter;
import com.jxxerick.customerapi.domain.model.Segment;
import com.jxxerick.customerapi.domain.port.SegmentRepository;

import java.time.Clock;

public class CreateSegmentUseCase {

    private final SegmentRepository segmentRepository;
    private final Clock clock;

    public CreateSegmentUseCase(SegmentRepository segmentRepository, Clock clock) {
        this.segmentRepository = segmentRepository;
        this.clock = clock;
    }

    /** Names are unique ignoring case. */
    public Segment execute(CreateSegmentCommand command) {
        CustomerFilter filter = CustomerFilter.of(command.nameContains(), command.tags(), command.state(),
                command.city());
        Segment segment = Segment.create(command.name(), filter, clock.instant());
        if (segmentRepository.existsByName(segment.name())) {
            throw new SegmentNameInUseException(segment.name());
        }
        return segmentRepository.save(segment);
    }
}
