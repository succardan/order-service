package com.orderservice.exception;

public class DuplicateOrderException extends Exception {

    public DuplicateOrderException(String message) {
        super(message);
    }

}