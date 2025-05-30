package com.joel.br.AutoClipster.execption;

public class RateLimitExceededException extends RuntimeException{


    public RateLimitExceededException(String message) {
        super(message);
    }

    public RateLimitExceededException(String message , Throwable error) {
        super(message, error);
    }
}
