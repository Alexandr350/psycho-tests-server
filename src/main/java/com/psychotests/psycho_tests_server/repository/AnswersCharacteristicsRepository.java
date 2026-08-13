package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.AnswersCharacteristics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnswersCharacteristicsRepository extends JpaRepository<AnswersCharacteristics, UUID> {
}
