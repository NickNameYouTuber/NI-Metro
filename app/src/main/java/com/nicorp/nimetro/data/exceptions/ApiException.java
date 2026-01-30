package com.nicorp.nimetro.data.exceptions;

public class ApiException extends Exception {
    private final int statusCode;
    private final String message;

    public ApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

