package com.example.RetailService.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class InvalidDatesException extends RuntimeException{
    public InvalidDatesException(){
        super("To date cannot be before from date");
    }
}
