package com.jxxerick.customerapi.web;

class InvalidIfMatchException extends RuntimeException {

    InvalidIfMatchException(String header) {
        super("Cabeçalho If-Match inválido: " + header + ". Use o ETag devolvido pela API, ex.: \"3\"");
    }
}
