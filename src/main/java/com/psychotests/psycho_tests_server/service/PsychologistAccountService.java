package com.psychotests.psycho_tests_server.service;

import com.psychotests.psycho_tests_server.dto.RegisteringPsychologicalAccountRequestDto;
import com.psychotests.psycho_tests_server.exception.ConflictCreateAccountPsychologist;
import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import com.psychotests.psycho_tests_server.repository.PsychologistAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationPsychologistAccountService {

    private final PsychologistAccountRepository psychologistAccountRepository;
    private final PasswordEncoder passwordEncoder;

    private boolean isCheckEmail(String email){
        return psychologistAccountRepository.findByEmail(email);
    }

    private UUID createUUID(){
        return UUID.randomUUID();
    }

    private String createHashPassword(String password){
        return passwordEncoder.encode(password);
    }

    public PsychologistAccount createAccountPsychologist(RegisteringPsychologicalAccountRequestDto dto){

        if(isCheckEmail(dto.emailPsychologist())){
           throw new ConflictCreateAccountPsychologist("Аккаунт с таким email уже существует");
        }

        var psychologist = new PsychologistAccount(
                createUUID(),
                dto.namePsychologist(),
                dto.lastnamePsychologist(),
                createHashPassword(dto.passwordPsychologist()),
                dto.emailPsychologist()
        );

        return psychologistAccountRepository.save(psychologist);

    }
}
