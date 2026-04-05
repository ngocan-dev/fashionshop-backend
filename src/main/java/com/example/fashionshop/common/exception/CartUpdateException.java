package com.example.fashionshop.common.exception;

public class CartUpdateException extends RuntimeException {
    public CartUpdateException() {
        super("Unable to add item to cart");
    }

    public CartUpdateException(String message) {
        super(message);
    }

    public CartUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
