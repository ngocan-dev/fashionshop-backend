package com.example.fashionshop.common.exception;

public class OrderDetailLoadException extends RuntimeException {
    public OrderDetailLoadException() {
        super("Unable to load order details");
    }

    public OrderDetailLoadException(Throwable cause) {
        super("Unable to load order details", cause);
    }
}
