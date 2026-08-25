package com.psychotests.psycho_tests_server.controller;

import com.psychotests.psycho_tests_server.dto.request.RegisteringPsychologicalAccountRequestDto;
import com.psychotests.psycho_tests_server.dto.response.RegisteringPsychologicalAccountResponseDto;
import com.psychotests.psycho_tests_server.service.PsychologistAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/psychologists")
@RequiredArgsConstructor
public class PsychologistAccountController {
    private final PsychologistAccountService service;

    @PostMapping("/register")
    public ResponseEntity<RegisteringPsychologicalAccountResponseDto> register(@Valid @RequestBody RegisteringPsychologicalAccountRequestDto dto){
        var psychologist = service.createAccountPsychologist(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RegisteringPsychologicalAccountResponseDto.from(psychologist));
    }
}
