package com.psychotests.psycho_tests_server.repository;

import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import com.psychotests.psycho_tests_server.model.Research;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;



public class ResearchRepositoryTest extends BaseRepositoryTest{

    private Research dataTest(){

        var psychologist = createPsychologist("Анна", "Петрова", "f34rdsf#", "");
        testEntityManager.persist(psychologist);

        var research = createResearch("Исследование", "Описание", "");
        linkResearchToPsychologist(psychologist, research);
        testEntityManager.persist(research);

        var survey = createSurvey("Опрос", "Описание", "");
        linkSurveyToResearch(research, survey);
        testEntityManager.persist(survey);
        testEntityManager.flush();

        return research;

    }

    @Test
    public void checkCreateResearch(){
        var psychologist = createPsychologist("Егор", "Иванов", "wer2r3r2#", "sdfsdf@sdf.com");
        psychologist = psychologistAccountRepository.save(psychologist);

        var research = createResearch("Исследование", "Описание исследования", "erwe");
        research.setPsychologistAccount(psychologist);

        var saved = researchRepository.save(research);

        testEntityManager.flush();
        testEntityManager.clear();

       var loaded = researchRepository.findById(saved.getIDResearch())
               .orElseThrow(() -> new AssertionError("Исследование не найдено"));

       assertThat(loaded.getStatusResearch()).isEqualTo(ResearchStatus.DRAFT);
       assertThat(loaded.getNameResearch()).isEqualTo("Исследование");
       assertThat(loaded.getPsychologistAccount().getIdPsychologistAccount())
               .isEqualTo(psychologist.getIdPsychologistAccount());

    }

    @Test
    public void checkAddSurvey(){

        var savedResearch = dataTest();
        testEntityManager.clear();

        var loaderResearch = researchRepository
                .findById(savedResearch.getIDResearch())
                .orElseThrow(() -> new AssertionError("Исследование не найдено"));

        assertThat(loaderResearch.getNameResearch()).isEqualTo("Исследование");
        assertThat(loaderResearch.getSurveyResearch()).isNotNull();

        assertThat(loaderResearch.getSurveyResearch().getTitleSurvey())
                .isEqualTo("Опрос");

        var loadedSurvey = surveyRepository
                .findById(loaderResearch.getSurveyResearch().getIDSurvey())
                .orElseThrow(() -> new AssertionError("Опрос не найден"));

        assertThat(loadedSurvey.getResearch().getIDResearch())
                .isEqualTo(loaderResearch.getIDResearch());
    }

    @Test
    public void checkCascadeRemove(){

        var savedResearch = dataTest();
        testEntityManager.clear();

        var surveyId = savedResearch.getSurveyResearch().getIDSurvey();

        var loaderResearch = researchRepository
                .findById(savedResearch.getIDResearch())
                .orElseThrow(() -> new AssertionError("Исследование не найдено"));

        var loaderSurvey = surveyRepository
                .findById(loaderResearch.getSurveyResearch().getIDSurvey())
                .orElseThrow(() -> new AssertionError("Опрос не найден"));


        assertThat(loaderResearch).isNotNull();
        assertThat(loaderSurvey).isNotNull();

        researchRepository.delete(loaderResearch);
        testEntityManager.flush();
        testEntityManager.clear();

        assertThat(researchRepository.findById(savedResearch.getIDResearch())).isEmpty();
        assertThat(surveyRepository.findById(surveyId)).isEmpty();

    }


}
