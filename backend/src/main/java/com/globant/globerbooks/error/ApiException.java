package com.globant.globerbooks.error;

/**
 * Base of the service exceptions that carry an error code and so an HTTP status. The advice turns it
 * into the error envelope through the body factory. The message is the code name, never user text.
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    /**
     * Creates the exception.
     *
     * @param code the error code, which decides the status
     */
    public ApiException(ErrorCode code) {
        super(code.name());
        this.code = code;
    }

    /**
     * Returns the error code.
     *
     * @return the code
     */
    public ErrorCode code() {
        return code;
    }

    /**
     * Returns the HTTP status of the code.
     *
     * @return the status
     */
    public int status() {
        return code.status();
    }
}
