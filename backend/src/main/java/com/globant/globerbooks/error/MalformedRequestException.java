package com.globant.globerbooks.error;

/** Raised when a request is well-formed HTTP but breaks a transport rule; maps to {@code MALFORMED_REQUEST}. */
public final class MalformedRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Creates the exception with a fixed message that holds no request data. */
    public MalformedRequestException() {
        super("malformed request");
    }
}
