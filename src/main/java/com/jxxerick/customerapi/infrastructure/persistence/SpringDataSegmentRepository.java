package com.jxxerick.customerapi.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataSegmentRepository extends JpaRepository<SegmentEntity, UUID> {

    boolean existsByNameIgnoreCase(String name);

    List<SegmentEntity> findAllByOrderByNameAsc();
}
