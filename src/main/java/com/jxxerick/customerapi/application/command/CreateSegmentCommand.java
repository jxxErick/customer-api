package com.jxxerick.customerapi.application.command;

import java.util.List;

/** Raw criteria as typed; tags are normalized and validated by the use case. */
public record CreateSegmentCommand(String name, String nameContains, List<String> tags, String state, String city) {

    public CreateSegmentCommand {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
