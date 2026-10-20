package com.psychotests.psycho_tests_server.model;
import java.time.LocalDateTime;

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

    @Column(name = "description_research", length = 1000)
    private String descriptionResearch;

    @Column(name = "customer_research", length = 100)
    private String customerResearch;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_research" , length = 10, nullable = false)
    private ResearchStatus statusResearch;

    @Column(name = "created_at_research", updatable = false, nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAtResearch;

    @Column(name = "start_time_research")
    private LocalDateTime startTimeResearch;

    @Column(name = "finish_time_research")
    private LocalDateTime finishTimeResearch;

    @OneToOne(mappedBy = "research", cascade = CascadeType.ALL, orphanRemoval = true)
    private Survey surveyResearch;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_research", length = 10, nullable = false)
    private TypeResearch typeResearch;

    @OneToMany(mappedBy = "research", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UniqueSurveyCode> uniqueSurveyCodes = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "psychologist_account", nullable = false)
    private PsychologistAccount psychologistAccount;

    public Research(
        UUID IDResearch, 
        String nameResearch, 
        String descriptionResearch, 
        String customerResearch,
        TypeResearch typeResearch){
            this.IDResearch = IDResearch;
            this.nameResearch = nameResearch;
            this.descriptionResearch = descriptionResearch;
            this.customerResearch = customerResearch;
            this.typeResearch = typeResearch;
            this.statusResearch = ResearchStatus.DRAFT;

    }

    public void changeStatus(ResearchStatus newStatus) {
        if (!statusResearch.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Недопустимый переход статуса: " + statusResearch + " → " + newStatus);
        }
        this.statusResearch = newStatus;

        if (newStatus == ResearchStatus.RUNNING && startTimeResearch == null) {
            this.startTimeResearch = LocalDateTime.now();
        }

        if(newStatus == ResearchStatus.STOPPED){
            this.finishTimeResearch = LocalDateTime.now();
        }
    }


}