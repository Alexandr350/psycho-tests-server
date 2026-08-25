package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PsychologistAccountRepository extends JpaRepository<PsychologistAccount, UUID> {
    boolean existsByEmailPsychologist(String email);
}
