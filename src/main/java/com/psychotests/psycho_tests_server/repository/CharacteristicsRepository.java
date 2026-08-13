package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.Characteristics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CharacteristicsRepository extends JpaRepository<Characteristics, UUID> {
}
