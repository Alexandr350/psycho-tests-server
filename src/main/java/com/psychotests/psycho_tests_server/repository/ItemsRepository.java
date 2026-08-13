package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.Items;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ItemsRepository extends JpaRepository<Items, UUID> {
}
