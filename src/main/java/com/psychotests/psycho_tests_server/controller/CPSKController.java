package com.psychotests.psycho_tests_server.controller;


import com.psychotests.psycho_tests_server.dto.CPSKClickRequest;
import com.psychotests.psycho_tests_server.dto.CPSKClickResponse;
import com.psychotests.psycho_tests_server.service.CPSK_Logic;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CPSKController {

    private final CPSK_Logic cpsk_Logic;

    public  CPSKController(CPSK_Logic cpsk){
        this.cpsk_Logic = cpsk;
    }

    @PostMapping("/move")
    public ResponseEntity<CPSKClickResponse> handleMove(@RequestBody CPSKClickRequest request){
        int number = request.getNumberSector();
        String imageURL = cpsk_Logic.getNextImagePath();
        System.out.println(number);
       
    
        return ResponseEntity.ok(new CPSKClickResponse(imageURL));
    }
}
