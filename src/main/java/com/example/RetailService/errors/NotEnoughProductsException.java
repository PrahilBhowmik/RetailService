package com.example.RetailService.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class NotEnoughProductsException extends RuntimeException{

    public NotEnoughProductsException(){
        super("Not enough/No products to sell/return");
    }
}
