package com.psychotests.psycho_tests_server.service;

import com.psychotests.psycho_tests_server.exception.UnauthorizedException;
import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import com.psychotests.psycho_tests_server.security.PsychologistUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentPsychologistProvider {

    public PsychologistAccount getPsychologistAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Пользователь не авторизован");
        }
        if (!(authentication.getPrincipal() instanceof PsychologistUserDetails details)) {
            throw new UnauthorizedException("Нет аккаунта");
        }
        return details.getAccount();
    }
}
