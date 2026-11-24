package com.jxxerick.customerapi.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class FieldChangeEmbeddable {

    @Column(nullable = false, length = 40)
    private String field;

    @Column(length = 500)
    private String previous;

    @Column(name = "current_value", length = 500)
    private String current;

    protected FieldChangeEmbeddable() {
    }

    FieldChangeEmbeddable(String field, String previous, String current) {
        this.field = field;
        this.previous = previous;
        this.current = current;
    }

    String getField() {
        return field;
    }

    String getPrevious() {
        return previous;
    }

    String getCurrent() {
        return current;
    }
}
