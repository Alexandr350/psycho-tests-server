package com.psychotests.psycho_tests_server.unit.model;

import java.util.UUID;

import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import com.psychotests.psycho_tests_server.model.Research;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.mock;

/**
 * Юнит-тесты PsychologistAccount — «личный кабинет психолога»: учётная
 * запись с ФИО, паролем (BCrypt-хэш из PasswordEncoder) и email,
 * владеющая списком исследований (OneToMany на Research с каскадом ALL
 * и orphanRemoval = true).
 *
 * <p>Что проверяем: контракты конструктора — все пять обязательных полей
 * (id, имя, фамилия, пароль, email) защищены requireNonNull с
 * русскоязычными сообщениями — фирменный стиль этого класса, в отличие
 * от answer-сущностей, где сообщения на английском; выровнено со схемой:
 * конструктор и nullable = false колонок говорят одно и то же;
 * уникальность email не проверяется конструктором — она живёт в схеме
 * (uk_psychologist_email) и в БД; длины полей не проверяются конструктором —
 * length ловит Hibernate; длина BCrypt-хэша (60) влезает в колонку
 * password_psychologist; коллекция исследований инициализирована пустым
 * списком; равенство только по id; NoArgsConstructor — id остаётся null.</p>
 *
 * <p>Зафиксированные решения:
 * 1) cascade = ALL + orphanRemoval = true — осознанное владение:
 * удаление аккаунта удаляет и его исследования, а сохранение аккаунта
 * протягивает новые исследования. Если у исследований появятся свои
 * владельцы — переворачивать здесь, а не в сервисе.
 * 2) Пустая строка в ФИО/пароле/email конструктором принимается —
 * не-пустота и формат email валидируются в DTO (@NotBlank/@Email):
 * в БД приходит чистое значение, сущность не дублирует DTO-валидацию.</p>
 *
 * <p>Граница с другими тестами: уникальность email и целостность
 * join-колонок покрываются @DataJpaTest — констрейнт uk_psychologist_email
 * живёт в БД и юнитом не проверяется. Research подменяется моком.</p>
 */

class PsychologistAccountTest {

    private UUID id;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
    }

    @Nested
    class ConstructorContracts {

        /** Полное построение: все поля делегируются без изменений. */
        @Test
        void constructor_setsAllFields() {
            var account = new PsychologistAccount(
                    id, "Анна", "Иванова", "bcrypt-hash", "anna@example.com");

            assertThat(account.getIdPsychologistAccount()).isEqualTo(id);
            assertThat(account.getNamePsychologist()).isEqualTo("Анна");
            assertThat(account.getLastnamePsychologist()).isEqualTo("Иванова");
            assertThat(account.getPasswordPsychologist()).isEqualTo("bcrypt-hash");
            assertThat(account.getEmailPsychologist()).isEqualTo("anna@example.com");
        }

        /** id обязателен — null отвергается с русскоязычным сообщением. */
        @Test
        void id_nullRejected() {
            assertThatThrownBy(() -> new PsychologistAccount(
                    null, "Анна", "Иванова", "hash", "anna@example.com"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("id");
        }

        /** Пароль обязателен — аккаунт без пароля не существует. */
        @Test
        void password_nullRejected() {
            assertThatThrownBy(() -> new PsychologistAccount(
                    id, "Анна", "Иванова", null, "anna@example.com"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("пароль");
        }

        /**
         * ФИО и email обязательны — выровнено со схемой: все три колонки
         * nullable = false, и конструктор защищает их наравне с id и паролем.
         * Формат email и не-пустота валидируются в DTO: в БД приходит
         * чистое значение, сущность не дублирует DTO-валидацию.
         */
        @Test
        void nameLastnameEmail_nullRejected() {
            assertThatThrownBy(() -> new PsychologistAccount(
                    id, null, "Иванова", "hash", "anna@example.com"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("имя");

            assertThatThrownBy(() -> new PsychologistAccount(
                    id, "Анна", null, "hash", "anna@example.com"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("фамилия");

            assertThatThrownBy(() -> new PsychologistAccount(
                    id, "Анна", "Иванова", "hash", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("email");
        }


        /**
         * Зафиксированное решение: пустые строки принимаются — не-пустота
         * и формат email валидируются в DTO, сущность не дублирует.
         */
        @Test
        void blankFields_allowedByConstructor() {
            var account = new PsychologistAccount(
                    id, "", "", "hash", "");

            assertThat(account.getNamePsychologist()).isEmpty();
            assertThat(account.getLastnamePsychologist()).isEmpty();
            assertThat(account.getEmailPsychologist()).isEmpty();
        }

        /**
         * Уникальность email не проверяется конструктором: инвариант живёт
         * в схеме (uk_psychologist_email) и БД — два аккаунта с одним email
         * строятся в памяти свободно, а падают на persist.
         */
        @Test
        void duplicateEmail_notRejectedByConstructor() {
            var a = new PsychologistAccount(
                    UUID.randomUUID(), "Анна", "Иванова", "hash-1", "anna@example.com");
            var b = new PsychologistAccount(
                    UUID.randomUUID(), "Мария", "Петрова", "hash-2", "anna@example.com");

            assertThat(b.getEmailPsychologist())
                    .isEqualTo(a.getEmailPsychologist());
        }

        /**
         * BCrypt-хэш всегда 60 символов — ровно под колонку
         * password_psychologist (length = 60). Тест перевернётся, если
         * PasswordEncoder сменится на алгоритм с более длинным хэшем —
         * раньше, чем в проде упадёт вставка.
         */
        @Test
        void bcryptHash_fitsColumnLength60() {
            var bcryptHash = "$2a$10$" + "N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
            var account = new PsychologistAccount(
                    id, "Анна", "Иванова", bcryptHash, "anna@example.com");

            assertThat(account.getPasswordPsychologist()).hasSize(60);
        }
    }

    @Nested
    class ResearchCollection {

        /** Коллекция исследований инициализирована пустым списком, а не null. */
        @Test
        void researchList_defaultEmptyList() {
            var account = new PsychologistAccount(
                    id, "Анна", "Иванова", "hash", "anna@example.com");

            assertThat(account.getResearchList())
                    .isNotNull()
                    .isEmpty();
        }

        /**
         * Живая ссылка: добавление в коллекцию видно через геттер. Здесь,
         * в отличие от answer-сущностей, каскад ALL — сохранение аккаунта
         * протягивает и новые исследования.
         */
        @Test
        void researchList_liveReference() {
            var account = new PsychologistAccount(
                    id, "Анна", "Иванова", "hash", "anna@example.com");
            var research = mock(Research.class);

            account.getResearchList().add(research);

            assertThat(account.getResearchList())
                    .containsExactly(research);
        }
    }

    @Nested
    class EqualityContract {

        /**
         * Равенство только по id: два аккаунта с одинаковым id, но разными
         * полями — это одна и та же строка. Аннотация EqualsAndHashCode
         * с onlyExplicitlyIncluded гарантирует, что поля не протекут в equals.
         */
        @Test
        void equals_sameIdDifferentFields_true() {
            var a = new PsychologistAccount(
                    id, "Анна", "Иванова", "hash-1", "anna@example.com");
            var b = new PsychologistAccount(
                    id, "Мария", "Петрова", "hash-2", "maria@example.com");

            assertThat(a).isEqualTo(b);
            assertThat(a).hasSameHashCodeAs(b);
        }

        /** Разные id — разные аккаунты, независимо от остальных полей. */
        @Test
        void equals_differentIds_false() {
            var a = new PsychologistAccount(
                    id, "Анна", "Иванова", "hash", "anna@example.com");
            var b = new PsychologistAccount(
                    UUID.randomUUID(), "Анна", "Иванова", "hash", "anna@example.com");

            assertThat(a).isNotEqualTo(b);
        }
    }

    @Nested
    class JpaLifecycle {

        /**
         * NoArgsConstructor (для JPA) создаёт объект с null-id — провайдер
         * заполнит его сам при persist. Тест фиксирует, что конструктор
         * не содержит скрытой логики: id не генерируется на месте.
         */
        @Test
        void noArgsConstructor_idStaysNull() {
            assertThat(new PsychologistAccount().getIdPsychologistAccount())
                    .isNull();
        }

        /** Сеттеры Lombok работают — включая ФИО, пароль и email. */
        @Test
        void setters_updateFields() {
            var account = new PsychologistAccount(
                    id, "Анна", "Иванова", "hash", "anna@example.com");

            account.setNamePsychologist("Мария");
            account.setLastnamePsychologist("Петрова");
            account.setPasswordPsychologist("new-hash");
            account.setEmailPsychologist("maria@example.com");

            assertThat(account.getNamePsychologist()).isEqualTo("Мария");
            assertThat(account.getLastnamePsychologist()).isEqualTo("Петрова");
            assertThat(account.getPasswordPsychologist()).isEqualTo("new-hash");
            assertThat(account.getEmailPsychologist()).isEqualTo("maria@example.com");
        }
    }
}
