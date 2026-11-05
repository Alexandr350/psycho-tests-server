package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.Research;
import com.psychotests.psycho_tests_server.model.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Optional;
import java.util.UUID;

public interface SurveyRepository extends JpaRepository<Survey, UUID> {

    @Query("""
        SELECT s FROM Survey s
        WHERE s.idSurvey = :id
          AND s.research.psychologistAccount.idPsychologistAccount = :psychologistId
        """)
    Optional<Survey> findByIdAndResearchPsychologistAccountIdPsychologistAccount(
            UUID id, UUID psychologistId);

    /** Опрос по исследованию — для сборки резюма в DTO. */
    @Query("""
        SELECT s FROM Survey s
        WHERE s.research.IDResearch = :idResearch
          AND s.research.psychologistAccount.idPsychologistAccount = :psychologistId
        """)
    Optional<Survey> findByResearchIdResearchAndPsychologistId(UUID idResearch, UUID psychologistId);

    /** Опрос по исследованию для публичного резолвера токена — без фильтра психолога. */
    @Query("""
        SELECT s FROM Survey s
        WHERE s.research.IDResearch = :idResearch
        """)
    Optional<Survey> findByResearchIdResearch(UUID idResearch);


}

