package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.Run;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RunRepository extends JpaRepository<Run, UUID> {

    /** Активный (незакрытый) запуск исследования — резолвится при создании
     * сессии и синхронизации запусков с изменением статуса исследования. */
    @Query("""
        select r from Run r
        where r.research.IDResearch = :idResearch
          and r.finishTimeRun is null
        """)
    Optional<Run> findOpenRun(@Param("idResearch") UUID idResearch);

    /** Запуск по публичному токену ссылки — вход для респондента
     * (resolvePublicToken) и для анонимного прохождения. */
    @Query("""
        select r from Run r
        where r.publicToken = :token
        """)
    Optional<Run> findByPublicToken(@Param("token") String token);

    /** Все запуски исследования по времени старта — для селектора волн в отчёте. */
    @Query("""
        select r from Run r
        where r.research.IDResearch = :idResearch
        order by r.startTimeRun asc
        """)
    List<Run> findAllByResearch(@Param("idResearch") UUID idResearch);


}
