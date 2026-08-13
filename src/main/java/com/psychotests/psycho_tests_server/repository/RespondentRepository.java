package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.Respondent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RespondentRepository extends JpaRepository<Respondent, UUID> {
}
