package com.psychotests.psycho_tests_server.model;

import java.util.List;
import java.util.ArrayList;
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

//Класс опроса (Личный кабинет/Создание исcледования)
@Entity
@Table(name = "survey")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Survey {
    
    @Id
    @Column(name = "id_survey")
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private UUID IDSurvey;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "research_id_survey", unique = true, nullable = false)
    private Research research;

    @OneToMany(
        mappedBy = "survey",
        cascade = {CascadeType.PERSIST, CascadeType.MERGE}, 
        orphanRemoval = false,
        fetch = FetchType.LAZY)
    private List<SurveySession> sessionsSurvey = new ArrayList<>();

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

    public Survey(UUID IDSurvey, String titleSurvey, String descriptionSurvey){
        this.titleSurvey = titleSurvey;
        this.descriptionSurvey = descriptionSurvey;
        this.IDSurvey = IDSurvey;
    }

}

