package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.Research;
import com.psychotests.psycho_tests_server.model.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResearchRepository extends JpaRepository<Research, UUID> {

    @Query("""
        SELECT r FROM Research r
        WHERE r.IDResearch = :id
          AND r.psychologistAccount.idPsychologistAccount = :psychologistId
        """)
    Optional<Research> findByIdAndPsychologistAccountIdPsychologistAccount(
            @Param("id") UUID id,
            @Param("psychologistId") UUID psychologistId);


    @Query("""
    SELECT r FROM Research r
    WHERE r.psychologistAccount.idPsychologistAccount = :psychologistId
    """)
    List<Research> findAllByPsychologistAccountIdPsychologistAccount(
            @Param("psychologistId") UUID psychologistId);


}
