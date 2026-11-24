package com.jxxerick.customerapi.web.csv;

/** The file can't be read at all (as opposed to a single bad row). */
public class InvalidCsvException extends RuntimeException {

    public InvalidCsvException(String message) {
        super(message);
    }
}
