package com.autoboxex.exception;

import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {
    private final String mismatch;

    public AuthException(String mismatch, String message) {
        super(message);
        this.mismatch = mismatch;
    }

    public AuthException(String message) {
        this(null, message);
    }
}
