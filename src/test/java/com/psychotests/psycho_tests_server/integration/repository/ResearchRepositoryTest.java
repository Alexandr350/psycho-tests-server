package com.psychotests.psycho_tests_server.integration.repository;

import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import com.psychotests.psycho_tests_server.model.Research;
import com.psychotests.psycho_tests_server.repository.ResearchRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Слойный тест ResearchRepository: два ручных JPQL-запроса с фильтром
 * по владельцу. Методы реализуют авторизацию по принадлежности
 * (исследование видно только его психологу) — изоляция чужих данных
 * здесь не пожелание, а требование безопасности, поэтому тесты на
 * «чужое не возвращается» обязательны для обоих методов.
 */
class ResearchRepositoryTest extends SurveyFixtureTest {

    @Autowired
    private ResearchRepository repository;

    // ---------- findByIdAndPsychologistAccountIdPsychologistAccount ----------

    @Test
    void findByIdReturnsResearch_whenOwnedByPsychologist() {
        PsychologistAccount owner = newPsychologistAccount();
        Research research = newResearch(owner);

        var found = repository.findByIdAndPsychologistAccountIdPsychologistAccount(
                research.getIDResearch(), owner.getIdPsychologistAccount());

        assertThat(found).isPresent();
        assertThat(found.get().getIDResearch()).isEqualTo(research.getIDResearch());
    }

    /** Ключевой тест на авторизацию: чужое исследование не найдено. */
    @Test
    void findByIdReturnsEmpty_whenResearchBelongsToAnotherPsychologist() {
        Research foreignResearch = newResearch(newPsychologistAccount());
        PsychologistAccount someoneElse = newPsychologistAccount();

        var found = repository.findByIdAndPsychologistAccountIdPsychologistAccount(
                foreignResearch.getIDResearch(), someoneElse.getIdPsychologistAccount());

        assertThat(found).isEmpty();
    }

    @Test
    void findByIdReturnsEmpty_whenResearchDoesNotExist() {
        PsychologistAccount owner = newPsychologistAccount();

        assertThat(repository.findByIdAndPsychologistAccountIdPsychologistAccount(
                UUID.randomUUID(), owner.getIdPsychologistAccount())).isEmpty();
    }

    // ---------- findAllByPsychologistAccountIdPsychologistAccount ----------

    @Test
    void findAllReturnsOnlyOwnResearches() {
        PsychologistAccount owner = newPsychologistAccount();
        Research ownFirst = newResearch(owner);
        Research ownSecond = newResearch(owner);
        Research foreign = newResearch(newPsychologistAccount());

        List<Research> result =
                repository.findAllByPsychologistAccountIdPsychologistAccount(
                        owner.getIdPsychologistAccount());

        assertThat(result)
                .extracting(Research::getIDResearch)
                .containsExactlyInAnyOrder(
                        ownFirst.getIDResearch(), ownSecond.getIDResearch())
                .doesNotContain(foreign.getIDResearch());
    }

    @Test
    void findAllReturnsEmptyList_whenPsychologistHasNoResearches() {
        PsychologistAccount owner = newPsychologistAccount();

        assertThat(repository.findAllByPsychologistAccountIdPsychologistAccount(
                owner.getIdPsychologistAccount())).isEmpty();
    }

    // ---------- findByPublicToken ----------

    @Test
    void findByPublicToken_returnsResearch_whenTokenExists() {
        PsychologistAccount owner = newPsychologistAccount();
        Research research = newResearch(owner);
        research.setPublicToken("token-abc");
        repository.saveAndFlush(research);

        var found = repository.findByPublicToken("token-abc");

        assertThat(found).isPresent();
        assertThat(found.get().getIDResearch()).isEqualTo(research.getIDResearch());
    }

    @Test
    void findByPublicToken_returnsEmpty_whenTokenUnknown() {
        assertThat(repository.findByPublicToken("no-such-token")).isEmpty();
    }

    /**
     * Unique-индекс не запрещает несколько NULL (поведение и MySQL, и H2):
     * у нас токен null для всех незапущенных исследований — если бы NULL
     * конфликтовали, сохранение второго DRAFT падало бы.
     */
    @Test
    void multipleNullTokens_areAllowed() {
        PsychologistAccount owner = newPsychologistAccount();
        Research first = newResearch(owner);
        Research second = newResearch(owner);

        repository.saveAndFlush(first);
        repository.saveAndFlush(second); // не бросает constraint violation

        assertThat(repository.findByPublicToken(null)).isEmpty(); // и по null не ищет — контракт
    }

}
