package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.Research;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ResearchRepository extends JpaRepository<Research, UUID> {
}
