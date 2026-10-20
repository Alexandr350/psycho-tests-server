package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.Characteristics;
import com.psychotests.psycho_tests_server.model.Survey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CharacteristicsRepository extends JpaRepository<Characteristics, UUID> {
    List<Characteristics> findBySurvey(Survey survey);
}
