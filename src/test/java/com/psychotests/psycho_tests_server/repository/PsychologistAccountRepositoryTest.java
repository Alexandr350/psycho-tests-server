package com.psychotests.psycho_tests_server.repository;


import com.psychotests.psycho_tests_server.model.Research;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;


public class PsychologistAccountRepositoryTest extends BaseRepositoryTest{

    @Test
    public void checkAddAndSearchByID(){
        var psychologist = createPsychologist( "Алексей", "Иванов");

        testEntityManager.persist(psychologist);
        testEntityManager.flush();
        testEntityManager.clear();

        var found = psychologistAccountRepository.findById(psychologist.getIdPsychologistAccount()).orElse(null);

        assertThat(found)
                .as("Психолог должен быть найден по ID")
                .isNotNull();

        assertThat(found.getIdPsychologistAccount())
                .as("ID найденного психолога должен совпадать с исходным")
                .isEqualTo(psychologist.getIdPsychologistAccount());

        assertThat(found.getNamePsychologist())
                .as("Имя психолога не совпадает")
                .isEqualTo("Алексей");

        assertThat(found.getLastnamePsychologist())
                .as("Фамилия психолога не совпадает")
                .isEqualTo("Иванов");
    }

    @Test
    public void checkAddResearchAndGetListResearch(){
        var psychologist = createPsychologist("Иван" , "Николаев");

        var research_1 = createResearch("Исследование 1", "Описание 1");
        research_1.setPsychologistAccount(psychologist);
        var research_2 = createResearch("Исследование 2", "Описание 2");
        research_2.setPsychologistAccount(psychologist);
        var research_3 = createResearch("Исследование 3", "Описание 3");
        research_3.setPsychologistAccount(psychologist);

        linkResearchToPsychologist(psychologist, research_1, research_2, research_3);

        testEntityManager.persist(psychologist);
        testEntityManager.flush();
        testEntityManager.clear();

        var result = psychologistAccountRepository
                .findById(psychologist.getIdPsychologistAccount())
                .orElseThrow(() -> new AssertionError("Психолог не найден"));


        assertThat(result.getResearchList())
                .as("У психолога должны быть привязаны исследования")
                .isNotNull();

        assertThat(result.getResearchList().size())
                .as("Количество исследований не совпадает")
                .isEqualTo(3);

        var titles = result.getResearchList()
                .stream()
                .map(Research::getNameResearch)
                .toList();

        assertThat(titles)
                .as("Список названий не совпадает")
                .containsExactlyInAnyOrder("Исследование 1", "Исследование 2", "Исследование 3");

        result.getResearchList().forEach(r ->
                assertThat(r.getPsychologistAccount().getIdPsychologistAccount())
                        .isEqualTo(psychologist.getIdPsychologistAccount()));

    }

    @Test
    public void checkRemoveCascadeLink(){
        var psychologist = createPsychologist("Сергей", "Кузнецов");

        var research_1 = createResearch("Исследование 1", "Описание 1");
        var research_2 = createResearch("Исследование 2", "Описание 2");
        var research_3 = createResearch("Исследование 3", "Описание 3");

        testEntityManager.persist(psychologist);
        linkResearchToPsychologist(psychologist, research_1, research_2, research_3);



        var loaderPsychologist = psychologistAccountRepository
                .findById(psychologist.getIdPsychologistAccount())
                .orElseThrow(() -> new AssertionError("Психолог с ID " + psychologist.getIdPsychologistAccount() + " не найден в БД"));

        assertThat(loaderPsychologist.getResearchList()).hasSize(3);

        psychologistAccountRepository.delete(loaderPsychologist);
        testEntityManager.flush();
        testEntityManager.clear();

        assertThat(psychologistAccountRepository.findById(psychologist.getIdPsychologistAccount())).isEmpty();
        assertThat(researchRepository.findAll()).isEmpty();
    }

}
