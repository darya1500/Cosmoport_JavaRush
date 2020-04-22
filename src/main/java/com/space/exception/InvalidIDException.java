package com.space.exception;

public class InvalidIDException extends Exception {
    public InvalidIDException(String message) {
        super(message);
    }

    public InvalidIDException(String message, Exception e) {
    }
}
