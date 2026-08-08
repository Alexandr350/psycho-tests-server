package com.psychotests.psycho_tests_server.model;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.CreationTimestamp;

import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import com.psychotests.psycho_tests_server.enums.TypeResearch;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//Класс исследования (личный кабинет)
@Entity
@Table(name = "research")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Research{

    @Id
    @Column(name = "id_research")                             
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private UUID IDResearch;

    @Column(name = "name_research", length = 100,  nullable = false)
    private String nameResearch;

    @Column(name = "description_research", length = 1000, nullable = false)
    private String descriptionResearch;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_research" , length = 10, nullable = false)
    private ResearchStatus statusResearch;

    @Column(name = "start_time_research", updatable = false, nullable = false)
    @CreationTimestamp
    private LocalDateTime startTimeResearch;

    @Column(name = "finish_time_research")
    private LocalDateTime finishTimeResearch;

    @OneToOne(mappedBy = "research", cascade = CascadeType.ALL, orphanRemoval = true)
    private Survey surveyResearch;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_research", length = 10, nullable = false)
    private TypeResearch typeResearch;

    @JsonIgnore
    @OneToMany(mappedBy = "research", fetch = FetchType.LAZY)
    private List<UniqueSurveyCode> uniqueSurveyCodes = new ArrayList<>();

    public Research(
        UUID IDResearch, 
        String nameResearch, 
        String descriptionResearch, 
        TypeResearch typeResearch){
            this.IDResearch = IDResearch;
            this.nameResearch = nameResearch;
            this.descriptionResearch = descriptionResearch;
            this.statusResearch = ResearchStatus.DRAFT;
            this.typeResearch = typeResearch;
    }

}