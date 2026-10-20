package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.ItemsSub;
import com.psychotests.psycho_tests_server.model.Survey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemsSubRepository extends JpaRepository<ItemsSub, UUID> {
    List<ItemsSub> findBySurvey(Survey survey);
}
