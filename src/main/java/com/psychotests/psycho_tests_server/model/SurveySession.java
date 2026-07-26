package com.psychotests.psycho_tests_server.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.psychotests.psycho_tests_server.enums.SurveySessionStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//Класс прохождения опроса респондентами
@Entity
@Table(name = "survey_session")
@Getter
@Setter()
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SurveySession {
    
    @Id
    @Column(name = "id_survey_session")
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private UUID IDSurveySession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_survey", nullable = false)
    private Survey IDSurvey;

    @OneToOne(
        mappedBy = "surveySession",
        cascade = {CascadeType.MERGE, CascadeType.PERSIST},
        orphanRemoval = false,
        fetch = FetchType.LAZY)
    private Respondent respondentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_survey_session", length = 10)
    private SurveySessionStatus statusSurveySession;

    @Column(name = "start_time_session", updatable = false, nullable = false)
    @CreationTimestamp
    private LocalDateTime startTimeSession;

    @Column(name = "finish_time_session")
    private LocalDateTime finishTimeSession;

    public SurveySession(UUID IDSurveySessin){
        this.IDSurveySession = IDSurveySessin;
        this.statusSurveySession = SurveySessionStatus.NOT_STARTED;
    }

}
