package com.ub.banking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class AccountNotFound extends RuntimeException{
	private String message;
    public AccountNotFound(String Message){
        super(Message);
        this.message = Message;
         
    }
	
}
