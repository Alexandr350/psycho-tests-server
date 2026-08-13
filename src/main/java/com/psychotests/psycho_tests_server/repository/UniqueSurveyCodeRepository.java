package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.UniqueSurveyCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UniqueSurveyCodeRepository extends JpaRepository<UniqueSurveyCode, UUID> {
}
