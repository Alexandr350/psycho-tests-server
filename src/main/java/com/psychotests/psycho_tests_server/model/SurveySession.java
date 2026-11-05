package com.psychotests.psycho_tests_server.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.psychotests.psycho_tests_server.enums.SurveySessionStatus;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//Класс прохождения опроса респондентами
@Entity
@Table(name = "survey_session", uniqueConstraints = @UniqueConstraint(
        name = "uk_session_respondent_run",
        columnNames = {"id_respondent", "id_run"}
))
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SurveySession {

    @Id
    @Column(name = "id_survey_session", updatable = false)
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private UUID idSurveySession;

    /** Запуск, в рамках которого отвечает респондент:
     * изолирует волны данных (запуск 1 / запуск 2). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_run", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Run run;

    /** Денормализация (survey = run.research.surveyResearch) — нужна
     * существующим запросам по idSurvey; каскад в БД сносит сессию
     * вместе с опросом. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_survey", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Survey survey;

    /** Респондент — НЕ каскадируется: сущность независимая, человек
     * может иметь сессии в других исследованиях. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_respondent", nullable = false)
    private Respondent respondent;

    @OneToMany(
            mappedBy = "surveySession",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY)
    private List<AnswersCharacteristics> answersCharacteristics = new ArrayList<>();

    @OneToMany(
            mappedBy = "surveySession",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY
    )
    private List<AnswersItems> answersItems = new ArrayList<>();

    @OneToMany(
            mappedBy = "surveySession",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY
    )
    private List<AnswersItemsSubs> answersItemsSubs = new ArrayList<>();

    @OneToMany(
            mappedBy = "surveySession",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY
    )
    private List<AnswersAssociations> answersAssociations = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status_survey_session", length = 15)
    private SurveySessionStatus statusSurveySession;

    @Column(name = "start_time_session", updatable = false, nullable = false)
    @CreationTimestamp
    @Setter(AccessLevel.NONE)
    private LocalDateTime startTimeSession;

    @Column(name = "finish_time_session")
    private LocalDateTime finishTimeSession;

    public SurveySession(UUID idSurveySession){
        this.idSurveySession = Objects.requireNonNull(idSurveySession, "idSurveySession must be set explicitly");
        this.statusSurveySession = SurveySessionStatus.NOT_STARTED;
    }
}
