package com.psychotests.psycho_tests_server.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import com.psychotests.psycho_tests_server.enums.SurveySessionStatus;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//Класс прохождения опроса респондентами
@Entity
@Table(name = "survey_session")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SurveySession {
    
    @Id
    @Column(name = "id_survey_session")
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private UUID IDSurveySession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_survey", nullable = false)
    private Survey survey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_respondent", unique = true, nullable = false)
    private Respondent respondent;

    @OneToMany(
            mappedBy = "surveySession",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            orphanRemoval = false,
            fetch = FetchType.LAZY)
    private List<AnswersCharacteristics> answersCharacteristics = new ArrayList<>();

    @OneToMany(
            mappedBy = "surveySession",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            orphanRemoval = false,
            fetch = FetchType.LAZY
    )
    private List<AnswersItems> answersItems = new ArrayList<>();

    @OneToMany(
            mappedBy = "surveySession",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            orphanRemoval = false,
            fetch = FetchType.LAZY
    )
    private List<AnswersItemsSub> answersItemsSubs = new ArrayList<>();

    @OneToMany(
            mappedBy = "surveySession",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            orphanRemoval = false,
            fetch = FetchType.LAZY
    )
    private List<AnswersAssociations> answersAssociations = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status_survey_session", length = 10)
    private SurveySessionStatus statusSurveySession;

    @Column(name = "start_time_session", updatable = false, nullable = false)
    @CreationTimestamp
    private LocalDateTime startTimeSession;

    @Column(name = "finish_time_session")
    private LocalDateTime finishTimeSession;

    public SurveySession(UUID IDSurveySession){
        this.IDSurveySession = IDSurveySession;
        this.statusSurveySession = SurveySessionStatus.NOT_STARTED;
    }

}
