package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.enums.SurveySessionStatus;
import com.psychotests.psycho_tests_server.model.SurveySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SurveySessionRepository extends JpaRepository<SurveySession, UUID> {

    /**
     * Проверяет, есть ли в опросе хотя бы одна незавершённая сессия.
     *
     * <p>Запрос выписан явно, потому что:
     * <ul>
     *   <li>производное имя метода не совпадает с полем сущности
     *       ({@code statusSurveySession});</li>
     *   <li>снимается NULL-ловушка производного {@code Not}:
     *       строка без статуса ({@code statusSurveySession is null})
     *       считается незавершённой и блокирует удаление.</li>
     * </ul>
     *
     * @param idSurvey идентификатор опроса
     * @param status   статус, который считается «завершённым»
     * @return {@code true}, если найдена сессия с другим статусом
     *         или без статуса
     */
    @Query("""
        select count(s) > 0 from SurveySession s
        where s.survey.idSurvey = :idSurvey
        and (s.statusSurveySession <> :status or s.statusSurveySession is null)
        """)
    boolean existsBySurveyIdSurveyAndStatusNot(
            @Param("idSurvey") UUID idSurvey,
            @Param("status") SurveySessionStatus status);

    /** Сессия респондента в конкретном запуске — для апсерта в createSession.
     * Запрос выписан явно: два FK-поля в одном методе читаются тяжелее,
     * чем JPQL, а поведение такое же. */
    @Query("""
        select s from SurveySession s
        where s.run.idRun = :idRun
        and s.respondent.idRespondent = :idRespondent
        """)
    Optional<SurveySession> findByRunIdRunAndRespondentIdRespondent(
            @Param("idRun") UUID idRun,
            @Param("idRespondent") UUID idRespondent);

    /**
     * Есть ли активные прохождения: сессии в статусе IN_PROGRESS,
     * начатые позже указанного момента (startTimeSession > cutoff).
     * Именно они блокируют удаление опроса — брошенные старые сессии
     * не мешают, они уйдут каскадом вместе с опросом.
     */
    @Query("""
        select count(ss) > 0 from SurveySession ss
        where ss.survey.idSurvey = :idSurvey
        and ss.statusSurveySession = :status
        and ss.startTimeSession > :cutoff
        """)
    boolean existsActiveSession(
            @Param("idSurvey") UUID idSurvey,
            @Param("status") SurveySessionStatus status,
            @Param("cutoff") LocalDateTime cutoff);

    /** Сколько сессий в запуске в заданном статусе. */
    @Query("""
        select count(ss) from SurveySession ss
        where ss.run.idRun = :idRun
        and ss.statusSurveySession = :status
        """)
    long countByRunIdRunAndStatusSurveySession(
            @Param("idRun") UUID idRun,
            @Param("status") SurveySessionStatus status);

    /** Число завершённых сессий, сгруппированных по опросу. */
    @Query("""
        select ss.survey.idSurvey, count(ss)
        from SurveySession ss
        where ss.statusSurveySession = :status
        group by ss.survey.idSurvey
        """)
    List<Object[]> countByStatusGroupedBySurvey(
            @Param("status") SurveySessionStatus status);
}
