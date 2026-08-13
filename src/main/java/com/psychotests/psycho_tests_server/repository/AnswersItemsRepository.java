package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.AnswersItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnswersItemsRepository extends JpaRepository<AnswersItems, UUID> {
}
