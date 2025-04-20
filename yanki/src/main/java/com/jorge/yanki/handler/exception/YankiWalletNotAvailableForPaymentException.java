package com.jorge.yanki.handler.exception;

public class YankiWalletNotAvailableForPaymentException extends RuntimeException {
    public YankiWalletNotAvailableForPaymentException() {
    }

    public YankiWalletNotAvailableForPaymentException(String message) {
        super(message);
    }

    public YankiWalletNotAvailableForPaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
