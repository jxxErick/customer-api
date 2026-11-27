package com.jxxerick.customerapi.web.dto;

import com.jxxerick.customerapi.domain.model.TagCount;

public record TagCountResponse(String tag, long customers) {

    public static TagCountResponse from(TagCount count) {
        return new TagCountResponse(count.tag(), count.customers());
    }
}
