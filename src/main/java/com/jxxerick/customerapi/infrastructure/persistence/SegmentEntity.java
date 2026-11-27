package com.jxxerick.customerapi.infrastructure.persistence;

import com.jxxerick.customerapi.domain.model.CustomerFilter;
import com.jxxerick.customerapi.domain.model.Segment;
import com.jxxerick.customerapi.domain.model.Tag;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "segments")
public class SegmentEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    @Column(name = "name_contains")
    private String nameContains;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "segment_tags", joinColumns = @JoinColumn(name = "segment_id"))
    @Column(name = "tag", nullable = false, length = 30)
    private Set<String> tags = new HashSet<>();

    @Column(length = 2)
    private String state;

    private String city;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SegmentEntity() {
    }

    static SegmentEntity from(Segment segment) {
        SegmentEntity entity = new SegmentEntity();
        entity.id = segment.id();
        entity.name = segment.name();
        entity.nameContains = segment.filter().nameContains();
        entity.tags = segment.filter().tags().stream().map(Tag::value).collect(Collectors.toCollection(HashSet::new));
        entity.state = segment.filter().state();
        entity.city = segment.filter().city();
        entity.createdAt = segment.createdAt();
        return entity;
    }

    Segment toDomain() {
        Set<Tag> filterTags = tags.stream().map(Tag::new).collect(Collectors.toSet());
        return new Segment(id, name, new CustomerFilter(nameContains, filterTags, state, city), createdAt);
    }
}
