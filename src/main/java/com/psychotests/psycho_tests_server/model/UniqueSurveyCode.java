package com.psychotests.psycho_tests_server.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "unique_survey_code")
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UniqueSurveyCode {

    @Id
    @Column(name = "id_unique_survey_code")
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private UUID idUniqueSurveyCode;

    @Column(name = "code", unique = true, nullable = false)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_research", nullable = false)
    private Research research;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_respondent")
    private Respondent respondent;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt = Instant.now();

    @Column(name = "used_at")
    private Instant usedAt;

    private UniqueSurveyCode(UUID idUniqueSurveyCode, String code, Research research, Respondent respondent){
        this.idUniqueSurveyCode = idUniqueSurveyCode;
        this.code = code;
        this.research = research;
        this.respondent = respondent;
    }

}
