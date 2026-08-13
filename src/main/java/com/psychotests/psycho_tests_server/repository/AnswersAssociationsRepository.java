package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.AnswersAssociations;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnswersAssociationsRepository extends JpaRepository<AnswersAssociations, UUID> {
}
