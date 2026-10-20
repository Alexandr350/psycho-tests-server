package com.psychotests.psycho_tests_server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResearchNotFoundException  extends RuntimeException{
    public ResearchNotFoundException(String message){
        super(message);
    }
}
