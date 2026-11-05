package com.psychotests.psycho_tests_server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

//Класс опроса (Личный кабинет/Создание исследования)
@Entity
@Table(name = "survey")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Survey {

    @Id
    @Column(name = "id_survey", updatable = false)
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private UUID idSurvey;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "research_id_survey", unique = true, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Research research;

    @OneToMany(
            mappedBy = "survey",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY)
    private List<SurveySession> surveySessions = new ArrayList<>();

    @OneToMany(
            mappedBy = "survey",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Characteristics> characteristicsSurvey = new ArrayList<>();

    @OneToMany(
            mappedBy = "survey",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Items> itemsSurvey = new ArrayList<>();

    @OneToMany(
            mappedBy = "survey",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ItemsSub> itemsSubsSurvey = new ArrayList<>();

    @Column(name = "title_survey", length = 150, nullable = false)
    private String titleSurvey;

    @Column(name = "description_survey", length = 1000)
    private String descriptionSurvey;

    @Column(name = "rules_survey", length = 2000)
    private String rulesSurvey;

    public Survey(UUID idSurvey, String titleSurvey, String descriptionSurvey, String rulesSurvey){
        this.idSurvey = Objects.requireNonNull(idSurvey, "idSurvey must be set explicitly");
        this.titleSurvey = Objects.requireNonNull(titleSurvey, "titleSurvey must be set explicitly");
        this.descriptionSurvey = descriptionSurvey;
        this.rulesSurvey = rulesSurvey;
    }

}
