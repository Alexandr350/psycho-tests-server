package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.AnswersItemsSubs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AnswersItemsSubRepository extends JpaRepository<AnswersItemsSubs, UUID> {

    @Query("""
        select ais from AnswersItemsSubs ais
        join fetch ais.itemsSub
        where ais.surveySession.survey.idSurvey = :idSurvey
          and (:idRun is null or ais.surveySession.run.id = :idRun)
        """)
    List<AnswersItemsSubs> findAllItemsSubBySurvey(@Param("idSurvey") UUID idSurvey,
                                                   @Param("idRun") UUID idRun);

    /** Все дополнительные изображения опроса, все волны — контракт по умолчанию. */
    default List<AnswersItemsSubs> findAllItemsSubBySurvey(UUID idSurvey) {
        return findAllItemsSubBySurvey(idSurvey, null);
    }
}

