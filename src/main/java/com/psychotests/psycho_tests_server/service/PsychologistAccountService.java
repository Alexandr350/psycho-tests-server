package com.psychotests.psycho_tests_server.service;

import com.psychotests.psycho_tests_server.dto.request.RegisteringPsychologicalAccountRequestDto;
import com.psychotests.psycho_tests_server.exception.ConflictCreateAccountPsychologist;
import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import com.psychotests.psycho_tests_server.repository.PsychologistAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.psychotests.psycho_tests_server.service.Utility.createUUID;

@Service
@RequiredArgsConstructor
public class PsychologistAccountService {

    private final PsychologistAccountRepository psychologistAccountRepository;
    private final PasswordEncoder passwordEncoder;


    private String createHashPassword(String password){
        return passwordEncoder.encode(password);
    }
    public boolean isCheckEmail(String email){
        return psychologistAccountRepository.existsByEmailPsychologist(email);
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
