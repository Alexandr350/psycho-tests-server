package com.psychotests.psycho_tests_server.security;

import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class PsychologistUserDetails implements UserDetails {
    private final PsychologistAccount psychologistAccount;



    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return psychologistAccount.getPasswordPsychologist();
    }

    @Override
    @NonNull
    public String getUsername() {
        return psychologistAccount.getNamePsychologist();
    }


    public PsychologistAccount getAccount (){
        return psychologistAccount;
    }
}
