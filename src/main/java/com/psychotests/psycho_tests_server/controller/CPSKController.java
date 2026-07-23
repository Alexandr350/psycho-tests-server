package com.psychotests.psycho_tests_server.controller;


import com.psychotests.psycho_tests_server.dto.CPSKClickRequest;
import com.psychotests.psycho_tests_server.dto.CPSKClickResponse;
import com.psychotests.psycho_tests_server.service.CPSK;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CPSKController {

    private final CPSK cpsk;

    public  CPSKController(CPSK cpsk){
        this.cpsk = cpsk;
    }

    @PostMapping("/move")
    public ResponseEntity<CPSKClickResponse> handleMove(@RequestBody CPSKClickRequest request){
        int number = request.getNumberSector();
        System.out.println(number);
        cpsk.countClick(number);
        String imagePath = "/image/2.jpg";
        return ResponseEntity.ok(new CPSKClickResponse(imagePath));
    }
}
