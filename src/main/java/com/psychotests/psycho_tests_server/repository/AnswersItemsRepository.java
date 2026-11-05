package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.AnswersItems;
import com.psychotests.psycho_tests_server.projection.SectorCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AnswersItemsRepository extends JpaRepository<AnswersItems, UUID> {

    @Query("""
            select ai.numberSector as numberSector,
                   count(distinct ai.surveySession.id) as count
            from AnswersItems ai
            where ai.item.id = :idItem
              and ai.surveySession.survey.id = :idSurvey
              and (:idRun is null or ai.surveySession.run.id = :idRun)
            group by ai.numberSector
            order by ai.numberSector
            """)
    List<SectorCountProjection> countSectorsByItem(@Param("idItem") UUID idItem,
                                                   @Param("idSurvey") UUID idSurvey,
                                                   @Param("idRun") UUID idRun);

    @Query("""
            select ai.numberSector as numberSector, count(ai) as count
            from AnswersItems ai
            where ai.surveySession.survey.id = :idSurvey
              and (:idRun is null or ai.surveySession.run.id = :idRun)
            group by ai.numberSector
            """)
    List<SectorCountProjection> countSectorHitsBySurvey(@Param("idSurvey") UUID idSurvey,
                                                        @Param("idRun") UUID idRun);

    @Query("""
            select ai from AnswersItems ai
            join fetch ai.item
            where ai.surveySession.survey.id = :idSurvey
              and (:idRun is null or ai.surveySession.run.id = :idRun)
            """)
    List<AnswersItems> findAllItemsBySurvey(@Param("idSurvey") UUID idSurvey,
                                            @Param("idRun") UUID idRun);

    /** Подсчёт по секторам изображения, все волны — контракт по умолчанию. */
    default List<SectorCountProjection> countSectorsByItem(UUID idItem,
                                                           UUID idSurvey) {
        return countSectorsByItem(idItem, idSurvey, null);
    }

    /** Все изображения опроса, все волны — контракт по умолчанию. */
    default List<AnswersItems> findAllItemsBySurvey(UUID idSurvey) {
        return findAllItemsBySurvey(idSurvey, null);
    }
}
