package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.SurveySession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SurveySessionRepository extends JpaRepository<SurveySession, UUID> {
}
