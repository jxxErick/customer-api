package com.jxxerick.customerapi.infrastructure.persistence;

import com.jxxerick.customerapi.domain.model.Segment;
import com.jxxerick.customerapi.domain.port.SegmentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapter from Spring Data JPA to the {@link SegmentRepository} port. */
@Repository
class JpaSegmentRepository implements SegmentRepository {

    private final SpringDataSegmentRepository jpa;

    JpaSegmentRepository(SpringDataSegmentRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Segment save(Segment segment) {
        return jpa.save(SegmentEntity.from(segment)).toDomain();
    }

    @Override
    public Optional<Segment> findById(UUID id) {
        return jpa.findById(id).map(SegmentEntity::toDomain);
    }

    @Override
    public List<Segment> findAll() {
        return jpa.findAllByOrderByNameAsc().stream().map(SegmentEntity::toDomain).toList();
    }

    @Override
    public boolean existsByName(String name) {
        return jpa.existsByNameIgnoreCase(name);
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
    }
}
