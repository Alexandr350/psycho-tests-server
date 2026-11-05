package com.psychotests.psycho_tests_server.integration.repository;

import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import com.psychotests.psycho_tests_server.repository.PsychologistAccountRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Слойный тест PsychologistAccountRepository: производные query methods
 * existsByEmailPsychologist и findByEmailPsychologist плюс уникальное
 * ограничение uk_psychologist_email из схемы. Аккаунт — корень цепочки
 * сущностей, внешних зависимостей у фикстуры нет.
 */
@DataJpaTest
class PsychologistAccountRepositoryTest {

    @Autowired
    private PsychologistAccountRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    /** Все пять полей обязательны; email передаётся снаружи для проверок. */
    private PsychologistAccount newAccount(String email) {
        return entityManager.persistAndFlush(new PsychologistAccount(
                UUID.randomUUID(),
                "Анна",
                "Тестовая",
                "$2a$10$" + "x".repeat(53),
                email));
    }

    // ---------- existsByEmailPsychologist ----------

    @Test
    void existsReturnsTrue_whenEmailSaved() {
        String email = "psychologist-" + UUID.randomUUID() + "@test.local";
        newAccount(email);

        assertThat(repository.existsByEmailPsychologist(email)).isTrue();
    }

    @Test
    void existsReturnsFalse_whenEmailUnknown() {
        assertThat(repository.existsByEmailPsychologist("nobody-" + UUID.randomUUID() + "@test.local"))
                .isFalse();
    }

    // ---------- findByEmailPsychologist ----------

    @Test
    void findByEmailReturnsSavedAccount() {
        PsychologistAccount saved = newAccount("found-" + UUID.randomUUID() + "@test.local");

        var found = repository.findByEmailPsychologist(saved.getEmailPsychologist());

        assertThat(found).isPresent();
        assertThat(found.get().getIdPsychologistAccount())
                .isEqualTo(saved.getIdPsychologistAccount());
    }

    @Test
    void findByEmailReturnsEmpty_whenEmailUnknown() {
        assertThat(repository.findByEmailPsychologist("nobody-" + UUID.randomUUID() + "@test.local"))
                .isEmpty();
    }

    // ---------- уникальность email (схема: uk_psychologist_email) ----------

    /**
     * Второй аккаунт с тем же email отклоняется БД на flush. Это проверка
     * ограничения схемы, а не логики репозитория, но именно её опирается
     * auth-флоу: регистрация не создаст дубликат.
     */
    @Test
    void duplicateEmailRejectedByUniqueConstraint() {
        String email = "dup-" + UUID.randomUUID() + "@test.local";
        newAccount(email);

        var duplicate = new PsychologistAccount(
                UUID.randomUUID(), "Пётр", "Дубль", "$2a$10$" + "y".repeat(53), email);

        // Сохраняем через репозиторий, как это будет делать сервис:
        // переводчик исключений Spring превращает Hibernate-ошибку
        // в DataIntegrityViolationException
        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

}
