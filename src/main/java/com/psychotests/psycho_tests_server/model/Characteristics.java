package com.psychotests.psycho_tests_server.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

//Класс характеристик объектов
@Entity
@Table(name = "characteristics")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Characteristics {

    @Id
    @Column(name = "id_characteristic")
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private UUID idCharacteristic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_survey_characteristics", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Survey survey;

    @OneToMany(
            mappedBy = "characteristic",
            cascade = {CascadeType.MERGE, CascadeType.PERSIST},
            fetch = FetchType.LAZY
    )
    private List<AnswersCharacteristics> answersCharacteristics = new ArrayList<>();

    @Column(name = "text_characteristic", length = 50)
    private String textCharacteristic;

    public Characteristics(UUID idCharacteristic, String textCharacteristic){
        this.idCharacteristic = Objects.requireNonNull(idCharacteristic, "idCharacteristic must be set explicitly");
        this.textCharacteristic = textCharacteristic;
    }

    public Characteristics(UUID idCharacteristic, Survey survey, String textCharacteristic) {
        this.idCharacteristic = Objects.requireNonNull(idCharacteristic, "idCharacteristic must be set explicitly");
        this.survey = Objects.requireNonNull(survey, "survey must be set explicitly");
        this.textCharacteristic = textCharacteristic;
    }
}
