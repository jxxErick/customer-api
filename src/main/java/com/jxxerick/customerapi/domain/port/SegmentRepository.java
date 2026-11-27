package com.jxxerick.customerapi.domain.port;

import com.jxxerick.customerapi.domain.model.Segment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SegmentRepository {

    Segment save(Segment segment);

    Optional<Segment> findById(UUID id);

    /** Ordered by name. */
    List<Segment> findAll();

    /** Case-insensitive. */
    boolean existsByName(String name);

    void delete(UUID id);
}
