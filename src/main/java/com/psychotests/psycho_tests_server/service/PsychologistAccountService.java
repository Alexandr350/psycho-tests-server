package com.psychotests.psycho_tests_server.service;

import com.psychotests.psycho_tests_server.dto.request.authorization_dto.LoginPsychologicalAccountRequestDto;
import com.psychotests.psycho_tests_server.dto.request.authorization_dto.RegisteringPsychologicalAccountRequestDto;
import com.psychotests.psycho_tests_server.exception.EmailAlreadyExistsException;
import com.psychotests.psycho_tests_server.exception.InvalidCredentialsException;
import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import com.psychotests.psycho_tests_server.repository.PsychologistAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.UUID;



@Service
@RequiredArgsConstructor
public class PsychologistAccountService {

    private final PsychologistAccountRepository psychologistAccountRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Фиктивный BCrypt-хэш (cost 10) для несуществующих email.
     * Гарантирует, что время ответа на «email не найден» совпадает со временем
     * ответа на «неверный пароль» — по таймингу нельзя перечислить email'ы.
     * **/
    private static final String DUMMY_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";


    public PsychologistAccount createAccountPsychologist(RegisteringPsychologicalAccountRequestDto dto){
        String email = dto.emailPsychologist().toLowerCase().trim();

        boolean emailTaken = psychologistAccountRepository.existsByEmailPsychologist(email);

        /*Хэширование выполняется ВСЕГДА до проверки — иначе тайминг ответа раскрывает, какие email'ы уже заняты.*/
        String passwordHash = passwordEncoder.encode(dto.passwordPsychologist());

        if(emailTaken){
           throw new EmailAlreadyExistsException("Психолог с таким email уже зарегистрирован");
        }

        var psychologist = new PsychologistAccount(
                UUID.randomUUID(),
                dto.namePsychologist(),
                dto.lastnamePsychologist(),
                passwordHash,
                email
        );

        try {
            return psychologistAccountRepository.save(psychologist);
        }catch (DataIntegrityViolationException ex){
            throw new EmailAlreadyExistsException("Психолог с таким email уже зарегистрирован");
        }

    }

    public PsychologistAccount authorizationAccount(LoginPsychologicalAccountRequestDto dto){
        String email = dto.email().toLowerCase().trim();

        var account = psychologistAccountRepository.findByEmailPsychologist(email).orElse(null);

        boolean matches = passwordEncoder.matches(dto.password(), account != null ? account.getPasswordPsychologist() : DUMMY_HASH);

        /*matches вызывается ВСЕГДА — с реальным хэшем или с заглушкой.
         * Не превращать в `account != null && matches(...)`: short-circuit
         * вернёт защиту на время выполнения. */

        if(account == null || !matches){
            throw new InvalidCredentialsException("Неверный email или пароль");
        }

        return account;
    }

}
