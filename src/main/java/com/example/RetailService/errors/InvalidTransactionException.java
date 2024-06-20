package com.example.RetailService.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class InvalidTransactionException extends RuntimeException{
    public InvalidTransactionException(){
        super("Transaction type is not valid");
    }
}
