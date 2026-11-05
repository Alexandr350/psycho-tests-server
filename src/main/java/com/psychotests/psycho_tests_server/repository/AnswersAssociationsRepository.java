package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.AnswersAssociations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AnswersAssociationsRepository extends JpaRepository<AnswersAssociations, UUID> {

    @Query("""
            select aa from AnswersAssociations aa
            where aa.surveySession.survey.id = :idSurvey
              and (:idRun is null or aa.surveySession.run.id = :idRun)
            order by aa.associationOrder
            """)
    List<AnswersAssociations> findAllAssociationsBySurvey(@Param("idSurvey") UUID idSurvey,
                                                          @Param("idRun") UUID idRun);

    /** Все ассоциации опроса, все волны — контракт по умолчанию. */
    default List<AnswersAssociations> findAllAssociationsBySurvey(UUID idSurvey) {
        return findAllAssociationsBySurvey(idSurvey, null);
    }
}

