package com.psychotests.psycho_tests_server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/status/html")
    public String getStatus(){
        return "Сервер готов!!!";
    }
}
