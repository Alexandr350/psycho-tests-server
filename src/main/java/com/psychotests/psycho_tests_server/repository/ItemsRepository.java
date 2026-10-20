package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.Items;
import com.psychotests.psycho_tests_server.model.Survey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemsRepository extends JpaRepository<Items, UUID> {
    List<Items> findBySurvey(Survey survey);
}
