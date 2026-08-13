package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.AnswersItemsSub;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnswersItemsSubRepository extends JpaRepository<AnswersItemsSub, UUID> {
}
