package com.psychotests.psycho_tests_server.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

//Класс уникальных кодов
@Entity
@Table(name = "unique_survey_code")
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UniqueSurveyCode {

    @Id
    @Column(name = "id_unique_survey_code", updatable = false)
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private UUID idUniqueSurveyCode;

    /** Код уникален глобально — так исключаются коллизии между запусками
     * даже при повторной генерации на второй волне. */
    @Column(name = "code", unique = true, nullable = false)
    private String code;

    /** Запуск, в рамках которого код выдан: каждый запуск получает свой
     * набор кодов (если исследование использует коды). Каскад в БД —
     * коды волны умирают вместе с запуском. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_run")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Run run;

    /** Денормализация (research = run.research) — оставлена для существующих
     * запросов и коллекции на Research. Каскад в БД — коды умирают вместе
     * с исследованием. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_research")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Research research;

    /** Респондент — НЕ каскадируется: человек независим от исследования. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_respondent")
    private Respondent respondent;

    @Column(name = "issued_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant issuedAt = Instant.now();

    @Column(name = "used_at")
    private Instant usedAt;

    public UniqueSurveyCode(UUID idUniqueSurveyCode, String code, Research research, Respondent respondent){
        this.idUniqueSurveyCode = Objects.requireNonNull(idUniqueSurveyCode, "idUniqueSurveyCode must be set explicitly");
        this.code = Objects.requireNonNull(code, "code must be set explicitly");
        this.research = Objects.requireNonNull(research, "research must be set explicitly");
        this.respondent = respondent;
    }
}
