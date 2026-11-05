package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.model.UniqueSurveyCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface UniqueSurveyCodeRepository extends JpaRepository<UniqueSurveyCode, UUID> {

    /** Сколько кодов выдано в запуске — ожидаемое число респондентов. */
    @Query("""
        select count(c) from UniqueSurveyCode c
        where c.run.idRun = :idRun
        """)
    long countByRunIdRun(@Param("idRun") UUID idRun);

}
