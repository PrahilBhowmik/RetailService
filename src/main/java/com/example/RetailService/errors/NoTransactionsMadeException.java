package com.example.RetailService.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NO_CONTENT)
public class NoTransactionsMadeException extends RuntimeException{
    public NoTransactionsMadeException(){
        super("No transactions made by the user");
    }
}
