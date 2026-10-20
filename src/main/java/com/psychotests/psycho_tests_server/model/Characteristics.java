package com.psychotests.psycho_tests_server.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private Survey survey;

    @OneToMany(
            mappedBy = "characteristic",
            cascade = {CascadeType.MERGE, CascadeType.PERSIST},
            orphanRemoval = false,
            fetch = FetchType.LAZY
    )
    private List<AnswersCharacteristics> characteristic = new ArrayList<>();

    @Column(name = "text_characteristic", length = 50)
    private String textCharacteristic;

    public Characteristics(UUID idCharacteristic, String textCharacteristic){
        this.idCharacteristic = idCharacteristic;
        this.textCharacteristic = textCharacteristic;
    }

    public Characteristics(UUID idCharacteristic, Survey survey, String textCharacteristic) {
        this.idCharacteristic = idCharacteristic;
        this.survey = survey;
        this.textCharacteristic = textCharacteristic;
    }

}
