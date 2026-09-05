package com.psychotests.psycho_tests_server.controller;

import com.psychotests.psycho_tests_server.dto.request.LoginPsychologicalAccountRequestDto;
import com.psychotests.psycho_tests_server.dto.request.RegisteringPsychologicalAccountRequestDto;
import com.psychotests.psycho_tests_server.dto.response.LoginPsychologicalAccountResponseDto;
import com.psychotests.psycho_tests_server.dto.response.RegisteringPsychologicalAccountResponseDto;
import com.psychotests.psycho_tests_server.security.PsychologistUserDetails;
import com.psychotests.psycho_tests_server.service.PsychologistAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
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
    public ResponseEntity<RegisteringPsychologicalAccountResponseDto> register(
            @Valid @RequestBody RegisteringPsychologicalAccountRequestDto dto,
            HttpServletRequest request){

        var psychologist = service.createAccountPsychologist(dto);
        authenticateUserInSession(psychologist, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(RegisteringPsychologicalAccountResponseDto.from(psychologist));
    }



    @PostMapping("/login")
    public ResponseEntity<LoginPsychologicalAccountResponseDto> login(
            @Valid @RequestBody LoginPsychologicalAccountRequestDto dto,
            HttpServletRequest request){
        var psychologist = service.authorizationAccount(dto);
        authenticateUserInSession(psychologist, request);
        return ResponseEntity.status(HttpStatus.OK).body(LoginPsychologicalAccountResponseDto.from(psychologist));

    }

    private void authenticateUserInSession(com.psychotests.psycho_tests_server.model.PsychologistAccount account, HttpServletRequest request) {
        PsychologistUserDetails userDetails = new PsychologistUserDetails(account);
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticationToken);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

    }
}
