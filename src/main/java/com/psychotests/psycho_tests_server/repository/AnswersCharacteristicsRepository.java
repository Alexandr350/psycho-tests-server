package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.AnswersCharacteristics;
import com.psychotests.psycho_tests_server.projection.SectorCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AnswersCharacteristicsRepository extends JpaRepository<AnswersCharacteristics, UUID> {

    @Query("""
            select ac.numberSector as numberSector,
                   count(distinct ac.surveySession.id) as count
            from AnswersCharacteristics ac
            where ac.characteristic.id = :idCharacteristic
              and ac.surveySession.survey.id = :idSurvey
              and (:idRun is null or ac.surveySession.run.id = :idRun)
            group by ac.numberSector
            order by ac.numberSector
            """)
    List<SectorCountProjection> countSectorsByCharacteristic(@Param("idCharacteristic") UUID idCharacteristic,
                                                             @Param("idSurvey") UUID idSurvey,
                                                             @Param("idRun") UUID idRun);

    /** Попадания ВСЕХ характеристик по секторам — для режима «весь опрос». */
    @Query("""
            select ac.numberSector as numberSector, count(ac) as count
            from AnswersCharacteristics ac
            where ac.surveySession.survey.id = :idSurvey
              and (:idRun is null or ac.surveySession.run.id = :idRun)
            group by ac.numberSector
            """)
    List<SectorCountProjection> countSectorHitsBySurvey(@Param("idSurvey") UUID idSurvey,
                                                        @Param("idRun") UUID idRun);

    @Query("""
            select ac from AnswersCharacteristics ac
            join fetch ac.characteristic
            where ac.surveySession.survey.id = :idSurvey
              and (:idRun is null or ac.surveySession.run.id = :idRun)
            """)
    List<AnswersCharacteristics> findAllCharacteristicsBySurvey(@Param("idSurvey") UUID idSurvey,
                                                                @Param("idRun") UUID idRun);

    /** Все характеристики опроса, все волны — контракт по умолчанию. */
    default List<AnswersCharacteristics> findAllCharacteristicsBySurvey(UUID idSurvey) {
        return findAllCharacteristicsBySurvey(idSurvey, null);
    }
    /** Подсчёт по секторам характеристики, все волны — контракт по умолчанию. */
    default List<SectorCountProjection> countSectorsByCharacteristic(UUID idCharacteristic,
                                                                     UUID idSurvey) {
        return countSectorsByCharacteristic(idCharacteristic, idSurvey, null);
    }

}


