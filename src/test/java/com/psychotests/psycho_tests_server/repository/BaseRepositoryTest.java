package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.enums.TypeResearch;
import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import com.psychotests.psycho_tests_server.model.Research;
import com.psychotests.psycho_tests_server.model.Survey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.AutoConfigureTestEntityManager;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import java.util.UUID;

@DataJpaTest
@AutoConfigureTestEntityManager
public abstract class BaseRepositoryTest {
    @Autowired
    protected PsychologistAccountRepository psychologistAccountRepository;

    @Autowired
    protected ResearchRepository researchRepository;

    @Autowired
    protected SurveyRepository surveyRepository;

    @Autowired
    protected TestEntityManager testEntityManager;

    protected PsychologistAccount createPsychologist(String name, String lastname){
        var id = UUID.randomUUID();
        return new PsychologistAccount(id, name, lastname);
    }

    protected Research createResearch(String title, String description){
        var id = UUID.randomUUID();
        return new Research(id, title, description, TypeResearch.CPSK);
    }

    protected Survey createSurvey(String title, String description){
        var id = UUID.randomUUID();
        return new Survey(id, title, description);
    }

    protected void linkResearchToPsychologist(PsychologistAccount psychologistAccount, Research... researches){
        for(var r: researches){
            r.setPsychologistAccount(psychologistAccount);
            psychologistAccount.getResearchList().add(r);
        }
    }

    protected void linkSurveyToResearch(Research research, Survey survey){
        research.setSurveyResearch(survey);
        survey.setResearch(research);
    }
}
