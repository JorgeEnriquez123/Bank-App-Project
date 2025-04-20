package com.jorge.yanki.handler;

import com.jorge.yanki.handler.exception.ResourceNotFoundException;
import com.jorge.yanki.handler.exception.YankiWalletNotAvailableForPaymentException;
import com.jorge.yanki.handler.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ErrorResponse resourceNotFoundExceptionHandler(ResourceNotFoundException ex){
        log.error("Resource not found");
        log.debug("Exception Details: ", ex);
        return ErrorResponse.builder()
                .errorCode(HttpStatus.NOT_FOUND.toString())
                .message(ex.getMessage())
                .build();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(YankiWalletNotAvailableForPaymentException.class)
    public ErrorResponse yankiWalletNotAvailableForPaymentExceptionHandler(YankiWalletNotAvailableForPaymentException ex){
        log.error("Yanki Wallet not available for payment");
        log.debug("Exception Details: ", ex);
        return ErrorResponse.builder()
                .errorCode(HttpStatus.BAD_REQUEST.toString())
                .message(ex.getMessage())
                .build();
    }
}
